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
import java.util.concurrent.CompletionStage;

import com.ibm.ws.http.channel.inputstream.HttpInputStreamObserver;
import com.ibm.ws.http.channel.internal.h2spool.H2SealedBody;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;

import io.netty.channel.ChannelHandlerContext;

/** Application stream for an HTTP/2 request whose body has already been sealed. */
public class H2SealedInputStreamImpl extends HttpInputStreamImpl implements H2SealedBodyInput {
    private final H2SealedBodyStream sealed = new H2SealedBodyStream(this, this::createSealedDecoder);

    public H2SealedInputStreamImpl(HttpInboundServiceContext context) {
        super(context);
    }

    @Override
    public void nettyConfigureSealedBody(H2SealedBody body, ChannelHandlerContext context,
                                          String contentEncoding, Runnable onClose) {
        sealed.configure(body, context, contentEncoding, onClose);
    }

    @Override
    public CompletionStage<Void> closeSealedBodyAsync() {
        return sealed.closeSealedBodyAsync();
    }

    H2SealedDecodedReader createSealedDecoder() {
        return sealed.createSealedDecoder();
    }

    @Override
    protected void validate() throws IOException {
        super.validate();
        sealed.validate();
    }

    @Override
    public int available() throws IOException {
        return sealed.available();
    }

    @Override
    public int read() throws IOException {
        validate();
        boolean use = sealed.beginSealedReadUse();
        try {
            return super.read();
        } finally {
            sealed.endSealedReadUse(use);
        }
    }

    @Override
    public int read(byte[] output, int offset, int length) throws IOException {
        validate();
        if (length == 0) {
            return super.read(output, offset, length);
        }
        boolean use = sealed.beginSealedReadUse();
        try {
            return super.read(output, offset, length);
        } finally {
            sealed.endSealedReadUse(use);
        }
    }

    @Override
    public long skip(long target) throws IOException {
        validate();
        boolean use = sealed.beginSealedReadUse();
        try {
            return super.skip(target);
        } finally {
            sealed.endSealedReadUse(use);
        }
    }

    @Override
    public void close() throws IOException {
        sealed.close();
    }

    @Override
    public void restart() {
        if (sealed.restart()) {
            super.restart();
        }
    }

    @Override
    public void setISObserver(HttpInputStreamObserver observer) {
        super.setISObserver(observer);
        sealed.setObserver(observer);
    }

    @Override
    public void if_enableMultiReadofPostData_set(boolean enabled) {
        // This inherited API must not set the parent's in-memory replay flag.
        setupforMultiRead(enabled);
    }

    @Override
    public void setupforMultiRead(boolean enabled) {
        sealed.setReplayEnabled(enabled);
    }

    @Override
    public void cleanupforMultiRead() {
        sealed.cleanupforMultiRead();
    }

    @Override
    protected boolean isMultiReadOfPostDataEnabled() {
        return false; // Sealed replay reads the spool, not the parent's buffer list.
    }

    @Override
    public boolean fillFromStreamingNettyIfAvailable() throws IOException {
        return sealed.fill(); // The body is complete, so there is no network wait.
    }

    @Override
    public boolean fillFromStreamingNetty(boolean waitForInput) throws IOException {
        return sealed.fill();
    }

    @Override
    protected boolean isStreamingEndReadyForCallback() {
        return sealed.endReadyForCallback();
    }

    @Override
    protected boolean isStreamingReadReadyForCallback() throws IOException {
        return sealed.readReadyForCallback();
    }
}
