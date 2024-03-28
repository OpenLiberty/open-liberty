/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.constants;

/**
 *
 */
public enum Http2Option implements HttpEndpointOption {

    CONNECTION_IDLE_TIMEOUT("http2ConnectionIdleTimeout", 0),
    MAX_CONCURRENT_STREAMS("maxConcurrentStreams", 200),
    MAX_FRAME_SIZE("connectionWindowSize", 57344),
    CONNECTION_WINDOW_SIZE("connectionWindowSize", 65535),
    LIMIT_WINDOW_UPDATE_FRAMES("limitWindowUpdateFrames", Boolean.FALSE);

    String id;
    Object defaultValue;

    Http2Option(String id, Object defaultValue) {
        this.id = id;
        this.defaultValue = defaultValue;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Object value() {
        return defaultValue;
    }

}
