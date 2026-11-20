/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.inbound;

import java.util.zip.DataFormatException;

/** A single HTTP/2 sealed-file decoding pass with one retained raw segment. */
interface H2BoundedDecompressionHandler {
    /** The caller keeps this array unchanged until needsInput() returns true. */
    void offer(byte[] input, int length);

    /** Returns at most 16 KiB, 0 when more raw input is needed, or -1 after validated EOF. */
    int pull(byte[] output, int offset, int length) throws DataFormatException;

    boolean needsInput();

    /** No more raw bytes exist. Truncated input is reported by the next pull. */
    void endInput();

    long getBytesRead();

    void close();
}
