package com.valensas.hoenir.grafana

import io.kubernetes.client.extended.controller.Controller
import io.kubernetes.client.extended.controller.builder.ControllerBuilder
import io.kubernetes.client.extended.controller.reconciler.Request
import io.kubernetes.client.extended.workqueue.WorkQueue
import io.kubernetes.client.informer.SharedInformerFactory
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1ConfigMap
import io.kubernetes.client.openapi.models.V1ConfigMapList
import io.kubernetes.client.util.CallGeneratorParams
import okhttp3.Call

fun grafanaInformerCall(
    coreV1Api: CoreV1Api,
    namespace: String?
): (params: CallGeneratorParams) -> Call =
    if (namespace == null) {
        { params ->
            coreV1Api.listConfigMapForAllNamespacesCall(
                null,
                null,
                null,
                "grafana_dashboard=1, grafana.valensas.com/dashboard",
                null,
                null,
                params.resourceVersion,
                null,
                true,
                params.timeoutSeconds,
                params.watch,
                null
            )
        }
    } else {
        { params ->
            coreV1Api.listNamespacedConfigMapCall(
                namespace,
                null,
                null,
                null,
                null,
                "grafana_dashboard=1, grafana.valensas.com/dashboard",
                null,
                params.resourceVersion,
                null,
                true,
                params.timeoutSeconds,
                params.watch,
                null
            )
        }
    }

fun grafanaController(
    coreV1Api: CoreV1Api,
    informerFactory: SharedInformerFactory,
    namespace: String?,
    defaultDatasourceName: String,
    workers: Int
): Controller {
    val configInformer = informerFactory.sharedIndexInformerFor(
        grafanaInformerCall(coreV1Api, namespace),
        V1ConfigMap::class.java,
        V1ConfigMapList::class.java
    )

    val grafanaDashboardReconciler = GrafanaDashboardReconciler(
        configInformer,
        coreV1Api,
        defaultDatasourceName
    )

    val controller: Controller = ControllerBuilder.defaultBuilder(informerFactory)
        .watch { workQueue: WorkQueue<Request?>? ->
            ControllerBuilder.controllerWatchBuilder(
                V1ConfigMap::class.java,
                workQueue
            )
                .build()
        }
        .withReconciler(grafanaDashboardReconciler)
        .withName("grafana-dashboard-controller")
        .withWorkerCount(workers)
        .withReadyFunc(configInformer::hasSynced)
        .build()

    return controller
}
