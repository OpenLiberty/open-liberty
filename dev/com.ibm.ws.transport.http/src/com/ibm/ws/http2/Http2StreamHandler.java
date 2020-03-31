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

import java.util.Map;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;

/**
 * HTTP/2 stream handler
 */
public interface Http2StreamHandler {

    /**
     * Invoked when complete headers have been received for a stream
     */
    public void headersReady();

    /**
     * A complete body has been received for a stream
     */
    public void dataReady(WsByteBuffer buffer, boolean endOfStream);

    /**
     * Send headers to be written
     */
    public void writeHeaders(Map<String, String> headers, boolean endOfHeaders, boolean endOfStream);
}
