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
package com.ibm.ws.wsoc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import javax.websocket.SendHandler;
import javax.websocket.SendResult;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.wsoc.MessageWriter.WRITE_TYPE;
import com.ibm.ws.wsoc.SendFuture.FUTURE_STATUS;
import com.ibm.ws.wsoc.WsocConnLink.RETURN_STATUS;
import com.ibm.ws.wsoc.injection.InjectionThings;
import com.ibm.ws.wsoc.util.Utils;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;

public class RemoteEndpointAsyncImpl {

    WsByteBufferPoolManager wsbbManager = null;

    WsocConnLink connLink = null;

    long sendTimeoutMillis = 0;
    int intSendTimeoutMillis = 0;

    public RemoteEndpointAsyncImpl() {

    }

    public void initialize(WsocConnLink _connLink) {
        connLink = _connLink;
        long timeout = connLink.getDefaultAsyncSendTimeout();
        setSendTimeout(timeout);
    }

    private WsByteBufferPoolManager getBufferManager() {
        return ServiceManager.getBufferPoolManager();
    }

    public long getSendTimeout() {
        return sendTimeoutMillis;
    }

    public void setSendTimeout(long timeoutMillis) {
        sendTimeoutMillis = timeoutMillis;
        this.intSendTimeoutMillis = Utils.longToInt(sendTimeoutMillis);
    }

    public void sendBinary(@Sensitive ByteBuffer data, SendHandler handler) {

        WsByteBuffer wsbb = getBufferManager().wrap(data);

        RETURN_STATUS ret = connLink.writeBuffer(wsbb, OpcodeType.BINARY_WHOLE, WRITE_TYPE.ASYNC, handler, intSendTimeoutMillis, false, false);

        if (ret != RETURN_STATUS.OK) {
            sendHandlerError(handler);
        }
    }

    public SendFuture sendBinary(@Sensitive ByteBuffer data) {

        WsByteBuffer wsbb = getBufferManager().wrap(data);

        SendFuture fut = new SendFuture();
        fut.initialize(connLink, FUTURE_STATUS.STARTED);
        SendHandlerForFuture handler = new SendHandlerForFuture();
        handler.initialize(fut);

        RETURN_STATUS ret = connLink.writeBuffer(wsbb, OpcodeType.BINARY_WHOLE, WRITE_TYPE.ASYNC, handler, intSendTimeoutMillis, false, false);
        if (ret != RETURN_STATUS.OK) {
            setFutureStatus(fut);
        }
        return fut;
    }

    public void sendObject(@Sensitive Object data, SendHandler handler) {

        RETURN_STATUS ret = connLink.writeObject(data, WRITE_TYPE.ASYNC, handler);
        if (ret != RETURN_STATUS.OK) {
            sendHandlerError(handler);
        }
    }

    public SendFuture sendObject(@Sensitive Object data) {

        SendFuture fut = new SendFuture();
        fut.initialize(connLink, FUTURE_STATUS.STARTED);
        SendHandlerForFuture handler = new SendHandlerForFuture();
        handler.initialize(fut);

        RETURN_STATUS ret = connLink.writeObject(data, WRITE_TYPE.ASYNC, handler);
        if (ret != RETURN_STATUS.OK) {
            setFutureStatus(fut);
        }
        return fut;
    }

    public void sendText(@Sensitive String textToSend, SendHandler handler) {

        // convert String to a byte buffer
        byte[] ba = textToSend.getBytes(Utils.UTF8_CHARSET);
        // get pooled buffer, don't wrap. This cuts down on a lot of garbage needing to be collected, especially during stress.  
        WsByteBuffer wsbb = getBufferManager().allocate(ba.length);
        wsbb.put(ba);
        wsbb.position(0);
        wsbb.limit(ba.length);

        RETURN_STATUS ret = connLink.writeBuffer(wsbb, OpcodeType.TEXT_WHOLE, WRITE_TYPE.ASYNC, handler, intSendTimeoutMillis, false, false);
        if (ret != RETURN_STATUS.OK) {
            sendHandlerError(handler);
        }
    }

    public SendFuture sendText(@Sensitive String textToSend) {

        // convert String to a byte buffer
        byte[] ba = textToSend.getBytes(Utils.UTF8_CHARSET);

        SendFuture fut = new SendFuture();
        fut.initialize(connLink, FUTURE_STATUS.STARTED);
        SendHandlerForFuture handler = new SendHandlerForFuture();
        handler.initialize(fut);

        // get pooled buffer, don't wrap. This cuts down on a lot of garbage needing to be collected, especially during stress.  
        WsByteBuffer wsbb = getBufferManager().allocate(ba.length);
        wsbb.put(ba);
        wsbb.position(0);
        wsbb.limit(ba.length);

        RETURN_STATUS ret = connLink.writeBuffer(wsbb, OpcodeType.TEXT_WHOLE, WRITE_TYPE.ASYNC, handler, intSendTimeoutMillis, false, false);
        if (ret != RETURN_STATUS.OK) {
            setFutureStatus(fut);
        }
        return fut;
    }

    private void setFutureStatus(SendFuture fut) {
        IllegalStateException ise = new IllegalStateException("write not allowed.  Most likely cause is that another Write or Close is in progress");
        ExecutionException ex = new ExecutionException(ise);
        fut.setStatus(FUTURE_STATUS.DONE, ex);
    }

    private void sendHandlerError(SendHandler handler) {
        ExecutorService es = ServiceManager.getExecutorThreadService();
        if (es != null) {
            IOException ioe = new IOException("write not allowed.  Most likely cause is that another Write or Close is in progress");
            SendResult sr = new SendResult(ioe);
            HandlerCallback hc = new HandlerCallback(handler, sr, connLink);
            es.execute(hc);
        }
    }

    private class HandlerCallback implements Runnable {
        protected SendHandler handler;
        protected SendResult result;
        protected WsocConnLink connLink = null;
        protected InjectionThings it = null;;

        protected HandlerCallback(SendHandler handlerInput, SendResult resultInput, WsocConnLink inConnLink) {
            handler = handlerInput;
            result = resultInput;
            connLink = inConnLink;
        }

        @Override
        public void run() {
            it = connLink.pushContexts();
            try {
                handler.onResult(result);
            } finally {
                connLink.popContexts(it);
            }
        }

    }

}
