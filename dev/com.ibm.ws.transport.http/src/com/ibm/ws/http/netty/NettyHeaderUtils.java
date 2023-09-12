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

}
