/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

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

    //TODO switch to use ghcr.io/open-telemetry/opentelemetry-collector-releases/opentelemetry-collector:0.117.0
    //TODO remove withDockerfileFromBuilder and instead create a dockerfile
    public OtelCollectorContainer(DockerImageName imageName, File configFile) {
        super(new ImageFromDockerfile().withDockerfileFromBuilder(builder -> builder.from(
                                                                                          ImageNameSubstitutor.instance()
                                                                                                              .apply(TestConstants.DOCKER_IMAGE_OPENTELEMETRY_COLLECTOR)
                                                                                                              .asCanonicalNameString())
                                                                                    .copy("/etc/otel-collector-config.yaml", "/etc/otel-collector-config.yaml")
                                                                                    .build())
                                       .withFileFromFile("/etc/otel-collector-config.yaml", configFile, 0644));
        withExposedPorts(OTLP_GRPC_PORT);
        withCommand("--config=/etc/otel-collector-config.yaml");
    }

    //TODO switch to use ghcr.io/open-telemetry/opentelemetry-collector-releases/opentelemetry-collector:0.117.0
    //TODO remove withDockerfileFromBuilder and instead create a dockerfile
    public OtelCollectorContainer(DockerImageName imageName, File configFile, int PROMETHEUS_METRIC_PORT) {
        super(new ImageFromDockerfile().withDockerfileFromBuilder(builder -> builder.from(
                                                                                          ImageNameSubstitutor.instance()
                                                                                                              .apply(TestConstants.DOCKER_IMAGE_OPENTELEMETRY_COLLECTOR)
                                                                                                              .asCanonicalNameString())
                                                                                    .copy("/etc/otel-collector-config.yaml", "/etc/otel-collector-config.yaml")
                                                                                    .build())
                                       .withFileFromFile("/etc/otel-collector-config.yaml", configFile, 0644));
        withExposedPorts(OTLP_GRPC_PORT, PROMETHEUS_METRIC_PORT);
        withCommand("--config=/etc/otel-collector-config.yaml");
    }

    //TODO switch to use ghcr.io/open-telemetry/opentelemetry-collector-releases/opentelemetry-collector:0.117.0
    //TODO remove withDockerfileFromBuilder and instead create a dockerfile
    public OtelCollectorContainer(DockerImageName imageName, File configFile, File tlsCert, File tlsKey) {
        super(new ImageFromDockerfile().withDockerfileFromBuilder(builder -> builder.from(
                                                                                          ImageNameSubstitutor.instance()
                                                                                                              .apply(TestConstants.DOCKER_IMAGE_OPENTELEMETRY_COLLECTOR)
                                                                                                              .asCanonicalNameString())
                                                                                    .copy("/etc/certificate.crt", "/etc/certificate.crt")
                                                                                    .copy("/etc/private.key", "/etc/private.key")
                                                                                    .copy("/etc/otel-collector-config.yaml", "/etc/otel-collector-config.yaml")
                                                                                    .build())
                                       .withFileFromFile("/etc/otel-collector-config.yaml", configFile, 0644)
                                       .withFileFromFile("/etc/certificate.crt", tlsCert, 0644)
                                       .withFileFromFile("/etc/private.key", tlsKey, 0644));
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
