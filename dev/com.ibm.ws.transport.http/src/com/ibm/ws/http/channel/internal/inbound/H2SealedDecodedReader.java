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

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.DataFormatException;

import com.ibm.ws.http.channel.internal.h2spool.H2SealedBody;
import com.ibm.wsspi.http.channel.HttpConstants;
import com.ibm.wsspi.http.channel.exception.IllegalHttpBodyException;

/** A single, bounded decode pass over the claimed raw spool file. */
final class H2SealedDecodedReader implements AutoCloseable {
    static final int OUTPUT_CHUNK = 16 * 1024;
    private static final int RAW_CHUNK = 64 * 1024;

    private final H2SealedBody body;
    private final String encoding;
    private final H2BoundedDecompressionHandler decoder;
    private final Runnable onRetired;
    private final byte[] raw;
    private final int firstRawSegment;
    private final int bodyRawSegment;
    private final int initialRawWindow;
    private final String ratioMessage;
    private final long ratioLimit;
    private final int ratioTolerance;
    private final boolean preserveSingleReadTransactions;
    private final ReentrantLock readLock = new ReentrantLock();
    private final AtomicBoolean closeRequested = new AtomicBoolean();
    private long rawPosition;
    private long decodedBytes;
    private long validatedDecodedLength = -1;
    private long committedDecodedBytes;
    private long cyclesAboveRatio;
    private IllegalHttpBodyException deferredBodyFailure;
    private boolean segmentProduced;
    private boolean rawEnded;
    private boolean decodedEnded;
    private boolean validated;
    private boolean preflightComplete;
    private boolean closed;

    H2SealedDecodedReader(H2SealedBody body, String encoding, int ratioLimit, int ratioTolerance,
                          Runnable onRetired, boolean preserveSingleReadTransactions) {
        this(body, encoding, ratioLimit, ratioTolerance, onRetired, preserveSingleReadTransactions,
                        RAW_CHUNK, RAW_CHUNK, RAW_CHUNK, "Decompression tolerance reached");
    }

    H2SealedDecodedReader(H2SealedBody body, String encoding, int ratioLimit, int ratioTolerance,
                          Runnable onRetired, boolean preserveSingleReadTransactions,
                          int firstRawSegment, int bodyRawSegment, int initialRawWindow, String ratioMessage) {
        this.body = Objects.requireNonNull(body);
        this.encoding = encoding;
        this.onRetired = onRetired;
        this.ratioLimit = ratioLimit;
        this.ratioTolerance = ratioTolerance;
        this.preserveSingleReadTransactions = preserveSingleReadTransactions;
        if (firstRawSegment <= 0 || bodyRawSegment <= 0 || initialRawWindow < firstRawSegment) {
            throw new IllegalArgumentException("Invalid sealed body segment sizes");
        }
        this.firstRawSegment = firstRawSegment;
        this.bodyRawSegment = bodyRawSegment;
        this.initialRawWindow = initialRawWindow;
        this.ratioMessage = Objects.requireNonNull(ratioMessage);
        this.raw = new byte[Math.max(firstRawSegment, bodyRawSegment)];
        decoder = newDecoder(encoding);
    }

    private static H2BoundedDecompressionHandler newDecoder(String encoding) {
        if (HttpConstants.GZIP.equalsIgnoreCase(encoding) || HttpConstants.X_GZIP.equalsIgnoreCase(encoding)) {
            return new H2BoundedGzipDecoder();
        } else if (HttpConstants.DEFLATE.equalsIgnoreCase(encoding)) {
            return new H2BoundedDeflateDecoder();
        } else {
            throw new IllegalArgumentException("Unsupported sealed HTTP/2 content encoding");
        }
    }

    long rawBytesRead() {
        return rawPosition;
    }

    long validatedDecodedLength() {
        if (!validated) {
            throw new IllegalStateException("Decoded HTTP/2 body was not validated");
        }
        return validatedDecodedLength;
    }

    boolean hasValidatedDecodedLength() {
        return validated;
    }

    boolean hasDeferredBodyFailure() {
        return deferredBodyFailure != null;
    }

    private int nextRawSegmentLength(long position) {
        return position < initialRawWindow
                        ? (int) Math.min((long) firstRawSegment, initialRawWindow - position)
                        : bodyRawSegment;
    }

    static long checkedDecodedLength(long before, int produced) throws IOException {
        try {
            return Math.addExact(before, produced);
        } catch (ArithmeticException overflow) {
            throw new IOException("Decoded HTTP/2 body count overflow", overflow);
        }
    }

