/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty;

import java.util.List;
import java.util.Objects;

import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

import io.netty.handler.codec.http.HttpHeaders;

/**
 *
 */
public class NettyHeaderUtils {

    /**
     * Returns the value of the last occurrence of the given header name.
     * If no headers are found, null is returned.
     *
     * @param headers
     * @param name
     * @return
     */
    public static String getLast(HttpHeaders headers, String name) {
        String value = null;

        List<String> values = headers.getAll(name);
        if (Objects.nonNull(values) && !values.isEmpty()) {
            value = values.get(values.size() - 1);
        }

        return value;
    }

    /**
     * Utility method that sets a Vary header with the given value. If a Vary header
     * already exists, this method will append it to the value using a comma ',' as
     * the delimiter.
     *
     * @param headers
     * @param value
     * @return
     */
    public static void setVary(HttpHeaders headers, String value) {

        Objects.nonNull(headers);
        Objects.nonNull(value);

        String headerValue;

        if (headers.contains(HttpHeaderKeys.HDR_VARY.getName())) {
            headerValue = headers.get(HttpHeaderKeys.HDR_VARY.getName()).toLowerCase();

            if (!headerValue.isEmpty() && !headerValue.contains(value.toLowerCase())) {
                headerValue = new StringBuilder().append(headerValue).append(", ").append(value).toString();
                headers.set(HttpHeaderKeys.HDR_VARY.getName(), headerValue);

            }
        } else {
            headers.set(HttpHeaderKeys.HDR_VARY.getName(), value);
        }
    }

}
