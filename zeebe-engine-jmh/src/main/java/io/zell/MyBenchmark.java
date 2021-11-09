/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package io.zell;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceResult;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import java.util.List;
import org.camunda.community.eze.EngineFactory;
import org.camunda.community.eze.ZeebeEngine;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

public class MyBenchmark {

  private static final BpmnModelInstance SIMPLE_PROCESS =
      Bpmn.createExecutableProcess("process").startEvent().endEvent().done();

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  public ProcessInstanceResult testEngineThroughput(Engine engine) {
    return engine
        .zeebeClient
        .newCreateInstanceCommand()
        .bpmnProcessId("process")
        .latestVersion()
        .withResult()
        .send()
        .join();
  }

  @State(Scope.Thread)
  public static class Engine {

    private final ZeebeClient zeebeClient;
    private final ZeebeEngine zeebeEngine;

    public Engine() {
      zeebeEngine = EngineFactory.INSTANCE.create(List.of());
      zeebeClient = zeebeEngine.createClient();
    }

    @Setup
    public void setup() {
      zeebeEngine.start();
      zeebeClient.newDeployCommand().addProcessModel(SIMPLE_PROCESS, "process.bpmn").send().join();
    }

    @TearDown
    public void cleanup() {
      zeebeEngine.stop();
    }
  }
}
