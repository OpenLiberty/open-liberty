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
package io.openliberty.http.ext;

import com.ibm.wsspi.http.HttpResponse;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

/**
 *
 */
public interface HttpResponseExt extends HttpResponse {

    /**
     * Access the first instance found for the given header key. This might be
     * null if no instance was found.
     *
     * @param key
     * @return String
     */
    default String getHeader(HttpHeaderKeys key) {
        return getHeader(key.getName());
    }

    /**
     * Set a header on the message using the provided name and value pair. This
     * will replace any currently existing instances of the header name.
     *
     * @param key
     * @param value
     */
    default void setHeader(HttpHeaderKeys key, String value) {
        setHeader(key.getName(), value);
    }

    /**
     * Set a header on the message using the provided name and value pair if
     * there isn't a value already set for this header.
     *
     * @param key
     * @param value
     * @return old value or null
     */
    default String setHeaderIfAbsent(HttpHeaderKeys key, String value) {
        String oldValue = getHeader(key);
        if (oldValue == null) {
            setHeader(key, value);
        }
        return oldValue;
    }

    /**
     * Remove the target header from the message.
     *
     * @param key
     */
    default void removeHeader(HttpHeaderKeys key) {
        removeHeader(key.getName());
    }

}
