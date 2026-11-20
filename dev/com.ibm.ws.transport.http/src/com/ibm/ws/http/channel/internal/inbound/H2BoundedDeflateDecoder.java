/*******************************************************************************
 * Copyright (c) 2004, 2026 IBM Corporation and others.
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
package com.ibm.ws.http.channel.internal.inbound;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpMessages;

/**
 * Bounded deflate decoder for a sealed HTTP/2 request body.
 *
 */
final class H2BoundedDeflateDecoder implements H2BoundedDecompressionHandler {

    /** RAS variable */
    private static final TraceComponent tc = Tr.register(H2BoundedDeflateDecoder.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Inflater used for this handler instance */
    private Inflater inflater = null;
    /** Maximum output produced by one pull. */
    private static final int OUTPUT_CHUNK = 16 * 1024;

    // running counts including resets
    private long countRead = 0;

    // The caller retains the offered array until this decoder needs input again.
    private byte[] boundedInput;
    private int boundedLength;
    private int boundedOffset;
    private boolean boundedEnd;
    private boolean boundedClosed;

    /**
     * Create a decoder for one sealed body pass.
     *
     */
    H2BoundedDeflateDecoder() {
        this.inflater = new Inflater(false);
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Created a bounded deflate decoder; " + this);
            }
        } catch (RuntimeException | Error failure) {
            this.inflater.end();
            this.inflater = null;
            throw failure;
        }
    }

    private void reset() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "reset");
        }
        this.countRead += this.inflater.getBytesRead();
        this.inflater.reset();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "reset done on inflater, counters updated");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "reset");
        }
    }

    @Override
    public void offer(byte[] input, int length) {
        if (boundedClosed || boundedEnd || boundedInput != null || input == null || length <= 0 || length > input.length) {
            throw new IllegalStateException("Deflate input is not ready for another bounded segment");
        }
        boundedInput = input;
        boundedLength = length;
        boundedOffset = 0;
    }

    @Override
    public boolean needsInput() {
        return boundedInput == null;
    }

    @Override
    public void endInput() {
        boundedEnd = true;
    }

    @Override
    public int pull(byte[] output, int offset, int length) throws DataFormatException {
        if (boundedClosed || output == null || offset < 0 || length < 0 || offset > output.length - length) {
            throw new IllegalStateException("Invalid bounded deflate pull");
        }
        if (length == 0) {
            return 0;
        }
        int wanted = Math.min(length, OUTPUT_CHUNK);
        while (true) {
            if (inflater.finished()) {
                if (boundedInput != null && boundedOffset < boundedLength) {
                    reset(); // concatenated zlib streams, matching the existing handler
                    continue;
                }
                boundedInput = null;
                return boundedEnd ? -1 : 0;
            }
            if (inflater.needsInput()) {
                if (boundedInput != null && boundedOffset < boundedLength) {
                    inflater.setInput(boundedInput, boundedOffset, boundedLength - boundedOffset);
                }
            }
            long before = inflater.getBytesRead();
            int produced = inflater.inflate(output, offset, wanted);
            boundedOffset += (int) (inflater.getBytesRead() - before);
            if (produced > 0) {
                return produced;
            }
            if (inflater.finished()) {
                continue;
            }
            if (inflater.needsDictionary()) {
                throw new DataFormatException("Deflate body requires a dictionary");
            }
            if (inflater.needsInput() && (boundedInput == null || boundedOffset == boundedLength)) {
                boundedInput = null;
                if (boundedEnd) {
                    throw new DataFormatException("Truncated deflate body");
                }
                return 0;
            }
            throw new DataFormatException("Deflate body made no progress");
        }
    }

    @Override
    public void close() {
        boundedClosed = true;
        boundedInput = null;
        this.inflater.end();
    }

    @Override
    public long getBytesRead() {
        return this.countRead + this.inflater.getBytesRead();
    }

}
