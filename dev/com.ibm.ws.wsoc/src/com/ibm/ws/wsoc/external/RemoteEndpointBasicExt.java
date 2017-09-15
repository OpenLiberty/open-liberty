/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc.external;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;

import javax.websocket.EncodeException;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.wsoc.Constants;
import com.ibm.ws.wsoc.OpcodeType;
import com.ibm.ws.wsoc.RemoteEndpointBasicImpl;

public class RemoteEndpointBasicExt implements javax.websocket.RemoteEndpoint.Basic {

    private RemoteEndpointBasicImpl impl = null;

    public RemoteEndpointBasicExt(RemoteEndpointBasicImpl _impl) {

        impl = _impl;

    }

    @Override
    public void flushBatch() throws IOException {
        return;
    }

    @Override
    public boolean getBatchingAllowed() {
        return false;
    }

    @Override
    public void setBatchingAllowed(boolean allowed) throws IOException {
        return;

    }

    @Override
    public OutputStream getSendStream() throws IOException {

        OutputStream os = impl.getSendStream();
        return os;
    }

    @Override
    public Writer getSendWriter() throws IOException {

        Writer w = impl.getSendWriter();
        return w;
    }

    @Override
    public void sendBinary(@Sensitive ByteBuffer data) throws IOException {

        if (data == null) {
            IllegalArgumentException up = new IllegalArgumentException();
            throw up;
        }

        impl.sendBinary(data, OpcodeType.BINARY_WHOLE);

    }

    @Override
    public void sendBinary(@Sensitive ByteBuffer partialByte, boolean isLast) throws IOException {
        if (partialByte == null) {
            IllegalArgumentException up = new IllegalArgumentException();
            throw up;
        }

        impl.sendBinary(partialByte, OpcodeType.BINARY_WHOLE, isLast);
        return;

    }

    @Override
    public void sendObject(@Sensitive Object data) throws IOException,
                    EncodeException {
        if (data == null) {
            IllegalArgumentException up = new IllegalArgumentException();
            throw up;
        }

        impl.sendObject(data);
    }

    @Override
    public void sendText(@Sensitive String text) throws IOException {

        if (text == null) {
            IllegalArgumentException up = new IllegalArgumentException();
            throw up;
        }

        impl.sendText(text);
        return;

    }

    @Override
    public void sendText(@Sensitive String partialMessage, boolean isLast) throws IOException {
        if (partialMessage == null) {
            IllegalArgumentException up = new IllegalArgumentException();
            throw up;
        }

        impl.sendText(partialMessage, isLast);
        return;

    }

    @Override
    public void sendPing(@Sensitive ByteBuffer data) throws IOException {
        // TODO: need to add translated messages here
        if (data == null) {
            IllegalArgumentException up = new IllegalArgumentException("a null ByteBuffer was passed into the sendPing API");
            throw up;
        }

        if (data.remaining() > Constants.MAX_PING_SIZE) {
            IllegalArgumentException up = new IllegalArgumentException("data pass into the sendPing API was too large");
            throw up;
        }

        impl.sendPing(data);

    }

    @Override
    public void sendPong(@Sensitive ByteBuffer data) throws IOException {
        if (data == null) {
            IllegalArgumentException up = new IllegalArgumentException();
            throw up;
        }

        impl.sendPong(data);

    }

}
