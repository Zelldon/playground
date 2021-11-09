# Zeebe Engine JMH

Run with eze (0.3.0) gives:

```
Result "io.zell.MyBenchmark.testEngineThroughput":
  866.911 ±(99.9%) 39.311 ops/s [Average]
  (min, avg, max) = (766.073, 866.911, 972.840), stdev = 52.479
  CI (99.9%): [827.600, 906.221] (assumes normal distribution)


# Run complete. Total time: 00:08:34

REMEMBER: The numbers below are just data. To gain reusable insights, you need to follow up on
why the numbers are the way they are. Use profilers (see -prof, -lprof), design factorial
experiments, perform baseline and negative tests that provide experimental control, make sure
the benchmarking environment is safe on JVM/OS/HW level, ask for reviews from the domain experts.
Do not assume the numbers tell you what you want them to tell.

Benchmark                          Mode  Cnt    Score    Error  Units
MyBenchmark.testEngineThroughput  thrpt   25  866.911 ± 39.311  ops/s
```


