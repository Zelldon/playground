import com.google.cloud.container.v1.ClusterManagerClient
import com.google.container.v1.ListClustersRequest

//
//class ReplacedGCPAuthenticator(val credentials : GoogleCredentials) : Authenticator {
//
//    override fun getName(): String {
//        return "gcp";
//    }
//
//    override fun getToken(config: MutableMap<String, Any>): String {
//        return config["access-token"] as String
//    }
//
//    override fun isExpired(config: MutableMap<String, Any>?): Boolean {
//        val expiryObj = config?.get("expiry")
//        val expiry : Instant;
//        if (expiryObj is Date) {
//            expiry = expiryObj.toInstant();
//        } else if (expiryObj is Instant) {
//            expiry = expiryObj;
//        } else {
//            if (expiryObj !is String) {
//                throw RuntimeException("Unexpected object type: " + expiryObj);
//            }
//            expiry = Instant.parse(expiryObj)
//        }
//        return expiry != null && expiry.compareTo(Instant.now()) <= 0;
//    }
//
//    override fun refresh(config: MutableMap<String, Any>?): MutableMap<String, Any> {
//        try {
//            val accessToken = this.credentials.refreshAccessToken()
//
//            config?.put(ACCESS_TOKEN, accessToken.getTokenValue())
//            config?.put(EXPIRY, accessToken.getExpirationTime())
//        } catch (e : IOException) {
//            throw RuntimeException(e);
//        }
//        return config!!
//    }
//
//    companion object {
//        const val ACCESS_TOKEN = "access-token"
//        const val EXPIRY = "expiry"
//    }
//}
//
//@Throws(IOException::class)
//private fun registerGcloudAuthenticator() {
//    KubeConfig.registerAuthenticator(ReplacedGCPAuthenticator(ServiceAccountCredentials.getApplicationDefault().createScoped("https://www.googleapis.com/auth/compute"
//)));
//}

fun main(args: Array<String>) {

//    registerGcloudAuthenticator()

ClusterManagerClient.create().use {
    val response = it.listClusters("projects/zeebe-io/locations/-");
    println(response.toString())
}
//    // https://kubernetes.io/docs/tasks/administer-cluster/access-cluster-api/#java-client
//    // file path to your KubeConfig
//    // only using kube config doesnt work - for gke
//    // We get then
//    // Unimplemented
//    //	at io.kubernetes.client.util.authenticators.GCPAuthenticator.refresh(GCPAuthenticator.java:61)
//    val kubeConfigPath = System.getenv("HOME") + "/.kube/config";
//
//    // loading the out-of-cluster config, a kubeconfig from file-system
//    val client =
//        ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(FileReader(kubeConfigPath))).build();
//
//    Configuration.setDefaultApiClient(client)
//
//    // the CoreV1Api loads default api-client from global configuration.
//    val api = CoreV1Api()
//    val list = api.listNamespacedPod(
//        "default",
//        null,
//        null,
//        null,
//        null,
//        null,
//        null,
//        null,
//        null,
//        null,
//        null
//    ) //.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null);
//
//    for (item in list.items) {
//        println(item.metadata!!.name)
//    }
    println("Hello World!")
}
