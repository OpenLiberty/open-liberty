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

import com.ibm.wsspi.http.channel.HttpConstants;
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

        String varyName = HttpHeaderKeys.HDR_VARY.getName();

        if (headers.contains(varyName)) {
            String headerValue = headers.get(varyName);
            if (!headerValue.isEmpty() && !headerValue.toLowerCase().contains(value.toLowerCase())) {
                headers.set(varyName, headerValue + ", " + value);
            }
        } else {
            headers.set(varyName, value);
        }
    }

    /**
     * Strip whitespace from a given String.
     *
     * @param string The String to strip whitespace from.
     * @return an empty String if the given String is null, otherwise a String with all whitespace removed.
     */
    public static String stripWhiteSpace(String string) {
        return Objects.isNull(string) ? HttpConstants.EMPTY_STRING : string.replaceAll("\\s+", "");
    }

}
