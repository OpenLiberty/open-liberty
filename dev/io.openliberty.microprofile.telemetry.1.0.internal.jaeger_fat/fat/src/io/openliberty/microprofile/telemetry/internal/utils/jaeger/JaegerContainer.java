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

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

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

    public static final DockerImageName IMAGE_NAME = DockerImageName.parse("jaegertracing/all-in-one:1.39");

    public static final int OLTP_GRPC_PORT = 4317;
    public static final int OLTP_HTTP_PORT = 4318;
    public static final int JAEGER_LEGACY_PORT = 14250;
    public static final int JAEGER_THRIFT_PORT = 14268;
    public static final int ZIPKIN_PORT = 9411;

    public static final int HTTP_QUERY_PORT = 16686;
    public static final int GRPC_QUERY_PORT = 16685;

    public JaegerContainer() {
        this(IMAGE_NAME);
    }

    public JaegerContainer(DockerImageName imageName) {
        super(imageName);

        withExposedPorts(OLTP_GRPC_PORT,
                         OLTP_HTTP_PORT,
                         JAEGER_LEGACY_PORT,
                         JAEGER_THRIFT_PORT,
                         GRPC_QUERY_PORT,
                         HTTP_QUERY_PORT);

        withEnv("COLLECTOR_OTLP_ENABLED", "true");
    }

    /**
     * Get the port to use to send OLTP spans via gRPC
     * <p>
     * Only valid when the container is started
     *
     * @return the OLTP gRPC port
     */
    public int getOltpGrpcPort() {
        return getMappedPort(OLTP_GRPC_PORT);
    }

    /**
     * Get the URL to use to send OLTP spans via gRPC
     * <p>
     * Only valid when the container is started
     *
     * @return the OLTP gRPC URL
     */
    public String getOltpGrpcUrl() {
        return "http://" + getHost() + ":" + getOltpGrpcPort();
    }

    /**
     * Get the port to use to send OLTP spans via HTTP
     * <p>
     * Only valid when the container is started
     *
     * @return the OLTP HTTP port
     */
    public int getOltpHttpPort() {
        return getMappedPort(OLTP_HTTP_PORT);
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
