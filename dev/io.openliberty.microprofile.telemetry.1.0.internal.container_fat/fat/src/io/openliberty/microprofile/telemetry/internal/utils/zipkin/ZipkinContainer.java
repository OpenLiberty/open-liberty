/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal.utils.zipkin;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.openliberty.microprofile.telemetry.internal.utils.TestConstants;

/**
 * A container for the Zipkin trace server
 * <p>
 * Usage:
 *
 * <pre>
 * {@code @ClassName}
 * public static ZipkinContainer zipkinContainer = new ZipkinContainer().withLogConsumer(new SimpleLogConsumer(MyTest.class, "zipkin"));
 * </pre>
 */
//TODO switch to use ghcr.io/openzipkin/zipkin-slim:2.23
public class ZipkinContainer extends GenericContainer<ZipkinContainer> {

    public static final int HTTP_PORT = 9411;

    public ZipkinContainer() {
        this(TestConstants.DOCKER_IMAGE_ZIPKIN_SLIM);
    }

    public ZipkinContainer(DockerImageName imageName) {
        super(imageName);

        withExposedPorts(HTTP_PORT);
    }

    /**
     * Get the port for the HTTP API
     * <p>
     * Only valid when the container is started
     *
     * @return the HTTP API port
     */
    public int getHttpPort() {
        return getMappedPort(HTTP_PORT);
    }

    /**
     * Get the base URL of the HTTP API
     * <p>
     * Only valid when the container is started
     *
     * @return The HTTP API base URL
     */
    public String getApiBaseUrl() {
        return "http://" + getHost() + ":" + getHttpPort() + "/api/v2";
    }

}
