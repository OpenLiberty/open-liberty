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
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.zip.DataFormatException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.inputstream.HttpInputStreamObserver;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.h2spool.H2SealedBody;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.http.channel.HttpConstants;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.exception.IllegalHttpBodyException;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

import io.netty.channel.ChannelHandlerContext;
import io.openliberty.http.netty.compression.HttpContentDecompressor;

/** File, decoder, and read lifetime for a completed HTTP/2 request body. */
final class H2SealedBodyStream {
    private static final TraceComponent tc = Tr.register(H2SealedBodyStream.class,
                    HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);
    private static final int SEALED_READ_CHUNK = 64 * 1024;

    private final HttpInputStreamImpl input;
    private final Supplier<H2SealedDecodedReader> decoderFactory;
    private final Object streamingReadLock = new Object();
    private final Object decoderLock = new Object();
    private final Object readResourcesLock = new Object();
    private final AtomicBoolean closeStarted = new AtomicBoolean();
    private final AtomicBoolean closeObserverInstalled = new AtomicBoolean();
    private final AtomicBoolean closeReported = new AtomicBoolean();
    private H2SealedBody body;
    private InputStream fileInput;
    private H2SealedDecodedReader decoder;
    private H2SealedBody.ReadLifetime readLifetime;
    private int readUsers;
    private int liveDecoders;
    private boolean passClosePending;
    private byte[] chunk;
    private long remaining;
    private boolean decodedReplay;
    private boolean decodedComplete;
    private boolean decodedMetadataPublished;
    private boolean incompleteMetadataPublished;
    private boolean multiRead;
    private int ratioLimit;
    private int ratioTolerance;
    private int headerSegmentSize;
    private int bodySegmentSize;
    private String contentEncoding;
    private HttpContentDecompressor decompressor;
    private long decodedBytesProduced;
    private IOException error;
    private Runnable bodyClosed;
    private HttpInputStreamObserver observer;

    H2SealedBodyStream(HttpInputStreamImpl input, Supplier<H2SealedDecodedReader> decoderFactory) {
        this.input = input;
        this.decoderFactory = decoderFactory;
    }

    void configure(H2SealedBody body, ChannelHandlerContext context, String contentEncoding, Runnable onClose) {
        this.body = Objects.requireNonNull(body);
        long length = body.length();
        if (length < 0) {
            throw new IllegalArgumentException("Sealed HTTP/2 body length must not be negative");
        }
        this.remaining = length;
        this.bodyClosed = onClose;
        input.context = context;
        input.autoRead = context != null && context.channel().config().isAutoRead();
        this.contentEncoding = contentEncoding == null ? null : contentEncoding.toLowerCase(Locale.ROOT);
        this.decompressor = new HttpContentDecompressor();
        HttpChannelConfig config = ((HttpInboundServiceContextImpl) input.isc).getHttpConfig();
        decodedReplay = isCompressed(this.contentEncoding) && config.isAutoDecompressionEnabled();
        ratioLimit = config.getDecompressionRatioLimit();
        ratioTolerance = config.getDecompressionTolerance();
        headerSegmentSize = config.getIncomingHdrBufferSize();
        bodySegmentSize = config.getIncomingBodyBufferSize();
        input.bytesRead = 0L;
        decodedBytesProduced = 0L;
        input.readChannelComplete = length == 0;
        input.queue = null;
        input.streaming = true;
    }

    private static boolean isCompressed(String encoding) {
        return HttpConstants.GZIP.equalsIgnoreCase(encoding) || HttpConstants.X_GZIP.equalsIgnoreCase(encoding)
                        || HttpConstants.DEFLATE.equalsIgnoreCase(encoding);
    }

    private boolean isSealedDecodedBody() {
        // Empty one-shot compressed bodies historically reached raw EOF without a decoder.
        return decodedReplay && (multiRead || body.length() != 0);
    }

    void validate() throws IOException {
        if (error != null) {
            throw error;
        }
    }

    void setObserver(HttpInputStreamObserver observer) {
        this.observer = observer;
    }

    void setReplayEnabled(boolean enabled) {
        multiRead = enabled;
        if (enabled) {
            input.readChannelComplete = false;
        }
    }

    int available() throws IOException {
        input.validate();
        boolean use = beginSealedReadUse();
        try {
            int buffered = input.buffer == null ? 0 : input.buffer.remaining();
            if (multiRead && !isSealedDecodedBody()) {
                return remaining >= Integer.MAX_VALUE - buffered ? Integer.MAX_VALUE : buffered + (int) remaining;
            }
            return buffered;
        } finally {
            endSealedReadUse(use);
        }
    }

