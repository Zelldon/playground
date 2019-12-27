import io.zeebe.client.ZeebeClient
import io.zeebe.client.impl.ZeebeClientFutureImpl
import java.util.concurrent.atomic.AtomicLong

fun main() {
    println("Zeebe Throughput Benchmark")

    val client = ZeebeClient
            .newClientBuilder()
            .brokerContactPoint("localhost:26500")
            .usePlaintext()
            .build()

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

private fun startInstance(client: ZeebeClient) =
        client.newCreateInstanceCommand().bpmnProcessId("simpleProcess").latestVersion().withResult().send() as ZeebeClientFutureImpl<*, *>
