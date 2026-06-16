/*******************************************************************************
 * Copyright (c) 2023, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.utils.otelCollector;

import java.io.File;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import io.openliberty.microprofile.telemetry.internal.utils.TestConstants;

/**
 * A container for the otelCollector trace server
 * <p>
 * Usage:
 *
 * <pre>
 * {@code @ClassName}
 * public static otelCollectorContainer otelCollectorContainer = new OtelCollectorContainer(configFile).withLogConsumer(new SimpleLogConsumer(MyTest.class, "otelCol"));
 * </pre>
 *
 */

public class OtelCollectorContainer extends GenericContainer<OtelCollectorContainer> {

    public static final int OTLP_GRPC_PORT = 4317;
    public static final int PROMETHEUS_METRIC_PORT = 3131;

    public OtelCollectorContainer(File configFile) {
        this(TestConstants.DOCKER_IMAGE_OPENTELEMETRY_COLLECTOR, configFile);
    }

    public OtelCollectorContainer(File configFile, int port) {
        this(TestConstants.DOCKER_IMAGE_OPENTELEMETRY_COLLECTOR, configFile, port);
    }

    public OtelCollectorContainer(File configFile, File tlsCert, File tlsKey) {
        this(TestConstants.DOCKER_IMAGE_OPENTELEMETRY_COLLECTOR, configFile, tlsCert, tlsKey);
    }

    public OtelCollectorContainer(DockerImageName imageName, File configFile) {
        super(TestConstants.DOCKER_IMAGE_OPENTELEMETRY_COLLECTOR);
        withCopyFileToContainer(MountableFile.forHostPath(configFile.toPath()), "/etc/otel-collector-config.yaml");
        withExposedPorts(OTLP_GRPC_PORT);
        withCommand("--config=/etc/otel-collector-config.yaml");
    }

    public OtelCollectorContainer(DockerImageName imageName, File configFile, int PROMETHEUS_METRIC_PORT) {
        super(TestConstants.DOCKER_IMAGE_OPENTELEMETRY_COLLECTOR);
        withCopyFileToContainer(MountableFile.forHostPath(configFile.toPath()), "/etc/otel-collector-config.yaml");
        withExposedPorts(OTLP_GRPC_PORT, PROMETHEUS_METRIC_PORT);
        withCommand("--config=/etc/otel-collector-config.yaml");
    }

    public OtelCollectorContainer(DockerImageName imageName, File configFile, File tlsCert, File tlsKey) {
        super(TestConstants.DOCKER_IMAGE_OPENTELEMETRY_COLLECTOR);
        withCopyFileToContainer(MountableFile.forHostPath(configFile.toPath()), "/etc/otel-collector-config.yaml");
        withCopyFileToContainer(MountableFile.forHostPath(tlsCert.toPath()), "/etc/certificate.crt");
        withCopyFileToContainer(MountableFile.forHostPath(tlsKey.toPath()), "/etc/private.key");
        withExposedPorts(OTLP_GRPC_PORT);
        withCommand("--config=/etc/otel-collector-config.yaml");
    }

    /**
     * Get the port to use to send OTLP spans via gRPC
     * <p>
     * Only valid when the container is started
     *
     * @return the OTLP gRPC port
     */
    public int getOtlpGrpcPort() {
        return getMappedPort(OTLP_GRPC_PORT);
    }

    /**
     * Get the port to use to send OTLP spans via gRPC
     * <p>
     * Only valid when the container is started
     *
     * @return the OTLP gRPC port
     */
    public int getPrometheusMetricPort() {
        return getMappedPort(PROMETHEUS_METRIC_PORT);
    }

    /**
     * Get the URL to use to send OTLP spans via gRPC
     * <p>
     * Only valid when the container is started
     *
     * @return the OTLP gRPC URL
     */
    public String getOtlpGrpcUrl() {
        return "http://" + getHost() + ":" + getOtlpGrpcPort();
    }

    /**
     * Get the URL to use to send OTLP spans via gRPC over tls
     * <p>
     * Only valid when the container is started
     *
     * @return the OTLP gRPC URL
     */
    public String getSecureOtlpGrpcUrl() {
        return "https://" + getHost() + ":" + getOtlpGrpcPort();
    }

    /**
     * Get the URL to use to send OTLP spans via gRPC
     * <p>
     * Only valid when the container is started
     *
     * @return the OTLP gRPC URL
     */
    public String getApiBaseUrl() {
        return "http://" + getHost() + ":" + getPrometheusMetricPort();
    }
}