    void close() throws IOException {
        if (input.isClosed()) {
            if (!multiRead && error != null) {
                throw error;
            }
            return;
        }
        if (isSealedDecodedBody()) {
            synchronized (readResourcesLock) {
                passClosePending = true;
            }
        } else if (input.buffer != null) {
            input.buffer.release();
            input.buffer = null;
        }
        input.closed = true;
        if (multiRead) {
            closeSealedDecoder();
            retireSealedReadResources();
            if (observer != null) {
                observer.alertISClose();
            }
        } else {
            closeSealedBodyAsync();
        }
        if (!multiRead && error != null) {
            throw error;
        }
    }

    boolean restart() {
        if (multiRead) {
            synchronized (streamingReadLock) {
                if (closeStarted.get()) {
                    return false;
                }
                closeSealedDecoder();
                decodedComplete = false;
                remaining = body.length();
                input.readChannelComplete = remaining == 0;
                input.bytesRead = 0L;
                decodedBytesProduced = 0L;
            }
        }
        return true;
    }

    void cleanupforMultiRead() {
        if (multiRead || isSealedDecodedBody()) {
            closeSealedDecoder();
            closeSealedBodyAsync();
        }
        input.bytesRead = 0L;
        WsByteBuffer currentBuffer = input.buffer;
        if (isSealedDecodedBody()) {
            synchronized (readResourcesLock) {
                passClosePending = true;
            }
            retireSealedReadResources();
        } else if (currentBuffer != null) {
            currentBuffer.release();
            input.buffer = null;
        }
    }

    CompletionStage<Void> closeSealedBodyAsync() {
        synchronized (readResourcesLock) {
            closeStarted.set(true);
        }
        closeSealedDecoder();
        retireSealedReadResources();
        CompletionStage<Void> closed = body.closeAsync();
        if (closeObserverInstalled.compareAndSet(false, true)) {
            closed.whenComplete((unused, failure) -> {
                if (failure == null) {
                    if (bodyClosed != null && closeReported.compareAndSet(false, true)) {
                        bodyClosed.run();
                    }
                } else {
                    closeObserverInstalled.set(false);
                }
            });
        }
        return closed;
    }

    boolean fill() throws IOException {
        boolean use = beginSealedReadUse();
        try {
            synchronized (streamingReadLock) {
                return fillFromSealedBodyLocked();
            }
        } finally {
            endSealedReadUse(use);
        }
    }

    boolean endReadyForCallback() {
        synchronized (streamingReadLock) {
            return endReadyForCallbackLocked();
        }
    }

    private boolean endReadyForCallbackLocked() {
        return (isSealedDecodedBody() ? decodedComplete : remaining == 0)
                        && (input.buffer == null || !input.buffer.hasRemaining());
    }

    boolean readReadyForCallback() throws IOException {
        synchronized (streamingReadLock) {
            if (closeStarted.get() && !endReadyForCallbackLocked()) {
                throw new IOException("Sealed HTTP/2 request body was closed before it was read");
            }
            return true;
        }
    }

    boolean beginSealedReadUse() throws IOException {
        if (!isSealedDecodedBody()) {
            return false;
        }
        synchronized (readResourcesLock) {
            if (closeStarted.get()) {
                // Successful one-shot EOF retired the reader and body already.
                // A later EOF/available query needs no new read lifetime.
                if (!multiRead && decodedComplete) {
                    return false;
                }
                throw new IOException("Sealed HTTP/2 request body was closed before it was read");
            }
            if (readLifetime == null) {
                readLifetime = body.beginReadLifetime();
            }
            readUsers++;
        }
        return true;
    }

    void endSealedReadUse(boolean used) {
        if (used) {
            synchronized (readResourcesLock) {
                readUsers--;
            }
            retireSealedReadResources();
        }
    }

    private void onSealedDecoderRetired() {
        synchronized (readResourcesLock) {
            liveDecoders--;
        }
        // A concurrent read's outer finally or closeSealedBodyAsync performs
        // final staging/output release after the native decoder has retired.
    }

    private void retireSealedReadResources() {
        H2SealedBody.ReadLifetime lifetime = null;
        synchronized (readResourcesLock) {
            if (readLifetime == null && readUsers == 0 && liveDecoders == 0 && !passClosePending) {
                return;
            }
            if (readUsers != 0 || liveDecoders != 0) {
                return;
            }
            WsByteBuffer currentBuffer = input.buffer;
            if (passClosePending || closeStarted.get()) {
                if (currentBuffer != null) {
                    currentBuffer.release();
                    input.buffer = null;
                }
                passClosePending = false;
            }
            if (closeStarted.get() && readLifetime != null) {
                chunk = null;
                lifetime = readLifetime;
                readLifetime = null;
            }
        }
        if (lifetime != null) {
            lifetime.close();
        }
    }

