package com.valensas.hoenir.api

import com.valensas.hoenir.crd.VirtualService
import com.valensas.hoenir.crd.VirtualServiceList
import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.apis.CustomObjectsApi
import io.kubernetes.client.util.CallGeneratorParams
import io.kubernetes.client.util.generic.GenericKubernetesApi
import okhttp3.Call

class VirtualServiceApi(
    apiClient: ApiClient
) {
    private val customObjectsApi = CustomObjectsApi(apiClient)

    private val client = GenericKubernetesApi(
        VirtualService::class.java,
        VirtualServiceList::class.java,
        VirtualService.ApiGroup,
        VirtualService.ApiVersion,
        VirtualService.Plural,
        customObjectsApi
    )

    fun get(name: String, namespace: String): VirtualService {
        val response = client.get(namespace, name).throwsApiException()
        return response.`object`
    }

    fun update(virtualService: VirtualService) {
        client.update(virtualService).throwsApiException()
    }

    fun namespacedInformerCall(params: CallGeneratorParams, namespace: String, labelSelector: String? = null): Call {
        return customObjectsApi.listNamespacedCustomObjectCall(
            VirtualService.ApiGroup,
            VirtualService.ApiVersion,
            namespace,
            VirtualService.Plural,
            null,
            null,
            null,
            null,
            labelSelector,
            null,
            params.resourceVersion,
            null,
            params.timeoutSeconds,
            params.watch,
            null
        )
    }

    fun clusterInformerCall(params: CallGeneratorParams, labelSelector: String? = null): Call {
        return customObjectsApi.listClusterCustomObjectCall(
            VirtualService.ApiGroup,
            VirtualService.ApiVersion,
            VirtualService.Plural,
            null,
            null,
            null,
            null,
            labelSelector,
            null,
            params.resourceVersion,
            null,
            params.timeoutSeconds,
            params.watch,
            null
        )
    }
}
