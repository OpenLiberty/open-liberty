/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
     * Remove the target header from the message.
     *
     * @param key
     */
    default void removeHeader(HttpHeaderKeys key) {
        removeHeader(key.getName());
    }

}