    private boolean fillFromSealedBodyLocked() throws IOException {
        boolean replay = multiRead;
        if (input.buffer != null && input.buffer.hasRemaining()) {
            return true;
        }
        if (input.buffer != null) {
            input.buffer.release();
            input.buffer = null;
        }
        if (isSealedDecodedBody()) {
            return fillFromSealedDecodedBodyLocked();
        }
        if (remaining == 0) {
            input.readChannelComplete = true;
            if (!replay) {
                closeSealedBodyAsync();
            }
            return false;
        }
        if (closeStarted.get()) {
            throw new IOException("Sealed HTTP/2 request body was closed before it was read");
        }
        if (input.context != null && input.context.executor().inEventLoop()) {
            throw new IllegalStateException("File-backed request read on event loop group thread");
        }
        if (!replay && fileInput == null) {
            fileInput = body.openInputStream();
        }
        if (chunk == null) {
            chunk = new byte[SEALED_READ_CHUNK];
        }
        while (remaining > 0) {
            int wanted = (int) Math.min(chunk.length, remaining);
            int len = replay ? body.readAt(body.length() - remaining, chunk, 0, wanted)
                            : fileInput.read(chunk, 0, wanted);
            if (len <= 0) {
                throw new IOException("Sealed HTTP/2 request body ended before its recorded length");
            }
            if (closeStarted.get()) {
                // Close may start while the file read is in progress.
                throw new IOException("Sealed HTTP/2 request body was closed before it was read");
            }
            remaining -= len;
            input.bytesRead = body.length() - remaining;
            if (remaining == 0 && !replay) {
                // The wrapper returns EOF without reading the sealed stream again.
                // Release the file after its final raw chunk has been copied.
                closeSealedBodyAsync();
            }

            WsByteBuffer source = copySealedChunk(len);
            WsByteBuffer out = source;
            if (!replay && contentEncoding != null && isCompressed(contentEncoding)) {
                HttpChannelConfig config = ((HttpInboundServiceContextImpl) input.isc).getHttpConfig();
                try {
                    out = decompressor.decompress(source, config, contentEncoding);
                } catch (DataFormatException dfe) {
                    IllegalHttpBodyException exception = new IllegalHttpBodyException(dfe.getMessage());
                    exception.initCause(dfe);
                    throw exception;
                }
            }
            if (out != null && out.hasRemaining()) {
                input.buffer = out;
                decodedBytesProduced += out.remaining();
                return true;
            }
            if (out != null) {
                out.release();
            }
        }
        input.readChannelComplete = true;
        return false;
    }

    private void closeSealedDecoder() {
        H2SealedDecodedReader reader;
        synchronized (decoderLock) {
            reader = decoder;
            decoder = null;
        }
        if (reader != null) {
            reader.close(); // nonblocking when a worker is inside inflation or file read
        }
    }

    private H2SealedDecodedReader sealedDecoderForRead() throws IOException {
        H2SealedDecodedReader reader;
        synchronized (decoderLock) {
            if (closeStarted.get()) {
                throw new IOException("Sealed HTTP/2 request body was closed before it was read");
            }
            reader = decoder;
        }
        if (reader == null) {
            synchronized (readResourcesLock) {
                liveDecoders++;
            }
            H2SealedDecodedReader created;
            try {
                created = decoderFactory.get();
            } catch (RuntimeException | Error failure) {
                onSealedDecoderRetired();
                throw failure;
            }
            synchronized (decoderLock) {
                if (!closeStarted.get()) {
                    decoder = created;
                    reader = created;
                }
            }
            if (reader == null) {
                created.close();
                throw new IOException("Sealed HTTP/2 request body was closed before it was read");
            }
        }
        if (closeStarted.get()) {
            closeSealedDecoder();
            throw new IOException("Sealed HTTP/2 request body was closed before it was read");
        }
        return reader;
    }

    H2SealedDecodedReader createSealedDecoder() {
        if (multiRead) {
            return new H2SealedDecodedReader(body, contentEncoding,
                            ratioLimit, ratioTolerance, this::onSealedDecoderRetired, false);
        }
        return new H2SealedDecodedReader(body, contentEncoding,
                        ratioLimit, ratioTolerance, this::onSealedDecoderRetired, true,
                        headerSegmentSize, bodySegmentSize, headerSegmentSize,
                        Tr.formatMessage(tc, "decompression.tolerance.reached"));
    }

    private WsByteBuffer copySealedChunk(int count) {
        // The pool may round a small output to a much larger custom entry.
        // An exact backing array keeps the sealed read within its memory limit.
        return ChannelFrameworkFactory.getBufferManager().wrap(Arrays.copyOf(chunk, count));
    }

