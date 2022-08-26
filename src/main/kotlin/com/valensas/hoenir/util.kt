package com.valensas.hoenir

import io.kubernetes.client.common.KubernetesObject
import io.kubernetes.client.extended.controller.reconciler.Request
import io.kubernetes.client.openapi.ApiException
import io.kubernetes.client.openapi.apis.NetworkingV1Api
import io.kubernetes.client.openapi.models.V1Ingress

fun Request.fullname() = "$namespace/$name"

fun KubernetesObject.fullname() = "${metadata.namespace!!}/${metadata.name!!}"

fun NetworkingV1Api.createOrReplaceNamespacedIngress(
    ingress: V1Ingress
) {
    try {
        replaceNamespacedIngress(
            ingress.metadata!!.name!!,
            ingress.metadata!!.namespace!!,
            ingress,
            null,
            null,
            null,
            null,
        )
    } catch (e: ApiException) {
        if (e.code != 404) {
            throw e
        }
        createNamespacedIngress(
            ingress.metadata!!.namespace!!,
            ingress,
            null,
            null,
            null,
            null,
        )
    }
}