    int read(byte[] output, int offset, int length) throws IOException {
        readLock.lock();
        try {
            return readLocked(output, offset, length);
        } finally {
            readLock.unlock();
            if (closeRequested.get()) {
                retireAfterRead();
            }
        }
    }

    private int readLocked(byte[] output, int offset, int length) throws IOException {
        if (closed || closeRequested.get()) {
            throw new IOException("Decoded HTTP/2 body reader is closed");
        }
        if (decodedEnded) {
            return -1;
        }
        if (output == null) {
            throw new NullPointerException("output");
        }
        if (offset < 0 || length < 0 || offset > output.length - length) {
            throw new IndexOutOfBoundsException();
        }
        if (length == 0) {
            return 0;
        }
        if (!preflightComplete) {
            validateBeforeDelivery();
        }
        while (true) {
            if (closeRequested.get()) {
                throw new IOException("Decoded HTTP/2 body reader is closed");
            }
            if (deferredBodyFailure != null && decodedBytes == committedDecodedBytes) {
                throw deferredBodyFailure;
            }
            int pullLength = Math.min(length, OUTPUT_CHUNK);
            if (deferredBodyFailure != null) {
                pullLength = (int) Math.min((long) pullLength, committedDecodedBytes - decodedBytes);
            }
            int produced;
            try {
                produced = decoder.pull(output, offset, pullLength);
            } catch (DataFormatException badBody) {
                if (legacyTruncatedEof(badBody, rawEnded)) {
                    produced = -1;
                } else {
                    IllegalHttpBodyException failure = new IllegalHttpBodyException(badBody.getMessage());
                    if (!preserveSingleReadTransactions) {
                        failure.initCause(badBody);
                    }
                    throw failure;
                }
            }
            if (produced > 0) {
                decodedBytes = checkedDecodedLength(decodedBytes, produced);
                segmentProduced = true;
                return produced;
            }
            // One offered raw file segment is the bounded decoder-call unit. A
            // single offer may produce many 16 KiB pulls; check its final ratio
            // once, after the handler has consumed that offer's decoded output.
            if (!preserveSingleReadTransactions && (produced < 0 || decoder.needsInput())) {
                checkSegmentRatio();
            }
            if (produced < 0) {
                if (deferredBodyFailure != null) {
                    throw new IOException("Decoded HTTP/2 body ended before its committed prefix");
                }
                if (preserveSingleReadTransactions && decodedBytes != validatedDecodedLength) {
                    throw new IOException("Decoded HTTP/2 body changed after validation");
                }
                decodedEnded = true;
                return -1;
            }
            if (!decoder.needsInput()) {
                throw new IOException("Decoded HTTP/2 body made no progress");
            }
            if (rawPosition < body.length()) {
                int wanted = (int) Math.min((long) nextRawSegmentLength(rawPosition), body.length() - rawPosition);
                rawPosition += offerRawSegment(rawPosition, wanted, decoder);
            } else if (!rawEnded) {
                rawEnded = true;
                decoder.endInput();
            } else {
                throw new IOException("Decoded HTTP/2 body made no progress at raw EOF");
            }
        }
    }

