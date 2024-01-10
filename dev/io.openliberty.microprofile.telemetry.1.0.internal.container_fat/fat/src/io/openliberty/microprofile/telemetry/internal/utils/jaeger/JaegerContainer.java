/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal.utils.jaeger;

import io.openliberty.microprofile.telemetry.internal.utils.TestConstants;

import java.io.File;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

import com.ibm.websphere.simplicity.log.Log;

/**
 * A container for the Jaeger trace server
 * <p>
 * Usage:
 *
 * <pre>
 * {@code @ClassName}
 * public static JaegerContainer jaegerContainer = new JaegerContainer().withLogConsumer(new SimpleLogConsumer(MyTest.class, "jaeger"));
 * </pre>
 *
 */
public class JaegerContainer extends GenericContainer<JaegerContainer> {

    private static final Class<?> c = JaegerContainer.class;

    public static final int OTLP_GRPC_PORT = 4317;
    public static final int OTLP_HTTP_PORT = 4318;
    public static final int JAEGER_LEGACY_PORT = 14250;
    public static final int JAEGER_THRIFT_PORT = 14268;
    public static final int ZIPKIN_PORT = 9411;

    public static final int HTTP_QUERY_PORT = 16686;
    public static final int GRPC_QUERY_PORT = 16685;

    public JaegerContainer() {
        this(TestConstants.DOCKER_IMAGE_ALL_IN_ONE);
        Log.info(c, "JaegerContainer", "creating JaegerContainer");
    }

    public JaegerContainer(DockerImageName imageName) {
        super(imageName);
        Log.info(c, "JaegerContainer", "creating JaegerContainer with imageName");

        withExposedPorts(OTLP_GRPC_PORT,
                         OTLP_HTTP_PORT,
                         JAEGER_LEGACY_PORT,
                         JAEGER_THRIFT_PORT,
                         GRPC_QUERY_PORT,
                         HTTP_QUERY_PORT);

        withEnv("COLLECTOR_OTLP_ENABLED", "true");
    }

    public JaegerContainer(File tlsCert, File tlsKey) {
        super(new ImageFromDockerfile().withDockerfileFromBuilder(builder -> builder.from(
                                                                                          ImageNameSubstitutor.instance()
                                                                                                              .apply(TestConstants.DOCKER_IMAGE_ALL_IN_ONE)
                                                                                                              .asCanonicalNameString())
                                                                                    .copy("/etc/certificate.crt", "/etc/certificate.crt")
                                                                                    .copy("/etc/private.key", "/etc/private.key")
                                                                                    .build())
                                       .withFileFromFile("/etc/certificate.crt", tlsCert, 0644)
                                       .withFileFromFile("/etc/private.key", tlsKey, 0644));

        Log.info(c, "JaegerContainer", "creating JaegerContainer with tls certificate and keys");

        withExposedPorts(OTLP_GRPC_PORT,
                         OTLP_HTTP_PORT,
                         JAEGER_LEGACY_PORT,
                         JAEGER_THRIFT_PORT,
                         GRPC_QUERY_PORT,
                         HTTP_QUERY_PORT);

        withEnv("COLLECTOR_OTLP_ENABLED", "true");
        withEnv("COLLECTOR_OTLP_GRPC_TLS_ENABLED", "true");
        withEnv("COLLECTOR_OTLP_GRPC_TLS_CERT", "/etc/certificate.crt");
        withEnv("COLLECTOR_OTLP_GRPC_TLS_KEY", "/etc/private.key");

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
     * Get the port to use to send OTLP spans via HTTP
     * <p>
     * Only valid when the container is started
     *
     * @return the OTLP HTTP port
     */
    public int getOtlpHttpPort() {
        return getMappedPort(OTLP_HTTP_PORT);
    }

    /**
     * Get the URL to use to send spans using the legacy Jaeger protocol
     * <p>
     * Only valid when the container is started
     *
     * @return the legacy Jaeger protocol URL
     */
    public String getJaegerLegacyUrl() {
        return "http://" + getHost() + ":" + getJaegerLegacyPort();
    }

    /**
     * Get the port to use to send spans using the legacy Jaeger protocol
     * <p>
     * Only valid when the container is started
     *
     * @return the legacy Jaeger protocol port
     */
    public int getJaegerLegacyPort() {
        return getMappedPort(JAEGER_LEGACY_PORT);
    }

    /**
     * Get the URL to use to send spans using the legacy Jaeger thrift protocol
     * <p>
     * Only valid when the container is started
     *
     * @return the legacy Jaeger thrift URL
     */
    public String getJaegerThriftUrl() {
        return "http://" + getHost() + ":" + getJaegerThiftPort() + "/api/traces";
    }

    /**
     * Get the port to use to send spans using the legacy Jaeger thrift protocol
     * <p>
     * Only valid when the container is started
     *
     * @return the legacy Jaeger thrift port
     */
    public int getJaegerThiftPort() {
        return getMappedPort(JAEGER_THRIFT_PORT);
    }

    /**
     * Get the port used to query for spans using HTTP
     * <p>
     * This query interface is used by the Jaeger UI but is unstable and undocumented
     * <p>
     * Only valid when the container is started
     *
     * @return the HTTP query port
     */
    public int getQueryHttpPort() {
        return getMappedPort(HTTP_QUERY_PORT);
    }

    /**
     * Get the port used to query for spans using gRPC
     * <p>
     * Only valid when the container is started
     *
     * @return the gRPC query port
     */
    public int getQueryGrpcPort() {
        return getMappedPort(GRPC_QUERY_PORT);
    }
}
