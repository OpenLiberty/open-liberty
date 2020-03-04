/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http2;

import java.util.Set;

import com.ibm.wsspi.http.channel.HttpRequestMessage;

/**
 * HTTP/2 connection handler
 */
public interface Http2ConnectionHandler {

    /**
     * @return String the content types that this handler supports
     */
    public Set<String> getSupportedContentTypes();

    /**
     * Notify this handler that a new HTTP/2 stream has been created by a client
     *
     * @param HttpRequestMessage request
     * @param Http2Stream        stream
     * @param Http2Connection    connection
     * @return
     */
    Http2StreamHandler onStreamCreated(HttpRequestMessage request, Http2Stream stream, Http2Connection connection);

}