    /** Validate one event per completed raw segment without retaining decoded output. */
    private void validateBeforeDelivery() throws IOException {
        byte[] output = new byte[OUTPUT_CHUNK];
        H2BoundedDecompressionHandler probe = newDecoder(encoding);
        long position = 0;
        long decoded = 0;
        long committed = 0;
        long cycles = 0;
        boolean producedInSegment = false;
        boolean inputEnded = false;
        try {
            while (true) {
                if (closeRequested.get()) {
                    throw new IOException("Decoded HTTP/2 body reader is closed");
                }
                int produced;
                try {
                    produced = probe.pull(output, 0, output.length);
                } catch (DataFormatException badBody) {
                    if (legacyTruncatedEof(badBody, inputEnded)) {
                        produced = -1;
                    } else {
                        IllegalHttpBodyException failure = new IllegalHttpBodyException(badBody.getMessage());
                        if (!preserveSingleReadTransactions) {
                            failure.initCause(badBody);
                        }
                        throw failure;
                    }
                }
                if (produced > 0) {
                    decoded = checkedDecodedLength(decoded, produced);
                    producedInSegment = true;
                    continue;
                }
                if (producedInSegment && (produced < 0 || probe.needsInput())) {
                    producedInSegment = false;
                    long compressed = probe.getBytesRead();
                    if (compressed > 0 && decoded / compressed > ratioLimit) {
                        try {
                            cycles = Math.addExact(cycles, 1L);
                        } catch (ArithmeticException overflow) {
                            throw new IOException("Decoded HTTP/2 ratio cycle count overflow", overflow);
                        }
                        if (cycles > ratioTolerance) {
                            if (preserveSingleReadTransactions) {
                                throw new IllegalHttpBodyException(ratioMessage);
                            }
                            throw new IllegalHttpBodyException("Decompression tolerance reached");
                        }
                    }
                    // Only a completed outer raw-segment decode can have
                    // returned its bytes through the eager one-shot path.
                    committed = decoded;
                }
                if (produced < 0) {
                    validatedDecodedLength = decoded;
                    validated = true;
                    preflightComplete = true;
                    return;
                }
                if (!probe.needsInput()) {
                    throw new IOException("Decoded HTTP/2 body made no progress");
                }
                if (position < body.length()) {
                    int wanted = (int) Math.min((long) nextRawSegmentLength(position), body.length() - position);
                    position += offerRawSegment(position, wanted, probe);
                } else if (!inputEnded) {
                    inputEnded = true;
                    probe.endInput();
                } else {
                    throw new IOException("Decoded HTTP/2 body made no progress at raw EOF");
                }
            }
        } catch (IllegalHttpBodyException dataFailure) {
            if (!preserveSingleReadTransactions) {
                throw dataFailure;
            }
            committedDecodedBytes = committed;
            deferredBodyFailure = dataFailure;
            preflightComplete = true;
        } finally {
            probe.close();
        }
    }

    private int offerRawSegment(long position, int wanted, H2BoundedDecompressionHandler target) throws IOException {
        if (preserveSingleReadTransactions) {
            if (closeRequested.get()) {
                throw new IOException("Decoded HTTP/2 body reader is closed");
            }
            // Mirror the eager one-shot stream's single positional file read.
            // A short positive return is one outer decompressor transaction.
            int count = body.readAt(position, raw, 0, wanted);
            if (count <= 0) {
                throw new IOException("Sealed HTTP/2 request body ended before its recorded length");
            }
            target.offer(raw, count);
            return count;
        }
        readRawSegment(position, wanted);
        target.offer(raw, wanted);
        return wanted;
    }

    private boolean legacyTruncatedEof(DataFormatException failure, boolean inputEnded) {
        // The existing one-shot path has no final decoder completeness check.
        // Its bounded replacement must retain that EOF result only after raw EOF.
        if (!preserveSingleReadTransactions || !inputEnded) {
            return false;
        }
        return (HttpConstants.GZIP.equalsIgnoreCase(encoding) || HttpConstants.X_GZIP.equalsIgnoreCase(encoding))
                        ? "Truncated gzip body".equals(failure.getMessage())
                        : HttpConstants.DEFLATE.equalsIgnoreCase(encoding)
                                        && "Truncated deflate body".equals(failure.getMessage());
    }

    private void readRawSegment(long position, int wanted) throws IOException {
        int filled = 0;
        while (filled < wanted) {
            if (closeRequested.get()) {
                throw new IOException("Decoded HTTP/2 body reader is closed");
            }
            int count = body.readAt(position + filled, raw, filled, wanted - filled);
            if (count <= 0) {
                throw new IOException("Sealed HTTP/2 request body ended before its recorded length");
            }
            filled += count;
        }
    }

    private void checkSegmentRatio() throws IOException {
        if (!segmentProduced) {
            return;
        }
        segmentProduced = false;
        long compressed = decoder.getBytesRead();
        if (compressed > 0 && decodedBytes / compressed > ratioLimit) {
            try {
                cyclesAboveRatio = Math.addExact(cyclesAboveRatio, 1L);
            } catch (ArithmeticException overflow) {
                throw new IOException("Decoded HTTP/2 ratio cycle count overflow", overflow);
            }
            if (cyclesAboveRatio > ratioTolerance) {
                throw new IllegalHttpBodyException("Decompression tolerance reached");
            }
        }
    }

    @Override
    public void close() {
        closeRequested.set(true);
        retireAfterRead();
    }

    private void retireAfterRead() {
        if (readLock.tryLock()) {
            try {
                if (!closed) {
                    decoder.close();
                    closed = true;
                    if (onRetired != null) {
                        onRetired.run();
                    }
                }
            } finally {
                readLock.unlock();
            }
        }
    }
}
