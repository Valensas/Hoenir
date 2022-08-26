package com.valensas.hoenir.crd

import io.kubernetes.client.common.KubernetesListObject
import io.kubernetes.client.common.KubernetesObject
import io.kubernetes.client.openapi.models.V1ListMeta
import io.kubernetes.client.openapi.models.V1ObjectMeta

data class VirtualService(
    private val apiVersion: String,
    private val kind: String,
    private val metadata: V1ObjectMeta,
    val spec: VirtualServiceSpec
) : KubernetesObject {
    companion object {
        const val ApiGroup = "networking.istio.io"
        const val ApiVersion = "v1beta1"
        const val Plural = "virtualservices"
    }
    override fun getApiVersion(): String = apiVersion
    override fun getKind(): String = kind
    override fun getMetadata(): V1ObjectMeta = metadata
}

data class VirtualServiceSpec(
    val hosts: List<String>?,
    val http: List<HttpElement>
)

data class HttpElement(
    val match: List<HTTPMatchRequest>?,
    val route: Any?,
    val redirect: Any?,
    val delegate: Any?
)

data class HTTPMatchRequest(
    val uri: HTTPMatchRequestUri?
)

data class HTTPMatchRequestUri(
    val exact: String?,
    val prefix: String?
)

data class VirtualServiceList(
    private val apiVersion: String,
    private val kind: String,
    private val metadata: V1ListMeta,
    private val items: MutableList<VirtualService>
) : KubernetesListObject {
    override fun getApiVersion(): String = apiVersion
    override fun getKind(): String = kind
    override fun getMetadata(): V1ListMeta = metadata
    override fun getItems(): MutableList<VirtualService> = items
}
