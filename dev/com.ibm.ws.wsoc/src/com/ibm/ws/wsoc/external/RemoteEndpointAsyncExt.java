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
import java.nio.ByteBuffer;
import java.util.concurrent.Future;

import javax.websocket.RemoteEndpoint;
import javax.websocket.SendHandler;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.wsoc.Constants;
import com.ibm.ws.wsoc.OpcodeType;
import com.ibm.ws.wsoc.RemoteEndpointAsyncImpl;
import com.ibm.ws.wsoc.RemoteEndpointBasicImpl;

public class RemoteEndpointAsyncExt implements RemoteEndpoint.Async {

    private RemoteEndpointAsyncImpl asyncImpl = null;
    private RemoteEndpointBasicImpl basicImpl = null;

    public RemoteEndpointAsyncExt(RemoteEndpointAsyncImpl asyncImpl, RemoteEndpointBasicImpl basicImpl) {
        this.basicImpl = basicImpl;
        this.asyncImpl = asyncImpl;

    }

    @Override
    public long getSendTimeout() {
        return asyncImpl.getSendTimeout();
    }

    @Override
    public Future<Void> sendBinary(@Sensitive ByteBuffer data) {

        if (data == null) {
            IllegalArgumentException up = new IllegalArgumentException();
            throw up;
        }

        Future<Void> future = asyncImpl.sendBinary(data);
        return future;
    }

    @Override
    public void sendBinary(@Sensitive ByteBuffer data, SendHandler handler) {
        if ((data == null) || (handler == null)) {
            IllegalArgumentException up = new IllegalArgumentException();
            throw up;
        }

        asyncImpl.sendBinary(data, handler);

    }

    @Override
    public Future<Void> sendObject(@Sensitive Object data) {
        if (data == null) {
            IllegalArgumentException up = new IllegalArgumentException();
            throw up;
        }

        Future<Void> future = asyncImpl.sendObject(data);
        return future;
    }

    @Override
    public void sendObject(@Sensitive Object data, SendHandler handler) {
        if ((data == null) || (handler == null)) {
            IllegalArgumentException up = new IllegalArgumentException();
            throw up;
        }

        asyncImpl.sendObject(data, handler);

    }

    @Override
    public Future<Void> sendText(@Sensitive String text) {

        if (text == null) {
            IllegalArgumentException up = new IllegalArgumentException();
            throw up;
        }

        Future<Void> future = asyncImpl.sendText(text);
        return future;
    }

    @Override
    public void sendText(@Sensitive String text, SendHandler handler) {
        if ((text == null) || (handler == null)) {
            IllegalArgumentException up = new IllegalArgumentException();
            throw up;
        }

        asyncImpl.sendText(text, handler);
        return;
    }

    @Override
    public void setSendTimeout(long timeoutmillis) {
        asyncImpl.setSendTimeout(timeoutmillis);
    }

    @Override
    public void sendPing(@Sensitive ByteBuffer data) {
        // TODO: need to add translated messages here
        if (data == null) {
            IllegalArgumentException up = new IllegalArgumentException("a null ByteBuffer was passed into the sendPing API");
            throw up;
        }

        if (data.remaining() > Constants.MAX_PING_SIZE) {
            IllegalArgumentException up = new IllegalArgumentException("data pass into the sendPing API was too large");
            throw up;
        }

        // pings are sent synchronously
        basicImpl.sendBinaryFromAsyncRemote(data, OpcodeType.PING);

    }

    @Override
    public void sendPong(@Sensitive ByteBuffer data) {
        if (data == null) {
            IllegalArgumentException up = new IllegalArgumentException();
            throw up;
        }

        // pongs are sent synchronously
        basicImpl.sendBinaryFromAsyncRemote(data, OpcodeType.PONG);

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.websocket.RemoteEndpoint#flushBatch()
     */
    @Override
    public void flushBatch() throws IOException {
        // Batch (which is optional per the spec) is not implemented

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.websocket.RemoteEndpoint#getBatchingAllowed()
     */
    @Override
    public boolean getBatchingAllowed() {
        // Batch (which is optional per the spec) is not implemented
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.websocket.RemoteEndpoint#setBatchingAllowed(boolean)
     */
    @Override
    public void setBatchingAllowed(boolean arg0) throws IOException {
        // Batch (which is optional per the spec) is not implemented

    }

}
