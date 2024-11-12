/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.netty.handler.codec.http2;


/**
 * Liberty specific decoder class for establishing header limits for HTTP 2.0 connections.
 */
public class LibertyDefaultHttp2HeadersDecoder extends DefaultHttp2HeadersDecoder {
	
	private long maxHeaderBlockSize = 0;

	
	/**
     * Create a new instance for Liberty specific decoding.
     * @param validateHeaders {@code true} to validate headers are valid according to the RFC.
     * @param maxHeaderBlockSize This is the configuration decided by Liberty and it matches the
     * behavior of the SETTINGS_MAX_HEADER_LIST_SIZE HTTP2 setting so we will use it as such.
     * @param limitFieldSize limit the name and value field sizes to be at most this length
     * @param limitNumHeaders limit the amount of headers for the stream
     */
    public LibertyDefaultHttp2HeadersDecoder(boolean validateHeaders, long maxHeaderListSize, long maxHeaderBlockSize, int limitFieldSize, int limitNumHeaders) {
        super(validateHeaders, false, new HpackDecoder(maxHeaderListSize, limitFieldSize, limitNumHeaders));
        this.maxHeaderBlockSize = maxHeaderBlockSize;
    }
    
    /**
     * Return the maximum number of bytes allowed in a header block.
     */
    long maxHeaderBlock() {
        return maxHeaderBlockSize;
    }
    
}