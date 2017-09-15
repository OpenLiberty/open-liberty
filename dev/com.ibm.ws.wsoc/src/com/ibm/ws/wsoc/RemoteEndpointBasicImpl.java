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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;

import javax.websocket.EncodeException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.wsoc.WsocConnLink.RETURN_STATUS;
import com.ibm.ws.wsoc.util.Utils;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.tcpchannel.TCPRequestContext;

public class RemoteEndpointBasicImpl {

    private static final TraceComponent tc = Tr.register(RemoteEndpointBasicImpl.class);

    public enum MessageWriteState {
        PARTIAL_NOT_IN_USE,
        PARTIAL_TEXT_IN_USE,
        PARTIAL_BINARY_IN_USE,
    }

    WsByteBufferPoolManager wsbbManager = null;
    WsocConnLink connLink = null;
    MessageWriteState messageWriteState = MessageWriteState.PARTIAL_NOT_IN_USE;

    public RemoteEndpointBasicImpl() {

    }

    public void initialize(WsocConnLink _connLink) {
        connLink = _connLink;
        messageWriteState = MessageWriteState.PARTIAL_NOT_IN_USE;
    }

    private WsByteBufferPoolManager getBufferManager() {
        return ServiceManager.getBufferPoolManager();
    }

    public void sendPing(@Sensitive ByteBuffer data) throws IOException {
        sendBinaryCommon(data, OpcodeType.PING, true);
    }

    public void sendPong(@Sensitive ByteBuffer data) throws IOException {
        sendBinaryCommon(data, OpcodeType.PONG, true);
    }

    //this is for sendPing(..) and sendPong(..) from RemoteEndpointAsync. Data is still sent
    //synchronously. 
    public void sendBinaryFromAsyncRemote(@Sensitive ByteBuffer data, OpcodeType type) {
        try {
            sendBinary(data, type);
        } catch (IOException e) {
            connLink.callOnError(e);
        }
    }

