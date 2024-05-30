package com.valensas.hoenir.grafana

import com.google.gson.Gson
import com.valensas.hoenir.fullname
import io.kubernetes.client.extended.controller.reconciler.Reconciler
import io.kubernetes.client.extended.controller.reconciler.Request
import io.kubernetes.client.extended.controller.reconciler.Result
import io.kubernetes.client.informer.SharedIndexInformer
import io.kubernetes.client.informer.cache.Lister
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1ConfigMap
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory

class GrafanaDashboardReconciler(
    configMapInformer: SharedIndexInformer<V1ConfigMap>,
    private val api: CoreV1Api,
    private val defaultDatasourceName: String,
) : Reconciler {
    private val configMapLister: Lister<V1ConfigMap> = Lister(configMapInformer.indexer)
    private val dashboardCache: MutableMap<String, Pair<String, String?>> = mutableMapOf()
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun reconcile(request: Request): Result {
        try {
            logger.info("Reconciling ConfigMap {}", request.fullname())

            val configMap = configMapLister.get(request.fullname()) ?: return Result(false)

            val id = configMap.metadata!!.labels!!["grafana.valensas.com/dashboard"]!!
            val revision = configMap.metadata!!.labels!!["grafana.valensas.com/revision"]
            val cached = dashboardCache[request.name]

            if (cached != null) {
                if (cached.first != id || cached.second != revision) {
                    dashboardCache[request.name] = id to revision
                    updateConfigMap(configMap, id, revision)
                }
            } else {
                dashboardCache[request.name] = id to revision
                updateConfigMap(configMap, id, revision)
            }
            logger.info("Successfully reconciled ConfigMap {}", request.fullname())
            return Result(false)
        } catch (e: Throwable) {
            logger.error("Error reconciling ${request.namespace}/${request.name}", e)
            return Result(true)
        }
    }

    private fun updateConfigMap(
        configMap: V1ConfigMap,
        id: String,
        revision: String?,
    ) {
        val name = configMap.metadata!!.name
        logger.info("Updating ConfigMap {}, with dashboard id {}, revision {}", configMap.fullname(), id, revision)
        configMap.data?.entries?.clear()
        try {
            configMap.putDataItem("$id.json", downloadDashboard(id, revision))
            configMap.metadata!!.annotations?.remove("grafana.valensas.com/error")
        } catch (e: Exception) {
            // We were unable to download the dashboard from grafana.com, most likely because
            // of a wrong dashboard id/revision or hitting rate limit quotas. Catch the error
            // here and do not rethrow as requeuing the request will most likely fail again.
            logger.error("Error updating ConfigMap ${configMap.fullname()}", e)
            configMap.metadata!!.putAnnotationsItem("grafana.valensas.com/error", e.message)
        }

        api.replaceNamespacedConfigMap(name, configMap.metadata!!.namespace, configMap).execute()
    }

    private fun downloadDashboard(
        id: String,
        revision: String?,
    ): String {
        val rev = revision ?: "latest"
        logger.info("Downloading dashboard {}, revision {}", id, rev)

        val client = OkHttpClient()
        val request =
            okhttp3.Request.Builder()
                .url("https://grafana.com/api/dashboards/$id/revisions/$rev/download")
                .build()

        val response = client.newCall(request).execute()
        val bodyStr = response.body?.string()!!
        if (!response.isSuccessful) {
            throw Exception("Invalid Request, check your id and revision labels.")
        }
        val map = Gson().fromJson(bodyStr, Map::class.java)

        val inputs = map["__inputs"] ?: return bodyStr

        if (inputs !is List<*>) {
            throw Exception("Dashboard inputs is not a list")
        }

        logger.info("Successfully downloaded dashboard {}, revision {}", id, rev)

        val datasource =
            inputs.find {
                (it as? Map<*, *>)?.get("type") == "datasource"
            } as? Map<*, *>
        val datasourceName = datasource?.get("name") as? String ?: return bodyStr

        val toBeReplaced = "\${$datasourceName}"
        return bodyStr.replace(toBeReplaced, defaultDatasourceName)
    }
}