    private boolean fillFromSealedDecodedBodyLocked() throws IOException {
        if (decodedComplete) {
            return false;
        }
        if (closeStarted.get()) {
            throw new IOException("Sealed HTTP/2 request body was closed before it was read");
        }
        if (input.context != null && input.context.executor().inEventLoop()) {
            throw new IllegalStateException("File-backed request read on event loop group thread");
        }
        H2SealedDecodedReader reader = sealedDecoderForRead();
        if (chunk == null) {
            chunk = new byte[H2SealedDecodedReader.OUTPUT_CHUNK];
        }
        try {
            int count = reader.read(chunk, 0, H2SealedDecodedReader.OUTPUT_CHUNK);
            input.bytesRead = reader.rawBytesRead();
            remaining = body.length() - input.bytesRead;
            if (count < 0) {
                if (closeStarted.get()) {
                    throw new IOException("Sealed HTTP/2 request body was closed before it was read");
                }
                publishSealedDecodedMetadata(reader.validatedDecodedLength());
                decodedComplete = true;
                input.readChannelComplete = true;
                closeSealedDecoder();
                if (!multiRead) {
                    closeSealedBodyAsync();
                }
                return false;
            }
            if (count == 0) {
                throw new IOException("Decoded HTTP/2 body made no progress");
            }
            long producedTotal = H2SealedDecodedReader.checkedDecodedLength(decodedBytesProduced, count);
            WsByteBuffer producedBuffer = copySealedChunk(count);
            boolean installed = false;
            try {
                if (closeStarted.get()) {
                    throw new IOException("Sealed HTTP/2 request body was closed before it was read");
                }
                if (reader.hasValidatedDecodedLength()) {
                    publishSealedDecodedMetadata(reader.validatedDecodedLength());
                } else if (reader.hasDeferredBodyFailure() && !multiRead) {
                    publishSealedIncompleteMetadata();
                }
                input.buffer = producedBuffer;
                decodedBytesProduced = producedTotal;
                installed = true;
                return true;
            } finally {
                if (!installed) {
                    producedBuffer.release();
                }
            }
        } catch (IOException | ArithmeticException e) {
            IOException failure = e instanceof IOException ? (IOException) e
                            : new IOException("Decoded HTTP/2 body count overflow", e);
            error = failure;
            if (incompleteMetadataPublished) {
                HttpRequestMessage request = ((HttpInboundServiceContextImpl) input.isc).getRequest();
                request.setContentLength(body.length());
                request.setHeader(HttpHeaderKeys.HDR_TRANSFER_ENCODING, "chunked");
            }
            closeSealedDecoder();
            if (!multiRead) {
                closeSealedBodyAsync();
            }
            throw failure;
        } catch (RuntimeException | Error e) {
            closeSealedDecoder();
            closeSealedBodyAsync();
            throw e;
        }
    }

    private void publishSealedDecodedMetadata(long decodedLength) throws IOException {
        synchronized (readResourcesLock) {
            if (closeStarted.get()) {
                throw new IOException("Sealed HTTP/2 request body was closed before it was read");
            }
            if (decodedMetadataPublished) {
                return;
            }
            HttpRequestMessage request = ((HttpInboundServiceContextImpl) input.isc).getRequest();
            if (!request.containsHeader(HttpHeaderKeys.HDR_$WSZIP)) {
                // Match the completed-body legacy transition at the first read,
                // before any decoded byte is returned to the application.
                request.removeHeader(HttpHeaderKeys.HDR_CONTENT_ENCODING);
                request.setHeader(HttpHeaderKeys.HDR_$WSZIP, contentEncoding);
                request.setHeader(HttpHeaderKeys.HDR_$WSORIGCL, Long.toString(body.length()));
                request.setContentLength(decodedLength); // updates Netty's cached numeric getter too
            }
            decodedMetadataPublished = true;
        }
    }

    private void publishSealedIncompleteMetadata() throws IOException {
        synchronized (readResourcesLock) {
            if (closeStarted.get()) {
                throw new IOException("Sealed HTTP/2 request body was closed before it was read");
            }
            if (incompleteMetadataPublished) {
                return;
            }
            HttpRequestMessage request = ((HttpInboundServiceContextImpl) input.isc).getRequest();
            if (!request.containsHeader(HttpHeaderKeys.HDR_$WSZIP)) {
                request.removeHeader(HttpHeaderKeys.HDR_CONTENT_ENCODING);
                request.setHeader(HttpHeaderKeys.HDR_$WSZIP, contentEncoding);
                request.setHeader(HttpHeaderKeys.HDR_$WSORIGCL, Long.toString(body.length()));
                request.removeHeader(HttpHeaderKeys.HDR_CONTENT_LENGTH);
                request.setHeader(HttpHeaderKeys.HDR_TRANSFER_ENCODING, "chunked");
            }
            incompleteMetadataPublished = true;
        }
    }
}
