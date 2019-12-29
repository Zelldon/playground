import io.zeebe.client.CredentialsProvider
import io.zeebe.client.ZeebeClient
import io.zeebe.client.impl.ZeebeClientFutureImpl
import io.zeebe.client.impl.oauth.OAuthCredentialsProvider
import io.zeebe.client.impl.oauth.OAuthCredentialsProviderBuilder
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.exitProcess

fun main() {
    println("Zeebe Throughput Benchmark")

    val client = createClient()

    println(client.newTopologyRequest().send().join());

    client.newDeployCommand()
            .addResourceStringUtf8({}.javaClass.getResource("simple-process.bpmn").readText(Charsets.UTF_8), "simple-process.bpmn")
            .send()
            .join()

    val seconds = 30
    println("Will run for %d".format(seconds))
    val startTime = System.currentTimeMillis()
    val expectedEndtime = startTime + (seconds * 1000)

    val benchmarkResult = List(seconds, { AtomicLong() })

    do {
        val startSecond = System.currentTimeMillis()

        val workflowInstanceBatchSize = 100
        for (i in 1..workflowInstanceBatchSize) {

            startInstance(client).whenComplete { _, t ->
                if (t == null) {
                    val expiredSeconds = (System.currentTimeMillis() - startTime) / 1000.0
                    val correspondingSecond = kotlin.math.floor(expiredSeconds).toInt()

                    benchmarkResult.get(correspondingSecond).incrementAndGet();
                }
                else
                {
                    println(t)
                }
            }
        }

        val end = System.currentTimeMillis()
        val diff = end - startSecond
        if (diff < 1000)
        {
            Thread.sleep(1000 - diff)
        }

        val currentTime = System.currentTimeMillis();
    } while (currentTime < expectedEndtime)


    val endTime = System.currentTimeMillis()

    println("Result after %d milliseconds".format(endTime - startTime))
    println(benchmarkResult)
}

private fun createClient(): ZeebeClient {
    return ZeebeClient
            .newClientBuilder()
            .credentialsProvider(
                    OAuthCredentialsProviderBuilder()
                            .audience("8e6feed1-d120-4d36-86ac-fbd53b23f2a2.zeebe.ultrawombat.com")
                            .authorizationServerUrl("https://login.cloud.ultrawombat.com/oauth/token")
                            .clientId("OgNtrArKHh0tQqkZzf95kcTDHWho1BLw")
                            .clientSecret("1pntn6611b24NYE63jxdqsOEfat6MQ18I83o_5heV7JUZAldPRm8c0QosfG0KTJp")
                            .build())
            .brokerContactPoint("8e6feed1-d120-4d36-86ac-fbd53b23f2a2.zeebe.ultrawombat.com:443")
            .build()
}

private fun startInstance(client: ZeebeClient) =
        client.newCreateInstanceCommand().bpmnProcessId("simpleProcess").latestVersion().withResult().send() as ZeebeClientFutureImpl<*, *>
