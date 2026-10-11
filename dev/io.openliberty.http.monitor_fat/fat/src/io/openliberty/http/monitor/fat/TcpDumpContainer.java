/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.monitor.fat;

import java.util.Objects;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.RepeatTestFilter;

/**
 * Runs in the network space of another container and runs {@code tcpdump} to capture its network traffic
 * <p>
 * Has some limitations - it has to start after the other container and exit before it, so it can't capture startup or shutdown traffic.
 * <p>
 * Example usage:
 * <p>
 *
 * <pre>
 * public static JaegerContainer jaegerContainer = new JaegerContainer().withLogConsumer(new SimpleLogConsumer(MyTest.class, "jaeger"));
 * public static TcpDumpContainer tcpDumpContainer = new TcpDumpContainer(jaegerContainer).withLogConsumer(new SimpleLogConsumer(MyTest.class, "tcpdump"));
 * public static RepeatTests repeat = TelemetryActions.latestTelemetryRepeats(SERVER_NAME);
 *
 * {@code @ClassRule}
 * public static RuleChain chain = RuleChain.outerRule(jaegerContainer).around(tcpDumpContainer).around(repeat);
 * </pre>
 *
 * <p>
 * By default, the packet capture is output to {@code results/tcpDump-TestClass_RepeatAction}
 */
public class TcpDumpContainer extends GenericContainer<TcpDumpContainer> {

    private static final Class<?> c = TcpDumpContainer.class;

    private String dumpFileDestination;
    private String testName;
    private final Container<?> containerToMonitor;

    /**
     * Create a tcpdump container to monitor the given container.
     * <p>
     * When using this constructor, the container must be used as a test rule. If you're not using it as a test rule, use {@link #TcpDumpContainer(Container, String)} instead.
     *
     * @param containerToMonitor the container to capture packets from
     */
    public TcpDumpContainer(Container<?> containerToMonitor) {
        this(containerToMonitor, null);
    }

    /**
     * Create a tcpdump container to monitor the given container.
     *
     * @param containerToMonitor the container to capture packets from
     * @param testName
     */
    public TcpDumpContainer(Container<?> containerToMonitor, String testName) {
        // Use Alpine image with tcpdump installed
        super(new ImageFromDockerfile().withDockerfileFromBuilder(builder -> builder.from(
                                                                                          ImageNameSubstitutor.instance().apply(DockerImageName.parse("alpine:3.17"))
                                                                                                          .asCanonicalNameString())
                        .run("apk add --no-cache tcpdump")
                        .cmd("tcpdump -w tcpdump.dump")
                        .build()));
        Objects.requireNonNull(containerToMonitor, "containerToMontior must not be null");
        this.containerToMonitor = containerToMonitor;
        this.testName = testName;
    }

    /**
     * Set a different output location for the package capture file
     *
     * @param destination the destination path, relative to the working directory
     * @return {@code this}
     */
    public TcpDumpContainer withDumpFileDestination(String destination) {
        dumpFileDestination = destination;
        return this;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        // Snaffle out the test class name
        if (testName == null) {
            testName = description.getClassName();
        }
        return super.apply(base, description);
    }

    @Override
    public void start() {
        if (dumpFileDestination == null && testName == null) {
            throw new RuntimeException("Either dump file destination or test name must be set");
        }
        if (!containerToMonitor.isRunning()) {
            throw new RuntimeException("The container to monitor must be started before the tcpdump container");
        }
        // Wait until start time to grab the containerId of the container we're monitoring because it's not available until that container has started.
        withNetworkMode("container:" + containerToMonitor.getContainerId());
        super.start();
    }

    @Override
    public void stop() {
        String destination = dumpFileDestination;
        if (destination == null) {
            destination = "results/tcpdump-" + testName + RepeatTestFilter.getRepeatActionsAsString();
        }

        // Before we stop, attempt to extract the dump file
        try {
            // Signal tcpdump to flush to file
            execInContainer("kill", "1");
            Thread.sleep(500);
            Log.info(c, "stop", "helooooooooo");
            copyFileFromContainer("tcpdump.dump", destination);
        } catch (Exception e) {
            Log.error(c, "stop", e, "Failed to extract dump file from container");
        }
        super.stop();
    }

}