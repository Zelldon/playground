import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.util.ClientBuilder

fun main(args: Array<String>) {

    // https://kubernetes.io/docs/tasks/administer-cluster/access-cluster-api/#java-client
    // file path to your KubeConfig
    // only using kube config doesnt work - for gke
    // We get then
    // Unimplemented
    //	at io.kubernetes.client.util.authenticators.GCPAuthenticator.refresh(GCPAuthenticator.java:61)

    // loading the in-cluster config, including:
    //   1. service-account CA
    //   2. service-account bearer-token
    //   3. service-account namespace
    //   4. master endpoints(ip, port) from pre-set environment variables
    val client = ClientBuilder.cluster().build()
    Configuration.setDefaultApiClient(client)

    // the CoreV1Api loads default api-client from global configuration.
    val api = CoreV1Api()
    val list = api.listNamespacedPod(
        "default",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    ) //.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null);

    for (item in list.items) {
        println(item.metadata!!.name)
    }
    println("Hello World!")
}
