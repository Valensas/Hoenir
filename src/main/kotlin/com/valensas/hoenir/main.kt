package com.valensas.hoenir

import com.valensas.hoenir.api.VirtualServiceApi
import com.valensas.hoenir.grafana.grafanaController
import com.valensas.hoenir.virtualservice.IstioConfg
import com.valensas.hoenir.virtualservice.virtualServiceController
import io.kubernetes.client.extended.controller.builder.ControllerBuilder
import io.kubernetes.client.informer.SharedInformerFactory
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.apis.NetworkingV1Api
import io.kubernetes.client.util.ClientBuilder
import java.lang.Integer.max

fun istioConfig() = IstioConfg(
    namespace = System.getenv("ISTIO_NAMESPACE") ?: "istio-system",
    ingressGatewayService = System.getenv("ISTIO_INGRESSGATEWAY_SERVICE") ?: "istio-ingressgateway",
    ingressGatewayPort = System.getenv("ISTIO_INGRESSGATEWAY_PORT")?.toInt() ?: 8080
)

fun main() {
    val client = ClientBuilder.standard().build()
    val coreV1Api = CoreV1Api(client)
    val networkingV1Api = NetworkingV1Api(client)
    val virtualServiceApi = VirtualServiceApi(client)
    val informerFactory = SharedInformerFactory(client)

    val namespace = System.getenv("WATCH_NAMESPACE")
    val cpus = Runtime.getRuntime().availableProcessors()

    val controllerManagerBuilder = ControllerBuilder.controllerManagerBuilder(informerFactory)

    if (System.getenv("GRAFANA_DISABLED") == null) {
        val grafanaDefaultDatasourceName = System.getenv("GRAFANA_DEFAULT_DATASOURCE_NAME") ?: "Prometheus"
        val workers = System.getenv("GRAFANA_WORKERS")?.toInt() ?: (cpus / 2)

        val grafanaController = grafanaController(
            coreV1Api = coreV1Api,
            informerFactory = informerFactory,
            namespace = namespace,
            defaultDatasourceName = grafanaDefaultDatasourceName,
            workers = max(workers, 1)
        )
        controllerManagerBuilder.addController(grafanaController)
    }

    if (System.getenv("VIRTUALSERVICE_DISABLED") == null) {
        val workers = System.getenv("VIRTUALSERVICE_WORKERS")?.toInt() ?: (cpus / 2)

        val virtualServiceController = virtualServiceController(
            networkingV1Api = networkingV1Api,
            virtualServiceApi = virtualServiceApi,
            istioConfig = istioConfig(),
            informerFactory = informerFactory,
            namespace = namespace,
            workers = max(workers, 1)
        )
        controllerManagerBuilder.addController(virtualServiceController)
    }

    informerFactory.startAllRegisteredInformers()

    val controllerManager = controllerManagerBuilder.build()

    controllerManager.run()
}