    public void sendBinary(@Sensitive ByteBuffer data, OpcodeType type) throws IOException {

        if (messageWriteState != MessageWriteState.PARTIAL_NOT_IN_USE) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Send while Send outstanding error.  throw IllegalStateException");
            }
            // another send is outstanding on this connection, as per spec behaivor, need to throw an illegalStateException
            IllegalStateException up = new IllegalStateException();
            throw up;
        }

        RETURN_STATUS ret = sendBinaryCommon(data, type, false);
        if (ret != RETURN_STATUS.OK) {
            // another write of an IO frame is outstanding on this connection
            IllegalStateException up = new IllegalStateException("write not allowed.  Most likely cause is that another Write or Close is in progress");
            throw up;
        }
    }

    public void sendBinary(@Sensitive ByteBuffer data, OpcodeType type, boolean isLast) throws IOException {

        OpcodeType t = type;

        if (messageWriteState == MessageWriteState.PARTIAL_TEXT_IN_USE) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Send while Send outstanding error.  throw IllegalStateException");
            }
            // another send is outstanding on this connection, as per spec behaivor, need to throw an illegalStateException
            IllegalStateException up = new IllegalStateException();
            throw up;
        }

        if (messageWriteState == MessageWriteState.PARTIAL_NOT_IN_USE) {
            messageWriteState = MessageWriteState.PARTIAL_BINARY_IN_USE;
            if (isLast) {
                t = OpcodeType.BINARY_WHOLE;
            } else {
                t = OpcodeType.BINARY_PARTIAL_FIRST;
            }
        } else {
            if (isLast) {
                t = OpcodeType.BINARY_PARTIAL_LAST;
            } else {
                t = OpcodeType.BINARY_PARTIAL_CONTINUATION;
            }
        }

        try {
            RETURN_STATUS ret = sendBinaryCommon(data, t, false);
            if (ret != RETURN_STATUS.OK) {
                // another write of an IO frame is outstanding on this connection
                IllegalStateException up = new IllegalStateException("write not allowed.  Most likely cause is that another Write or Close is in progress");
                throw up;
            }
        } finally {
            if (isLast == true) {
                messageWriteState = MessageWriteState.PARTIAL_NOT_IN_USE;
            }
        }
    }

    private RETURN_STATUS sendBinaryCommon(@Sensitive ByteBuffer data, OpcodeType type, boolean wait) throws IOException {
        WsByteBuffer wsbb = getBufferManager().wrap(data);

        // appears that no timeout is to be used in this websocket call.
        return connLink.writeBufferForBasicRemoteSync(wsbb, type, TCPRequestContext.NO_TIMEOUT, wait);

    }

    public void sendText(@Sensitive String textToSend, boolean isLast) throws IOException {

        OpcodeType t = null;

        if (messageWriteState == MessageWriteState.PARTIAL_BINARY_IN_USE) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Send while Send outstanding error.  throw IllegalStateException. messageWriteState is: " + messageWriteState);
            }
            // another send is outstanding on this connection, as per spec behaivor, need to throw an illegalStateException
            IllegalStateException up = new IllegalStateException();
            throw up;
        }

        if (messageWriteState == MessageWriteState.PARTIAL_NOT_IN_USE) {
            messageWriteState = MessageWriteState.PARTIAL_TEXT_IN_USE;
            if (isLast) {
                t = OpcodeType.TEXT_WHOLE;
            } else {
                t = OpcodeType.TEXT_PARTIAL_FIRST;
            }
        } else {
            if (isLast) {
                t = OpcodeType.TEXT_PARTIAL_LAST;
            } else {
                t = OpcodeType.TEXT_PARTIAL_CONTINUATION;
            }
        }

        try {
            RETURN_STATUS ret = sendTextCommon(textToSend, t);
            if (ret != RETURN_STATUS.OK) {
                // another write of an IO frame is outstanding on this connection
                IllegalStateException up = new IllegalStateException("write not allowed.  Most likely cause is that another Write or Close is in progress");
                throw up;
            }
        } finally {
            if (isLast == true) {
                messageWriteState = MessageWriteState.PARTIAL_NOT_IN_USE;
            }
        }
    }

    public void sendText(@Sensitive String textToSend) throws IOException {

        if (messageWriteState != MessageWriteState.PARTIAL_NOT_IN_USE) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Send while Send outstanding error.  throw IllegalStateException");
            }
            // another send is outstanding on this connection, as per spec behaivor, need to throw an illegalStateException
            IllegalStateException up = new IllegalStateException();
            throw up;
        }
        RETURN_STATUS ret = sendTextCommon(textToSend, OpcodeType.TEXT_WHOLE);
        if (ret != RETURN_STATUS.OK) {
            // another write of an IO frame is outstanding on this connection
            IllegalStateException up = new IllegalStateException("write not allowed.  Most likely cause is that another Write or Close is in progress");
            throw up;
        }
    }

    private RETURN_STATUS sendTextCommon(@Sensitive String textToSend, OpcodeType type) throws IOException {
        // convert String to a byte buffer
        RETURN_STATUS ret = null;
        byte[] ba = textToSend.getBytes();

        ba = textToSend.getBytes(Utils.UTF8_CHARSET);

        // get pooled buffer, don't wrap. This cuts down on a lot of garbage needing to be collected, especially during stress.  
        WsByteBuffer wsbb = getBufferManager().allocate(ba.length);
        wsbb.put(ba);
        wsbb.position(0);
        wsbb.limit(ba.length);

        // appears that no timeout is to be used in this websocket call.
        try {
            ret = connLink.writeBufferForBasicRemoteSync(wsbb, type, TCPRequestContext.NO_TIMEOUT, false);
        } finally {
            wsbb.release();
        }

        return ret;
    }

    public void sendObject(@Sensitive Object data) throws IOException,
                    EncodeException {

        if (messageWriteState != MessageWriteState.PARTIAL_NOT_IN_USE) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Send while Send outstanding error.  throw IllegalStateException");
            }
            // another send is outstanding on this connection, as per spec behaivor, need to throw an illegalStateException
            IllegalStateException up = new IllegalStateException();
            throw up;
        }

        RETURN_STATUS ret = connLink.writeObjectBasicRemoteSync(data);

        if (ret != RETURN_STATUS.OK) {
            // another write of an IO frame is outstanding on this connection
            IllegalStateException up = new IllegalStateException("write not allowed.  Most likely cause is that another Write or Close is in progress");
            throw up;
        }
    }

    public OutputStream getSendStream() throws IOException {

        if (messageWriteState != MessageWriteState.PARTIAL_NOT_IN_USE) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Send while Send outstanding error.  throw IllegalStateException");
            }
            IllegalStateException up = new IllegalStateException();
            throw up;
        }

        RemoteOutputStream ros = new RemoteOutputStream();
        ros.initialize(connLink);
        return ros;
    }

    public Writer getSendWriter() throws IOException {

        if (messageWriteState != MessageWriteState.PARTIAL_NOT_IN_USE) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Send while Send outstanding error.  throw IllegalStateException");
            }
            IllegalStateException up = new IllegalStateException();
            throw up;
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        RemoteWriter rw = new RemoteWriter(bos);
        rw.initialize(connLink);

        return rw;

    }
}
