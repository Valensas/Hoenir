package com.valensas.hoenir.virtualservice

import com.valensas.hoenir.api.VirtualServiceApi
import com.valensas.hoenir.crd.HttpElement
import com.valensas.hoenir.crd.VirtualService
import com.valensas.hoenir.createOrReplaceNamespacedIngress
import com.valensas.hoenir.fullname
import io.kubernetes.client.extended.controller.reconciler.Reconciler
import io.kubernetes.client.extended.controller.reconciler.Request
import io.kubernetes.client.extended.controller.reconciler.Result
import io.kubernetes.client.informer.SharedIndexInformer
import io.kubernetes.client.informer.cache.Lister
import io.kubernetes.client.openapi.ApiException
import io.kubernetes.client.openapi.apis.NetworkingV1Api
import io.kubernetes.client.openapi.models.V1IngressBuilder
import io.kubernetes.client.openapi.models.V1IngressRule
import io.kubernetes.client.openapi.models.V1IngressRuleBuilder
import io.kubernetes.client.openapi.models.V1IngressRuleFluent
import org.slf4j.LoggerFactory

data class IstioConfg(
    val namespace: String,
    val ingressGatewayService: String,
    val ingressGatewayPort: Int
)

class VirtualServiceReconciler(
    virtualServiceInformer: SharedIndexInformer<VirtualService>,
    private val virtualServiceApi: VirtualServiceApi,
    private val networkingV1Api: NetworkingV1Api,
    private val istioConfig: IstioConfg
) : Reconciler {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val virtualServiceLister: Lister<VirtualService> = Lister(virtualServiceInformer.indexer)

    override fun reconcile(request: Request): Result {
        try {
            logger.info("Reconciling ${request.fullname()}")
            val virtualService = virtualServiceLister.get(request.fullname())
            val ingressName = "${virtualService.metadata.name}.${virtualService.metadata.namespace}"

            if (virtualService == null) {
                try {
                    networkingV1Api.deleteNamespacedIngress(
                        ingressName,
                        istioConfig.namespace,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                    )
                } catch (e: ApiException) {
                    if (e.code != 404) {
                        throw e
                    }
                }
            }

            if (virtualService.metadata.deletionTimestamp != null) {
                handleVirtualServiceDeletion(virtualService, ingressName, istioConfig.namespace)
            } else {
                createOrUpdateIngress(virtualService, ingressName, istioConfig.namespace)
            }
            logger.info("Successfully reconciled ${request.fullname()}")
            return Result(false)
        } catch (e: Throwable) {
            logger.error("Error reconciling VirtualService ${request.fullname()}", e)
            return Result(true)
        }
    }

    private fun handleVirtualServiceDeletion(
        virtualService: VirtualService,
        name: String,
        namespace: String,
    ) {
        val result = runCatching {
            networkingV1Api.deleteNamespacedIngress(
                name,
                namespace,
                null,
                null,
                null,
                null,
                null,
                null,
            )
        }

        val e = result.exceptionOrNull()
        if (result.isSuccess || (e is ApiException && e.code == 404)) {
            if (virtualService.metadata.finalizers?.remove("istio.valensas.com/ingress") == true) {
                virtualServiceApi.update(virtualService)
            }
            if (e is ApiException && e.code == 404) {
                logger.debug("Ingress for VirtualService ${virtualService.fullname()} not found")
            }
        } else {
            throw e!!
        }
    }

    private fun buildPath(
        type: String,
        uriValue: String,
        ruleBuilder: V1IngressRuleFluent.HttpNested<V1IngressRuleBuilder>
    ): V1IngressRuleFluent.HttpNested<V1IngressRuleBuilder> {
        return ruleBuilder
            .addNewPath()
            .withPath(uriValue)
            .withPathType(type)
            .withNewBackend()
            .withNewService()
            .withName(istioConfig.ingressGatewayService)
            .withNewPort()
            .withNumber(istioConfig.ingressGatewayPort)
            .endPort()
            .endService()
            .endBackend()
            .endPath()
    }

    private fun buildRule(
        https: List<HttpElement>,
        host: String?,
    ): V1IngressRule {
        val ruleBuilder =
            V1IngressRuleBuilder().let { if (host != null) it.withHost(host) else it }.withNewHttp().withPaths()
        return https.fold(ruleBuilder) { it, http ->
            val uri = http.match?.get(0)?.uri
            if (http.route != null || http.redirect != null || http.delegate != null) {
                if (uri?.exact != null) {
                    buildPath("Exact", uri.exact, it)
                } else if (uri?.prefix != null || http.match == null) {
                    buildPath("Prefix", uri?.prefix ?: "/", it)
                } else {
                    it
                }
            } else {
                it
            }
        }.endHttp().build()
    }

    private fun createOrUpdateIngress(
        virtualService: VirtualService,
        name: String,
        namespace: String,
    ) {
        val annotations = virtualService.metadata.annotations
        val labels = virtualService.metadata.labels
        val className = virtualService.metadata.annotations?.get("istio.valensas.com/ingress-class")
        val isIngressTLS = virtualService.metadata.annotations?.get("istio.valensas.com/ingress-tls") == "true"
        val tlsSecret = virtualService.metadata.annotations?.get("istio.valensas.com/tls-secret")
        val https = virtualService.spec.http
        val hosts = virtualService.spec.hosts
        val tlsSecretName = if (isIngressTLS) {
            tlsSecret ?: "$name.tls"
        } else {
            null
        }

        val rules = hosts?.map { buildRule(https, it) } ?: listOf(buildRule(https, null))

        val ingress = V1IngressBuilder()
            .withApiVersion("networking.k8s.io/v1")
            .withKind("Ingress")
            .withNewMetadata()
            .withAnnotations<String, String>(annotations)
            .withLabels<String, String>(labels)
            .withNamespace(namespace)
            .withName(name)
            .endMetadata()
            .withNewSpec()
            .let {
                if (!className.isNullOrBlank()) {
                    it.withIngressClassName(className)
                } else {
                    it
                }
            }
            .let {
                if (isIngressTLS) {
                    it
                        .addNewTl()
                        .withHosts()
                        .addAllToHosts(hosts)
                        .withSecretName(tlsSecretName)
                        .endTl()
                } else {
                    it
                }
            }
            .withRules(rules)
            .endSpec()
            .build()

        if (virtualService.metadata.finalizers?.contains("istio.valensas.com/ingress") != true) {
            virtualService.metadata.addFinalizersItem("istio.valensas.com/ingress")
            virtualServiceApi.update(virtualService)
        }

        try {
            networkingV1Api.createOrReplaceNamespacedIngress(ingress)
        } catch (e: Throwable) {
            logger.error("Failed to create Ingress ${ingress.fullname()} for VirtualService ${virtualService.fullname()}", e)
            throw e
        }
    }
}
