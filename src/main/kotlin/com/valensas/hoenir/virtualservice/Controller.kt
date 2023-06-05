package com.valensas.hoenir.virtualservice

import com.valensas.hoenir.api.VirtualServiceApi
import com.valensas.hoenir.crd.VirtualService
import com.valensas.hoenir.crd.VirtualServiceList
import io.kubernetes.client.extended.controller.Controller
import io.kubernetes.client.extended.controller.builder.ControllerBuilder
import io.kubernetes.client.extended.controller.reconciler.Request
import io.kubernetes.client.extended.workqueue.WorkQueue
import io.kubernetes.client.informer.SharedInformerFactory
import io.kubernetes.client.openapi.apis.NetworkingV1Api
import io.kubernetes.client.util.CallGeneratorParams
import okhttp3.Call

fun virtualServiceInformerCall(
    virtualServiceApi: VirtualServiceApi,
    namespace: String?
): (params: CallGeneratorParams) -> Call =
    if (namespace == null) {
        { params ->
            virtualServiceApi.clusterInformerCall(
                params,
                "istio.valensas.com/ingress=true"
            )
        }
    } else {
        { params ->
            virtualServiceApi.namespacedInformerCall(
                params,
                namespace,
                "istio.valensas.com/ingress=true"
            )
        }
    }

fun virtualServiceController(
    networkingV1Api: NetworkingV1Api,
    virtualServiceApi: VirtualServiceApi,
    istioConfig: IstioConfg,
    informerFactory: SharedInformerFactory,
    namespace: String?,
    workers: Int
): Controller {
    val virtualServiceInformer = informerFactory.sharedIndexInformerFor(
        virtualServiceInformerCall(virtualServiceApi, namespace),
        VirtualService::class.java,
        VirtualServiceList::class.java
    )

    val virtualServiceReconciler = VirtualServiceReconciler(
        virtualServiceInformer,
        virtualServiceApi,
        networkingV1Api,
        istioConfig = istioConfig
    )

    val controller: Controller = ControllerBuilder.defaultBuilder(informerFactory)
        .watch { workQueue: WorkQueue<Request?>? ->
            ControllerBuilder.controllerWatchBuilder(
                VirtualService::class.java,
                workQueue
            ).withOnDeleteFilter { _, _ -> false }
                .build()
        }
        .withReconciler(virtualServiceReconciler)
        .withName("virtualService-controller")
        .withWorkerCount(workers)
        .withReadyFunc(virtualServiceInformer::hasSynced)
        .build()

    return controller
}
