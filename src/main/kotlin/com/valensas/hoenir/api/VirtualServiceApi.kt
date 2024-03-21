package com.valensas.hoenir.api

import com.valensas.hoenir.crd.VirtualService
import com.valensas.hoenir.crd.VirtualServiceList
import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.apis.CustomObjectsApi
import io.kubernetes.client.util.CallGeneratorParams
import io.kubernetes.client.util.generic.GenericKubernetesApi
import okhttp3.Call

class VirtualServiceApi(
    apiClient: ApiClient,
) {
    private val customObjectsApi = CustomObjectsApi(apiClient)

    private val client =
        GenericKubernetesApi(
            VirtualService::class.java,
            VirtualServiceList::class.java,
            VirtualService.API_GROUP,
            VirtualService.API_VERSION,
            VirtualService.PLURAL,
            customObjectsApi,
        )

    fun get(
        name: String,
        namespace: String,
    ): VirtualService {
        val response = client.get(namespace, name).throwsApiException()
        return response.`object`
    }

    fun update(virtualService: VirtualService) {
        client.update(virtualService).throwsApiException()
    }

    fun namespacedInformerCall(
        params: CallGeneratorParams,
        namespace: String,
        labelSelector: String? = null,
    ): Call {
        return customObjectsApi
            .listNamespacedCustomObject(
                VirtualService.API_GROUP,
                VirtualService.API_VERSION,
                namespace,
                VirtualService.PLURAL,
            )
            .labelSelector(labelSelector)
            .resourceVersion(params.resourceVersion)
            .timeoutSeconds(params.timeoutSeconds)
            .watch(params.watch)
            .buildCall(null)
    }

    fun clusterInformerCall(
        params: CallGeneratorParams,
        labelSelector: String? = null,
    ): Call {
        return customObjectsApi
            .listClusterCustomObject(
                VirtualService.API_GROUP,
                VirtualService.API_VERSION,
                VirtualService.PLURAL,
            )
            .labelSelector(labelSelector)
            .resourceVersion(params.resourceVersion)
            .timeoutSeconds(params.timeoutSeconds)
            .watch(params.watch)
            .buildCall(null)
    }
}
