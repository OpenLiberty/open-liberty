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

/**
 * HTTP/2 stream
 */
public interface Http2Stream {

    /**
     * Send headers to be written
     */
    public void writeHeaders(Map<String, String> pseudoHeaders, Map<String, String> headers, boolean endOfHeaders, boolean endOfStream);

    /**
     * Send trailers
     */
    public void writeData(byte[] buffer, boolean endOfStream);

    /**
     * Send trailers
     */
    public void writeTrailers(Map<String, String> headers);

    /**
     * Send a RST_STREAM to terminate the stream
     *
     * @param reason
     */
    public void cancel(Exception reason, int code);

    /**
     * @return
     */
    public int getId();
}
