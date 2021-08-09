/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.websocket.CloseReason;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.MessageHandler.Partial;
import javax.websocket.MessageHandler.Whole;
import javax.websocket.PongMessage;
import javax.websocket.SessionException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.wsoc.MessageReader.FSeqState;
import com.ibm.ws.wsoc.MessageWriter.WRITE_TYPE;
import com.ibm.ws.wsoc.WsocConnLink.CLOSE_FRAME_STATE;
import com.ibm.ws.wsoc.WsocConnLink.DATA_TYPE;
import com.ibm.ws.wsoc.WsocConnLink.READ_LINK_STATUS;
import com.ibm.ws.wsoc.util.Utils;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;
import com.ibm.wsspi.tcpchannel.TCPRequestContext;

/**
 *
 */
public class LinkRead {

    private static final TraceComponent tc = Tr.register(LinkRead.class);

    private final HashMap<Class<?>, ArrayList<Decoder>> binaryDecoders = new HashMap<Class<?>, ArrayList<Decoder>>();
    private final HashMap<Class<?>, ArrayList<Decoder>> textDecoders = new HashMap<Class<?>, ArrayList<Decoder>>();
    private final HashMap<Class<?>, ArrayList<Decoder>> binaryStreamDecoders = new HashMap<Class<?>, ArrayList<Decoder>>();
    private final HashMap<Class<?>, ArrayList<Decoder>> textStreamDecoders = new HashMap<Class<?>, ArrayList<Decoder>>();

    private MessageReader messageReader = null;
    private MessageHandler appTextMessageHandler = null;
    private MessageHandler appBinaryMessageHandler = null;
    private MessageHandler appPongMessageHandler = null;
    private TCPReadRequestContext tcpReadContext = null;
    private EndpointConfig endpointConfig = null;

    private Class<?> appTextMessageHandlerClass = null;
    private Class<?> appBinaryMessageHandlerClass = null;
    private Class<?> appPongMessageHandlerClass = null;
    private MessageHandler appTextPartialMessageHandler = null;
    private MessageHandler appBinaryPartialMessageHandler = null;
    private Class<?> appTextPartialMessageHandlerClass = null;
    private Class<?> appBinaryPartialMessageHandlerClass = null;

    private boolean annotatedTextMethodPresent = false;
    private boolean annotatedBinaryMethodPresent = false;
    private boolean annotatedPongMethodPresent = false;

    private WsocConnLink connLink = null;

    private boolean shouldReadMaskedData = false;

    private enum PartialState {
        NOT_ATTEMPTED,
        MESSAGE_HANDLER_ATTEMPTED,
        AE_TEXT_ATTEMPTED,
        AE_BINARY_ATTEMPTED,
        OFF
    }

    private PartialState partialProcessingState = PartialState.NOT_ATTEMPTED;

    public void initialize(TCPReadRequestContext _rrc, EndpointConfig _epc, Endpoint _ep, WsocConnLink _link, boolean _shouldReadMaskedData) {
        tcpReadContext = _rrc;
        endpointConfig = _epc;
        connLink = _link;
        shouldReadMaskedData = _shouldReadMaskedData;

        // can not allow message handlers to be added, if annotated ones already exist.
        if (_ep instanceof AnnotatedEndpoint) {
            AnnotatedEndpoint ae = (AnnotatedEndpoint) _ep;
            EndpointMethodHelper m = null;

            m = ae.getOnMessageTextMethod();
            if (m != null) {
                annotatedTextMethodPresent = true;
            }
            m = ae.getOnMessageBinaryMethod();
            if (m != null) {
                annotatedBinaryMethodPresent = true;
            }
            m = ae.getOnMessagePongMethod();
            if (m != null) {
                annotatedPongMethodPresent = true;
            }
        }

        messageReader = new MessageReader();
        WsocReadCallback readCallback = connLink.getReadCallback();
        messageReader.initialize(readCallback, connLink, shouldReadMaskedData);

    }

    public boolean processRead(TCPReadRequestContext rrc, Endpoint appEndPoint) {

        DATA_TYPE dt = DATA_TYPE.UNKNOWN;
        MessageReadInfo messageInfo;
        AnnotatedEndpoint ae = null;
        boolean appMessageProcessingAttempted = false;

        try {
            messageInfo = messageReader.processRead(rrc, isPartialAvailable(appEndPoint, true), isPartialAvailable(appEndPoint, false),
                                                    connLink.anticipatingCloseFrame());

        } catch (FrameFormatException ffe) {
            // allow instrumented FFDC to be used here
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception in reading incoming message frame: " + ffe.getMessage() + ". Calling Endpoint.onClose()");
            }
            String reasonPhrase = ffe.getMessage();
            //length of close reason can only be 123 UTF-8 encoded character bytes length
            reasonPhrase = Utils.truncateCloseReason(reasonPhrase);
            CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.PROTOCOL_ERROR, reasonPhrase);
            connLink.setReadLinkStatus(READ_LINK_STATUS.ON_READ_THREAD);
            connLink.closeUsingSession(closeReason, false, false);
            messageReader.reset();
            return false;
        } catch (WsocBufferException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No more Bytes available while reading an incoming message: " + e.getMessage() + ". Calling Endpoint.onError()");
            }

            String msg = Tr.formatMessage(tc,
                                          "bytes.notavailable");
            SessionException se = new SessionException(msg, e, connLink.getWsocSession());
            connLink.callOnError(se, true);
            return false;
        } catch (MaxMessageException e) {
            String reasonPhrase = e.getMessage();
            //length of close reason can only be 123 UTF-8 encoded character bytes length
            reasonPhrase = Utils.truncateCloseReason(reasonPhrase);
            // ok to pass null for either of these parameters
            CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.TOO_BIG, reasonPhrase);
            connLink.setReadLinkStatus(READ_LINK_STATUS.ON_READ_THREAD);
            connLink.closeUsingSession(closeReason, false, false);
            messageReader.reset();
            return false;
        }

        if (messageInfo.getState() == MessageReadInfo.State.CLOSE_FRAME_ERROR) {
            connLink.setReadLinkStatus(READ_LINK_STATUS.ON_READ_THREAD);
            connLink.closeUsingSession(null, false, true);
            messageReader.reset();
        }

        if ((messageInfo.getState() == MessageReadInfo.State.CONTROL_MESSAGE_EMBEDDED)
            || ((messageInfo.getState() == MessageReadInfo.State.COMPLETE)
                && ((partialProcessingState == PartialState.NOT_ATTEMPTED) || (partialProcessingState == PartialState.OFF)))) {

            // the opcode type store in messageInfo will not contain "PARTIAL" types, but partials will may to "WHOLE" types
            OpcodeType ot = messageInfo.getType();
            if (ot == OpcodeType.BINARY_WHOLE) {
                dt = DATA_TYPE.BINARY;
            } else if (ot == OpcodeType.TEXT_WHOLE) {
                dt = DATA_TYPE.TEXT;
            } else if (ot == OpcodeType.PING) {
                dt = DATA_TYPE.PING;
            } else if (ot == OpcodeType.PONG) {
                dt = DATA_TYPE.PONG;
            } else if (ot == OpcodeType.CONNECTION_CLOSE) {
                dt = DATA_TYPE.CLOSE;
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Could not determine message type");
                }
            }

            if ((connLink.getLinkStatus() == WsocConnLink.LINK_STATUS.LOCAL_CLOSING) && (dt != DATA_TYPE.CLOSE)) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "processRead :closeframe: Received a non close frame when close frame sent, ignoring.");
                }
                // keep reading, hopefully will get a close frame
                messageReader.reset();
                connLink.setReadLinkStatusAndCloseFrameState(READ_LINK_STATUS.OK_TO_READ, CLOSE_FRAME_STATE.ANTICIPATING);
                return false;
            }

            else if (dt == DATA_TYPE.CLOSE) {

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "processRead :closeframe: Received close frame: Close frame state: " + connLink.getCloseFrameState());
                }

                byte[] data = convertPayloadToByteArray(true);
                int closeCode = 1000;
                String reason = "";

                if (data.length == 1) {
                    closeCode = 1002;
                    reason = "Close frame must either zero bytes or 2 or more bytes.";
                } else if (data.length >= 2) {

                    // control frame data can not be greater than 125 characters.

                    int reasonLength = data.length - 2;

                    if (data.length > 125) {
                        reasonLength = 123;
                    }

                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "data len: " + data.length + " reason len: " + reasonLength);
                    }
                    closeCode = ((data[0]) & 0x000000ff) << 8 | ((data[1]) & 0x000000ff);
                    /*
                     * Question, Authobah seems like it wants a 1000 clsoe code sent when a 1001 is received... consider that.
                     *
                     * if (closeCode == 1001) {
                     * // Respond to a 1001 with a 1000 close code and no reason needed.
                     * closeCode = 1000;
                     * reason = "";
                     * }
                     */
                    if (reasonLength > 0) {
                        byte[] reasonData = new byte[reasonLength];
                        System.arraycopy(data, 2, reasonData, 0, (reasonLength));
                        try {
                            reason = Utils.uTF8byteArrayToString(reasonData);
                        } catch (CharacterCodingException e) {
                            closeCode = 1007;
                            reason = "Invalid utf-8 data received in  close frame.";
                        }

                    }
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "closeCode: " + closeCode + " reason: " + reason);
                    }
                }

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Closing with reason of: " + reason);
                }

                CloseReason cr = null;
                if ((closeCode == 1006) || (closeCode == 1004) || (closeCode == 1005) || ((closeCode > 1011) && (closeCode < 3000)) || (closeCode < 1000)) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Invalid closecode " + closeCode + " Coverting closecode to 1002");
                    }
                    cr = new CloseReason(CloseReason.CloseCodes.getCloseCode(1002), "Invalid close code " + closeCode + " provided, closing with protocol error.");
                } else {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Valid closecode " + closeCode);
                    }
                    cr = new CloseReason(CloseReason.CloseCodes.getCloseCode(closeCode), reason);
                }
                connLink.incomingCloseConnection(cr);
                messageReader.reset();
                appMessageProcessingAttempted = true;

            }
            if (dt == DATA_TYPE.PING) {
                byte[] retData = convertPayloadToByteArray(true);
                WsByteBuffer wsBuffer = connLink.getBufferManager().wrap(retData);
                connLink.writeBuffer(wsBuffer, OpcodeType.PONG, WRITE_TYPE.SYNC, null, 0, true, true);
                appMessageProcessingAttempted = true;
            } else if (appEndPoint instanceof AnnotatedEndpoint) {
                ae = (AnnotatedEndpoint) appEndPoint;
                EndpointMethodHelper m = null;

                if (dt == DATA_TYPE.TEXT) {
                    m = ae.getOnMessageTextMethod();
                    if (m != null) {
                        appMessageProcessingAttempted = true;
                        try {
                            processOnMessageTextAnnotation(ae, m, true);
                        } catch (CharacterCodingException e) {
                            CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.NOT_CONSISTENT, "Invalid UTF-8 data received.");
                            connLink.setReadLinkStatus(READ_LINK_STATUS.ON_READ_THREAD);
                            connLink.closeUsingSession(closeReason, true, false);
                            messageReader.reset();
                            return false;
                        }

                    }
                }
                if (dt == DATA_TYPE.BINARY) {
                    m = ae.getOnMessageBinaryMethod();
                    if (m != null) {
                        appMessageProcessingAttempted = true;
                        processOnMessageBinaryAnnotation(ae, m, true);
                    }
                }
                if (dt == DATA_TYPE.PONG) {
                    m = ae.getOnMessagePongMethod();
                    if (m != null) {
                        appMessageProcessingAttempted = true;
                        processOnMessagePongAnnotation(ae, m);
                    }
                }
            }

            if (appMessageProcessingAttempted == false) {
                try {
                    appMessageProcessingAttempted = processWithWholeMessageHandler(dt);
                    if (appMessageProcessingAttempted == false) {

                        if (dt != DATA_TYPE.PONG) {
                            // try processing this complete message with a partial message handler, if there is one
                            appMessageProcessingAttempted = processWithPartialMessageHandler(dt, true);

                            if (appMessageProcessingAttempted == false) {
                                // ok, now give up, we can not process this message
                                String name = null;
                                if (ae != null) {
                                    name = ae.getServerEndpointClass().getName();
                                } else {
                                    name = appEndPoint.getClass().getName();
                                }
                                String reasonPhrase = Tr.formatMessage(tc,
                                                                       "onmessage.notdefined",
                                                                       name);
                                //there is no FFDC here since there is no exception. Hence Log a error as this is a rare case.
                                Tr.warning(tc,
                                           "onmessage.notdefined",
                                           name);
                                connLink.callOnClose(reasonPhrase, CloseReason.CloseCodes.CANNOT_ACCEPT);
                            }
                        }
                    }
                } catch (CharacterCodingException e) {
                    CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.NOT_CONSISTENT, "Invalid UTF-8 data received.");
                    connLink.setReadLinkStatus(READ_LINK_STATUS.ON_READ_THREAD);
                    connLink.closeUsingSession(closeReason, true, false);
                    messageReader.reset();
                    return false;

                } catch (Throwable t) {
                    //any exception which are not handled in  processWithWholeMessageHandler() method is caught here.
                    //e.g RuntimeException thrown from @onMessage method
                    connLink.callOnError(t, true);
                }
            }
            // message was complete and processing was at least attempted, so get ready for the next message
            if (messageInfo.getState() != MessageReadInfo.State.CONTROL_MESSAGE_EMBEDDED) {
                partialProcessingState = PartialState.NOT_ATTEMPTED;
                // reset will release allocated frame ByteBuffers
                messageReader.reset();
            } else {
                // only reset frame parameters pertaining to the control message and will release allocated frame ByteBuffers
                messageReader.resetControlFrameParameters(true);

            }
        } else if ((partialProcessingState != PartialState.OFF)
                   && ((messageInfo.getState() == MessageReadInfo.State.PARTIAL_COMPLETE) || (messageInfo.getState() == MessageReadInfo.State.COMPLETE))) {
            // if the message state is complete, and we get here, then it means this is the last frame of this partial message.
            // if message state is partial, then entire frame(s) have been read in, but message is not complete,
            // so see if a partial message handler is available to use
            try {
                boolean isLast = false;
                boolean lookForProcessor = true;

                if (messageInfo.getState() == MessageReadInfo.State.COMPLETE) {
                    isLast = true;
                }

                OpcodeType ot = messageInfo.getType();
                if (ot == OpcodeType.BINARY_WHOLE) {
                    dt = DATA_TYPE.BINARY;
                } else if (ot == OpcodeType.TEXT_WHOLE) {
                    dt = DATA_TYPE.TEXT;
                } else {
                    lookForProcessor = false;
                    partialProcessingState = PartialState.OFF;
                }

                if (lookForProcessor) {
                    if (appEndPoint instanceof AnnotatedEndpoint) {
                        if (((partialProcessingState == PartialState.AE_TEXT_ATTEMPTED) || (partialProcessingState == PartialState.NOT_ATTEMPTED))
                            && (dt == DATA_TYPE.TEXT)) {

                            ae = (AnnotatedEndpoint) appEndPoint;
                            EndpointMethodHelper m = ae.getOnMessageTextMethod();
                            if (m != null) {
                                if (processOnMessageTextAnnotation(ae, m, isLast)) {
                                    messageReader.removeFirstFrameFromProcessor();
                                    partialProcessingState = PartialState.AE_TEXT_ATTEMPTED;
                                    messageReader.setFrameSequenceState(FSeqState.EXPECTING_PARTIAL_OR_LAST);
                                    lookForProcessor = false;
                                }
                            }
                        }
                        if ((lookForProcessor)
                            && ((partialProcessingState == PartialState.AE_BINARY_ATTEMPTED) || (partialProcessingState == PartialState.NOT_ATTEMPTED))
                            && (dt == DATA_TYPE.BINARY)) {

                            ae = (AnnotatedEndpoint) appEndPoint;
                            EndpointMethodHelper m = ae.getOnMessageBinaryMethod();
                            if (m != null) {
                                appMessageProcessingAttempted = true;
                                if (processOnMessageBinaryAnnotation(ae, m, isLast)) {
                                    messageReader.removeFirstFrameFromProcessor();
                                    partialProcessingState = PartialState.AE_BINARY_ATTEMPTED;
                                    messageReader.setFrameSequenceState(FSeqState.EXPECTING_PARTIAL_OR_LAST);
                                    lookForProcessor = false;
                                }
                            }
                        }
                    }

                    if ((lookForProcessor) &&
                        (partialProcessingState == PartialState.MESSAGE_HANDLER_ATTEMPTED) || (partialProcessingState == PartialState.NOT_ATTEMPTED)) {
                        if (processWithPartialMessageHandler(dt, isLast)) {
                            partialProcessingState = PartialState.MESSAGE_HANDLER_ATTEMPTED;
                            messageReader.setFrameSequenceState(FSeqState.EXPECTING_PARTIAL_OR_LAST);
                            lookForProcessor = false;
                        }
                    }

                    if (lookForProcessor) {
                        if (partialProcessingState == PartialState.NOT_ATTEMPTED) {
                            // didn't work first time, so don't use partial for this attempt.
                            partialProcessingState = PartialState.OFF;
                            messageReader.setFrameSequenceState(FSeqState.EXPECTING_PARTIAL_OR_LAST);
                        } else {

                            // partial message handler was working on this message, but then got removed.
                            String reasonPhrase = Tr.formatMessage(tc,
                                                                   "onmessage.notdefined",
                                                                   appEndPoint.getClass().getName());
                            //there is no FFDC here since there is no exception. Hence log a warning. Should it be Tr.error() instead?
                            Tr.warning(tc,
                                       "onmessage.notdefined",
                                       appEndPoint.getClass().getName());
                            connLink.callOnClose(reasonPhrase, CloseReason.CloseCodes.CANNOT_ACCEPT);
                        }
                    }
                }

            } catch (CharacterCodingException e) {
                CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.NOT_CONSISTENT, "Invalid UTF-8 data received.");
                connLink.setReadLinkStatus(READ_LINK_STATUS.ON_READ_THREAD);
                connLink.closeUsingSession(closeReason, true, false);
                messageReader.reset();
                return false;
            } catch (Throwable t) {
                //any exception which are not handled in  processWithWholeMessageHandler() method is caught here.
                //e.g RuntimeException thrown from @onMessage method
                connLink.callOnError(t, true);
            }

            if (messageInfo.getState() == MessageReadInfo.State.COMPLETE) {
                // message was complete and processed, so get ready for the next message
                partialProcessingState = PartialState.NOT_ATTEMPTED;
                messageReader.reset();
            }
        }
        return messageInfo.isMoreBufferToProcess();
    }

    @SuppressWarnings("unchecked")
    // return true - data was attempted to be processed, false - nothing was defined to attempt to process the data.
    public boolean processWithPartialMessageHandler(DATA_TYPE dt, boolean isLast) throws CharacterCodingException {
        if ((appTextPartialMessageHandler != null) && (dt == DATA_TYPE.TEXT)) {

            Class<?> clazz = appTextPartialMessageHandlerClass;

            if (clazz.equals(String.class)) {
                String message = convertMessageToString();

                // call the application's MessageHandler.onMessage(<String> x) method
                try {
                    connLink.setReadLinkStatus(READ_LINK_STATUS.ON_READ_THREAD);
                    ((MessageHandler.Partial<String>) appTextPartialMessageHandler).onMessage(message, isLast);
                } finally {
                    messageReader.removeFirstFrameFromProcessor();
                }

                return true;
            }
        }

        if ((appBinaryPartialMessageHandler != null) && (dt == DATA_TYPE.BINARY)) {
            Class<?> clazz = appBinaryPartialMessageHandlerClass;

            if (clazz.equals(ByteBuffer.class)) {
                ByteBuffer message = convertMessageToByteBuffer();

                // call the application's MessageHandler.onMessage(<ByteBuffer> x) method
                try {
                    connLink.setReadLinkStatus(READ_LINK_STATUS.ON_READ_THREAD);
                    ((MessageHandler.Partial<ByteBuffer>) appBinaryPartialMessageHandler).onMessage(message, isLast);
                } finally {
                    messageReader.removeFirstFrameFromProcessor();
                }

                return true;
            }

            else if (clazz.equals(byte[].class)) {
                byte[] message = convertPayloadToByteArray();

                // call the application's MessageHandler.onMessage(<byte[]> x) method
                try {
                    connLink.setReadLinkStatus(READ_LINK_STATUS.ON_READ_THREAD);
                    ((MessageHandler.Partial<byte[]>) appBinaryPartialMessageHandler).onMessage(message, isLast);
                } finally {
                    messageReader.removeFirstFrameFromProcessor();
                }

                return true;
            }
        }

        return false;
    }

    // return true - data was attempted to be processed, false - nothing was defined to attempt to process the data.
    @FFDCIgnore(IOException.class)
    public boolean processWithWholeMessageHandler(DATA_TYPE dt) throws CharacterCodingException {
        if ((appTextMessageHandler != null) && (dt == DATA_TYPE.TEXT)) {

            Class<?> clazz = appTextMessageHandlerClass;

            if (clazz.equals(String.class)) {
                String message = convertMessageToString();

                // call the application's MessageHandler.onMessage(<String> x) method
                connLink.setReadLinkStatus(READ_LINK_STATUS.ON_READ_THREAD);
                ((MessageHandler.Whole<String>) appTextMessageHandler).onMessage(message);
                return true;
            }

            else if (clazz.equals(Reader.class)) {
                Reader message = convertMessageToReader();

                // call the application's MessageHandler.onMessage(<Reader> x) method
                connLink.setReadLinkStatus(READ_LINK_STATUS.ON_READ_THREAD);
                ((MessageHandler.Whole<Reader>) appTextMessageHandler).onMessage(message);
                return true;
            }
        }

        if ((appBinaryMessageHandler != null) && (dt == DATA_TYPE.BINARY)) {
            Class<?> clazz = appBinaryMessageHandlerClass;

            if (clazz.equals(ByteBuffer.class)) {
                ByteBuffer message = convertMessageToByteBuffer();

                // call the application's MessageHandler.onMessage(<ByteBuffer> x) method
                connLink.setReadLinkStatus(READ_LINK_STATUS.ON_READ_THREAD);
                ((MessageHandler.Whole<ByteBuffer>) appBinaryMessageHandler).onMessage(message);
                return true;
            }

            else if (clazz.equals(byte[].class)) {
                byte[] message = convertPayloadToByteArray();

                // call the application's MessageHandler.onMessage(<byte[]> x) method
                connLink.setReadLinkStatus(READ_LINK_STATUS.ON_READ_THREAD);
                ((MessageHandler.Whole<byte[]>) appBinaryMessageHandler).onMessage(message);
                return true;
            }

            else if (clazz.equals(InputStream.class)) {
                InputStream message = convertMessageToInputStream();

                // call the application's MessageHandler.onMessage(<InputStream> x) method
                connLink.setReadLinkStatus(READ_LINK_STATUS.ON_READ_THREAD);
                ((MessageHandler.Whole<InputStream>) appBinaryMessageHandler).onMessage(message);
                return true;
            }
        }
        if ((appPongMessageHandler != null) && (dt == DATA_TYPE.PONG)) {
            Class<?> clazz = appPongMessageHandlerClass;
            if (clazz.equals(PongMessage.class)) {
                ByteBuffer message = convertMessageToByteBuffer(true);
                PongMessage msg = new PongMessageImpl(message);

                // call the application's MessageHandler.onMessage(<Reader> x) method
                connLink.setReadLinkStatus(READ_LINK_STATUS.ON_READ_THREAD);
                ((MessageHandler.Whole<PongMessage>) appPongMessageHandler).onMessage(msg);
                return true;
            }
        }

        // if the generic type for this MessageHandler.Whole equals one of the configured decoder generic types, then use that decoder
        // clazz: is the generic class that the MessageHandler.Whole specifies
        // then go through the decoders that also have been specified with this same (or assignable) generic class.
        // in other words, let me see if I have any decoders<X> to use with this  MessageHandler<X>.
        if ((appTextMessageHandler != null) && (dt == DATA_TYPE.TEXT)) {

            Class<?> clazz = appTextMessageHandlerClass;
            ArrayList<Decoder> dcList = findDecoderFromAppDecoderType(clazz, dt);

            if (dcList != null && dcList.size() > 0) {
                Decoder firstDecoder = dcList.get(0);
                if (firstDecoder instanceof Decoder.Text) {
                    String message = convertMessageToString();
                    //user might have declared multiple decoders of same type for this endpoint. In this case, check
                    //decoder list and find the first decoder in the list which has implemented willDecode(..) to return
                    //'true'
                    Decoder dc = findWillDecodeDecoder(dcList, message);
                    if (dc != null) {
                        try {
                            dc.init(endpointConfig);
                            Object appObj = ((Decoder.Text) dc).decode(message);
                            connLink.setReadLinkStatus(READ_LINK_STATUS.ON_READ_THREAD);
                            ((MessageHandler.Whole) appTextMessageHandler).onMessage(appObj);
                            return true;
                        } catch (DecodeException e) {
                            connLink.callOnError(e, true);
                            return true;
                        }
                    }
                } else if (firstDecoder instanceof Decoder.TextStream) {
                    Reader message = convertMessageToReader();
                    try {
                        //WebSocket API does not support willDecode() method for Decoder.BinaryStream type. Hence, even if
                        //user has declared multiple same type of decoder, just pick the first one in the list and use it.

                        firstDecoder.init(endpointConfig);
                        Object appObj = ((Decoder.TextStream) firstDecoder).decode(message);
                        connLink.setReadLinkStatus(READ_LINK_STATUS.ON_READ_THREAD);
                        ((MessageHandler.Whole) appTextMessageHandler).onMessage(appObj);
                        return true;
                    } catch (DecodeException e) {
                        connLink.callOnError(e, true);
                        return true;
                    } catch (IOException e) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "IOException calling onMessage on class " + clazz.getName() + " while decoding Decoder.TextStream type "
                                         + ((Decoder.TextStream) firstDecoder).toString()
                                         + "Exception is: " + e.getMessage());
                        }
                        //Since IOexception has occurred, there is no point in attempting to read message TextStream to convert into
                        //message string to pass as first param to form DecodeException. Hence passing just a error message
                        //"IOException decoding BinaryStream" as first parameter here
                        String msg = Tr.formatMessage(tc,
                                                      "decoder.ioexception",
                                                      appTextMessageHandler.getClass().getName(), firstDecoder.getClass().getName());

                        DecodeException de = new DecodeException(msg, e.getMessage(), e);
                        connLink.callOnError(de, true);
                        return true;
                    }
                }
            }
        }

        if ((appBinaryMessageHandler != null) && (dt == DATA_TYPE.BINARY)) {
            Class<?> clazz = appBinaryMessageHandlerClass;
            ArrayList<Decoder> dcList = findDecoderFromAppDecoderType(clazz, dt);

            if (dcList != null && dcList.size() > 0) {
                Decoder firstDecoder = dcList.get(0);
                if (firstDecoder instanceof Decoder.Binary) {
                    // Decode the given bytes into an object of type T
                    ByteBuffer message = convertMessageToByteBuffer();
                    //user might have declared multiple decoders of same type for this endpoint. In this case, check
                    //decoder list and find the first decoder in the list which has implemented willDecode(..) to return
                    //'true'
                    Decoder dc = findWillDecodeDecoder(dcList, message);
                    if (dc != null) {
                        try {
                            dc.init(endpointConfig);
                            Object appObj = ((Decoder.Binary) dc).decode(message);
                            connLink.setReadLinkStatus(READ_LINK_STATUS.ON_READ_THREAD);
                            ((MessageHandler.Whole) appBinaryMessageHandler).onMessage(appObj);
                            return true;
                        } catch (DecodeException e) {
                            connLink.callOnError(e, true);
                            return true;
                        }
                    } else {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "willDecode returned false");
                        }
                        return true;
                    }
                } else if (firstDecoder instanceof Decoder.BinaryStream) {
                    InputStream message = convertMessageToInputStream();
                    try {
                        //WebSocket API does not support willDecode() method for Decoder.BinaryStream type. Hence, even if
                        //user has declared multiple same type of decoder, just pick the first one in the list and use it.
                        firstDecoder.init(endpointConfig);
                        Object appObj = ((Decoder.BinaryStream) firstDecoder).decode(message);
                        connLink.setReadLinkStatus(READ_LINK_STATUS.ON_READ_THREAD);
                        ((MessageHandler.Whole) appBinaryMessageHandler).onMessage(appObj);
                        return true;
                    } catch (DecodeException e) {
                        connLink.callOnError(e, true);
                        return true;
                    } catch (IOException e) {
                        // do NOT allow instrumented FFDC to be used here
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "IOException calling onMessage on class " + clazz.getName() + " while decoding Decoder.BinaryStream type "
                                         + ((Decoder.BinaryStream) firstDecoder).toString()
                                         + "Exception is: " + e.getMessage());
                        }
                        //Since IOexception has occurred, there is no point in attempting to read message BinaryStream to convert into
                        //message string to pass as first param to form DecodeException. Hence passing just a error message
                        //"IOException decoding BinaryStream" as first parameter here
                        String msg = Tr.formatMessage(tc,
                                                      "decoder.ioexception",
                                                      appBinaryMessageHandler.getClass().getName(), ((Decoder.BinaryStream) firstDecoder).getClass().getName());
                        DecodeException de = new DecodeException(msg, e.getMessage(), e);
                        connLink.callOnError(de, true);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void processOnMessagePongAnnotation(AnnotatedEndpoint ae, EndpointMethodHelper m) {
        Object args[] = new Object[m.getMethod().getParameterTypes().length];
        MethodData methodData = ae.getOnMessagePongMethod().getMethodData();
        Class<?> inputType = methodData.getMessageType();
        int msgIndex = methodData.getMessageIndex();
        //check if method has optional Session parameter
        int sessionIndex = methodData.getSessionIndex();
        if (sessionIndex >= 0) {
            args[sessionIndex] = connLink.getWsocSession();
        }

        if (inputType.equals(PongMessage.class)) {
            ByteBuffer message = convertMessageToByteBuffer(true);
            PongMessage pm = new PongMessageImpl(message);
            args[msgIndex] = pm;

            Object appInstance = ae.getAppInstance();
            Object obj = callOnMessage(args, appInstance, m, ae);

            if (obj != null) {
                connLink.writeObject(obj, WRITE_TYPE.SYNC, null, true);
            }
        }
    }

    public boolean isPartialAvailable(Endpoint appEndPoint, boolean isText) {

        if (isText) {
            if (appTextPartialMessageHandler != null) {
                return true;
            }

            if (appEndPoint instanceof AnnotatedEndpoint) {
                AnnotatedEndpoint ae = (AnnotatedEndpoint) appEndPoint;
                if (ae.getOnMessageTextMethod() != null) {
                    if (isPartialMethodPresent(ae.getOnMessageTextMethod().getMethodData())) {
                        return true;
                    }
                }
            }
        } else {
            if (appBinaryPartialMessageHandler != null) {
                return true;
            }
            if (appEndPoint instanceof AnnotatedEndpoint) {
                AnnotatedEndpoint ae = (AnnotatedEndpoint) appEndPoint;
                if (ae.getOnMessageBinaryMethod() != null) {
                    if (isPartialMethodPresent(ae.getOnMessageBinaryMethod().getMethodData())) {
                        return true;
                    }
                }
            }
        }
        return false;

    }

    public boolean isPartialMethodPresent(MethodData methodData) {
        boolean retVal = false;
        int booleanIndex = methodData.getMsgBooleanPairIndex();
        if (booleanIndex >= 0) {
            retVal = true;
        }
        return retVal;
    }

    /**
     *
     * False is returned if this is called with isLast set to true, but no there partial boolean flag on the annotated method.
     *
     * @throws CharacterCodingException
     */
    @FFDCIgnore(IOException.class)
    public boolean processOnMessageTextAnnotation(AnnotatedEndpoint ae, EndpointMethodHelper m, boolean isLast) throws CharacterCodingException {
        Object args[] = new Object[m.getMethod().getParameterTypes().length];
        MethodData methodData = ae.getOnMessageTextMethod().getMethodData();
        Class<?> inputType = methodData.getMessageType();
        int msgIndex = methodData.getMessageIndex();
        //check if method has optional Session parameter
        int sessionIndex = methodData.getSessionIndex();
        if (sessionIndex >= 0) {
            args[sessionIndex] = connLink.getWsocSession();
        }

        if (inputType.equals(String.class)) {

            //Spec: method can have optional String and boolean pair to receive the message in parts
            //check if this msg has a boolean pair to indicate if this is a last part of the message
            int booleanIndex = methodData.getMsgBooleanPairIndex();
            if (booleanIndex >= 0) {
                args[booleanIndex] = isLast;
            } else if (isLast == false) {
                // AE doesn't do partial messages
                return false;
            }
            String message = convertMessageToString();
            args[msgIndex] = message;
        } else if (isLast == false) {
            return false;
        } else if (inputType.equals(Reader.class)) {
            Reader message = convertMessageToReader();
            args[msgIndex] = message;
        } else if (inputType.equals(boolean.class)) {
            String payload = convertPayloadToString();
            boolean message = Boolean.valueOf(payload).booleanValue();
            args[msgIndex] = message;
        } else if (inputType.equals(Boolean.class)) {
            String payload = convertPayloadToString();
            Boolean message = Boolean.valueOf(payload);
            args[msgIndex] = message;
        } else if (inputType.equals(byte.class)) {
            byte message = Byte.parseByte(convertPayloadToString());
            args[msgIndex] = message;
        } else if (inputType.equals(Byte.class)) {
            args[msgIndex] = Byte.parseByte(convertPayloadToString());
        } else if (inputType.equals(short.class)) {
            String payload = convertPayloadToString();
            short message = new Short(payload).shortValue();
            args[msgIndex] = message;
        } else if (inputType.equals(Short.class)) {
            String payload = convertPayloadToString();
            Short message = new Short(payload);
            args[msgIndex] = message;
        } else if (inputType.equals(char.class)) {
            byte ba[] = convertPayloadToByteArray();
            char message = 0;
            try {
                message = Utils.byteArrayToChar(ba);
            } catch (WsocBufferException e) {
                connLink.callOnError(e.getCause(), true);
            }
            args[msgIndex] = message;
        } else if (inputType.equals(Character.class)) {
            byte ba[] = convertPayloadToByteArray();
            char message = 0;
            try {
                message = Utils.byteArrayToChar(ba);
            } catch (WsocBufferException e) {
                connLink.callOnError(e.getCause(), true);
            }
            args[msgIndex] = Character.valueOf(message);
        } else if (inputType.equals(int.class)) {
            String payload = convertPayloadToString();
            int message = new Integer(payload).intValue();
            args[msgIndex] = message;
        } else if (inputType.equals(Integer.class)) {
            String payload = convertPayloadToString();
            Integer message = new Integer(payload);
            args[msgIndex] = message;
        } else if (inputType.equals(long.class)) {
            String payload = convertPayloadToString();
            long message = new Long(payload).longValue();
            args[msgIndex] = message;
        } else if (inputType.equals(Long.class)) {
            String payload = convertPayloadToString();
            Long message = new Long(payload);
            args[msgIndex] = message;
        } else if (inputType.equals(float.class)) {
            String payload = convertPayloadToString();
            float message = new Float(payload).floatValue();
            args[msgIndex] = message;
        } else if (inputType.equals(Float.class)) {
            String payload = convertPayloadToString();
            Float message = new Float(payload);
            args[msgIndex] = message;
        } else if (inputType.equals(double.class)) {
            String payload = convertPayloadToString();
            double message = new Double(payload).doubleValue();
            args[msgIndex] = message;
        } else if (inputType.equals(Double.class)) {
            String payload = convertPayloadToString();
            Double message = new Double(payload);
            args[msgIndex] = message;
        } else {
            // see if this endpoint has a decoder for this type of message
            // Performance: EndpointMethodHelper could store the parameterized type found in AnnotatedEndpoint and then findDecoder... wouldn't need to find it again.
            ArrayList<Decoder> dcList = findDecoderFromAppDecoderType(inputType, DATA_TYPE.TEXT);
            if (dcList != null && dcList.size() > 0) {
                Decoder firstDecoder = dcList.get(0);

                if (firstDecoder instanceof Decoder.Text) {
                    String message = convertMessageToString();
                    //user might have declared multiple decoders of same type for this endpoint. In this case, check
                    //decoder list and find the first decoder in the list which has implemented willDecode(..) to return
                    //'true'
                    Decoder dc = findWillDecodeDecoder(dcList, message);
                    if (dc != null) {
                        try {
                            dc.init(endpointConfig);
                            Object appObj = ((Decoder.Text) dc).decode(message);
                            args[msgIndex] = appObj;
                        } catch (DecodeException e) {
                            connLink.callOnError(e, true);
                            args = null;
                        }
                    }
                }

                else if (firstDecoder instanceof Decoder.TextStream) {
                    Reader message = convertMessageToReader();

                    try {
                        firstDecoder.init(endpointConfig);
                        //WebSocket API does not support willDecode() method for Decoder.BinaryStream type. Hence, even if
                        //user has declared multiple same type of decoder, just pick the first one in the list and use it.
                        Object appObj = ((Decoder.TextStream) firstDecoder).decode(message);
                        args[msgIndex] = appObj;
                    } catch (DecodeException e) {
                        args = null;
                        connLink.callOnError(e, true);
                    } catch (IOException e) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "IOException calling onMessage method " + m.getMethod().getName() + " in class " + ae.getServerEndpointClass().getName()
                                         + " while decoding Decoder.TextStream type " + ((Decoder.TextStream) firstDecoder).toString()
                                         + "Exception is: " + e.getMessage());
                        }
                        args = null;

                        //Since IOexception has occurred, there is no point in attempting to read message TextStream to convert into
                        //message string to pass as first param to form DecodeException. Hence passing just a error message
                        //"IOException decoding BinaryStream" as first parameter here
                        String msg = Tr.formatMessage(tc,
                                                      "decoder.ioexception",
                                                      ae.getServerEndpointClass().getName(), ((Decoder.TextStream) firstDecoder).getClass().getName());
                        DecodeException de = new DecodeException(msg, e.getMessage(), e);
                        connLink.callOnError(de, true);
                    }
                }
            }
        }

        // proceed if we were able to determine the argument(s) for the call
        if (args != null) {
            Object appInstance = ae.getAppInstance();
            Object obj = callOnMessage(args, appInstance, m, ae);
            if (obj != null) {
                connLink.writeObject(obj, WRITE_TYPE.SYNC, null, true);
            }
        }
        return true;
    }

    @FFDCIgnore(IOException.class)
    public boolean processOnMessageBinaryAnnotation(AnnotatedEndpoint ae, EndpointMethodHelper m, boolean isLast) {
        Object args[] = new Object[m.getMethod().getParameterTypes().length];
        MethodData methodData = ae.getOnMessageBinaryMethod().getMethodData();
        Class<?> inputType = methodData.getMessageType();
        int msgIndex = methodData.getMessageIndex();
        //check if method has optional Session parameter
        int sessionIndex = methodData.getSessionIndex();
        if (sessionIndex >= 0) {
            args[sessionIndex] = connLink.getWsocSession();
        }

        if (inputType.equals(java.nio.ByteBuffer.class)) {

            //Spec: method can have optional byte[] and boolean pair, or ByteBuffer and boolean pair to receive the message in parts
            //check if this msg has a boolean pair to indicate if this is a last part of the message
            int booleanIndex = methodData.getMsgBooleanPairIndex();
            if (booleanIndex >= 0) {
                args[booleanIndex] = isLast;
            } else if (isLast == false) {
                // this AE doesn't do partial messages
                return false;
            }
            ByteBuffer message = convertMessageToByteBuffer();
            args[msgIndex] = message;
        } else if (inputType.equals(byte[].class)) {
            byte[] message = this.convertPayloadToByteArray();
            args[msgIndex] = message;
            //Spec: method can have optional byte[] and boolean pair, or ByteBuffer and boolean pair to receive the message in parts
            //check if this msg has a boolean pair to indicate if this is a last part of the message
            int booleanIndex = methodData.getMsgBooleanPairIndex();
            if (booleanIndex >= 0) {
                args[booleanIndex] = isLast;
            } else if (isLast == false) {
                // this AE doesn't do partial messages
                return false;
            }
        } else if (isLast == false) {
            return false;
        } else if (inputType.equals(InputStream.class)) {

            InputStream message = this.convertMessageToInputStream();
            args[msgIndex] = message;
        } else {
            ArrayList<Decoder> dcList = findDecoderFromAppDecoderType(inputType, DATA_TYPE.BINARY);
            if (dcList != null && dcList.size() > 0) {
                Decoder firstDecoder = dcList.get(0);

                //just check the first decoder in the list because a list will contain only one type of decoder. For e.g
                //if the first decoder is of type Decoder.Binary, then all the decoders in that decoder list will of type
                //Decoder.Binary

                if (firstDecoder instanceof Decoder.Binary) {

                    // Decode the given bytes into an object of type T
                    ByteBuffer message = convertMessageToByteBuffer();
                    //user might have declared multiple decoders of same type for this endpoint. In this case, check
                    //decoder list and find the first decoder in the list which has implemented willDecode(..) to return
                    //'true'
                    Decoder dc = findWillDecodeDecoder(dcList, message);
                    if (dc != null) {
                        try {
                            dc.init(endpointConfig);
                            Object appObj = ((Decoder.Binary) dc).decode(message);
                            args[msgIndex] = appObj;
                        } catch (DecodeException e) {
                            connLink.callOnError(e, true);
                        }
                    }
                }

                else if (firstDecoder instanceof Decoder.BinaryStream) {
                    InputStream message = convertMessageToInputStream();
                    try {
                        //WebSocket API does not support willDecode() method for Decoder.BinaryStream type. Hence, even if
                        //user has declared multiple same type of decoder, just pick the first one in the list and use it.
                        firstDecoder.init(endpointConfig);
                        Object appObj = ((Decoder.BinaryStream) firstDecoder).decode(message);
                        args[msgIndex] = appObj;
                    } catch (DecodeException e) {
                        connLink.callOnError(e, true);
                    } catch (IOException e) {
                        // do NOT allow instrumented FFDC to be used here
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "IOException calling onMessage method " + m.getMethod().getName() + " in class " + ae.getServerEndpointClass().getName()
                                         + " while decoding Decoder.BinaryStream type " + ((Decoder.BinaryStream) firstDecoder).toString()
                                         + "Exception is: " + e.getMessage());
                        }
                        //Since IOexception has occured, there is no point int attempting to read message InputStream to convert into
                        //ByteBuffer to pass as first param to form DecodeException. Hence passing just a error message ""IOException decoding BinaryStream"
                        //as first parameter here
                        String msg = Tr.formatMessage(tc,
                                                      "decoder.ioexception",
                                                      ae.getServerEndpointClass().getName(), ((Decoder.BinaryStream) firstDecoder).getClass().getName());
                        DecodeException de = new DecodeException(msg, e.getMessage(), e);
                        connLink.callOnError(de, true);
                    }
                }
            }
        }

        // proceed if we were able to determine the argument(s) for the call
        if (args != null) {
            Object appInstance = ae.getAppInstance();
            Object obj = callOnMessage(args, appInstance, m, ae);
            if (obj != null) {
                connLink.writeObject(obj, WRITE_TYPE.SYNC, null, true);
            }
        }

        return true;
    }

    @Sensitive
    public Object callOnMessage(@Sensitive Object args[], Object _appInstance, EndpointMethodHelper _m, AnnotatedEndpoint ae) {
        Object methodReturn = null;
        //substitute values for @PathParam variables for this method.
        try {
            _m.processPathParameters(ae, args);
        } catch (DecodeException e) {
            /*
             * JSR 356 @PathParam section
             * "If the container cannot decode the path segment appropriately to the annotated path parameter, then the container must raise an DecodeException to the error
             * handling method of the websocket containing the path segment. [WSC-4.3-6]"
             */
            connLink.callOnError(e, true);
            return null;
        }
        try {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "METHOD: " + _m.getMethod().getName() + " Number of arguments: " + args.length + " Message index: " + _m.getMethodData().getMessageIndex()
                             + " Boolean pair index: " + _m.getMethodData().getMsgBooleanPairIndex() + " Session index: " + _m.getMethodData().getSessionIndex());
                //don't trace arg values (security), but leave in for special debug if needed.
                //    for (int i = 0; i < args.length; i++) {
                //       Tr.debug(tc, "Argument " + i + ":   " + args[i]);
                // }
            }
            connLink.setReadLinkStatus(READ_LINK_STATUS.ON_READ_THREAD);
            methodReturn = _m.getMethod().invoke(_appInstance, args);

        } catch (IllegalAccessException e) {
            connLink.callOnError(getCause(e), true);
        } catch (IllegalArgumentException e) {
            connLink.callOnError(getCause(e), true);
        } catch (InvocationTargetException e) {
            connLink.callOnError(getCause(e), true);
        } catch (Throwable t) {
            connLink.callOnError(getCause(t), true);
        }
        return methodReturn;
    }

    //TCK wants the 'cause' of exception to be passed to @OnError not the outer wrapper exception.
    //get if 'cause' exists, else return the original exception
    private Throwable getCause(Throwable t) {
        Throwable cause = t.getCause();
        if (t.getCause() != null) {
            return cause;
        } else {
            return t;
        }
    }

    private ArrayList<Decoder> findDecoderFromAppDecoderType(Class<?> typeToLookFor, DATA_TYPE dataType) {
        List<Class<? extends Decoder>> decoders = endpointConfig.getDecoders();
        ArrayList<Decoder> dcList = null;
        // no decoders, that's easy
        if (decoders == null) {
            return null;
        }

        Class<?> clazz = null;
        try {
            for (Class<? extends Decoder> decoder : decoders) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "looking at decoder: " + decoder);
                }
                // try to find a decoder with the same generic type as that of the type we are looking for.
                ArrayList<Type> interfaces = new ArrayList<Type>();
                Utils.getAllInterfaces(decoder, interfaces);
                Object ta[] = interfaces.toArray();
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "ta[]: " + Arrays.toString(ta));
                }
                for (Object t : ta) {
                    clazz = Utils.getCodingClass((Type) t);

                    if (clazz != null) {
                        if (clazz.equals(typeToLookFor)) {

                            dcList = getDecoderFromCache(clazz);

                            if (dcList == null) {
                                dcList = new ArrayList<Decoder>();
                                dcList.add(decoder.newInstance());
                                if (tc.isDebugEnabled()) {
                                    Tr.debug(tc, "(1) Added decoder of class: " + decoder);
                                }
                            } else {
                                // only add this if there are no instance of this class already
                                boolean foundOne = false;
                                for (int i = 0; i < dcList.size(); i++) {
                                    if (decoder.equals(dcList.get(i).getClass())) {
                                        foundOne = true;
                                        break;
                                    }
                                }
                                if (!foundOne) {
                                    dcList.add(decoder.newInstance());
                                    if (tc.isDebugEnabled()) {
                                        Tr.debug(tc, "(2) Added decoder of class: " + decoder);
                                    }
                                }
                            }
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, "Number of decoders: " + dcList.size());
                            }
                            //just check the first decoder in the list because a list will contain only one type of decoder. For e.g
                            //if the first decoder is of type Decoder.Binary, then all the decoders in that decoder list will of type
                            //Decoder.Binary
                            if (dcList.get(0) instanceof Decoder.Binary) {
                                binaryDecoders.put(clazz, dcList);
                            } else if (dcList.get(0) instanceof Decoder.Text) {
                                textDecoders.put(clazz, dcList);
                            } else if (dcList.get(0) instanceof Decoder.BinaryStream) {
                                binaryStreamDecoders.put(clazz, dcList);
                            } else if (dcList.get(0) instanceof Decoder.TextStream) {
                                textStreamDecoders.put(clazz, dcList);
                            }
                        }
                    }
                }
            }
        } catch (InstantiationException ie) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Problem creating new instance of decoder class " + clazz.getName() + " Exception: " + ie.getMessage() + ". Calling Endpoint.onError()");
            }
            String msg = Tr.formatMessage(tc,
                                          "decoder.create.exception",
                                          clazz.getName(), ie.getMessage());
            DecodeException de = new DecodeException(msg, ie.getMessage(), ie);
            connLink.callOnError(de, true);
        } catch (IllegalAccessException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Problem creating new instance of decoder class " + clazz.getName() + " Exception: " + e.getMessage() + ". Calling Endpoint.onError()");
            }
            String msg = Tr.formatMessage(tc,
                                          "decoder.create.exception",
                                          clazz.getName(), e.getMessage());
            DecodeException de = new DecodeException(msg, e.getMessage(), e);
            connLink.callOnError(de, true);
        }
        if (dcList != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Number of decoders: " + dcList.size());
            }
        }
        return dcList;
    }

    private ArrayList<Decoder> getDecoderFromCache(Class<?> decoderType) {

        if (binaryDecoders != null) {
            ArrayList<Decoder> dc = binaryDecoders.get(decoderType);
            if (dc != null) {
                return dc;
            }
        }

        if (textDecoders != null) {
            ArrayList<Decoder> dc = textDecoders.get(decoderType);
            if (dc != null) {
                return dc;
            }
        }

        if (binaryStreamDecoders != null) {
            ArrayList<Decoder> dc = binaryStreamDecoders.get(decoderType);
            if (dc != null) {
                return dc;
            }
        }

        if (textStreamDecoders != null) {
            ArrayList<Decoder> dc = textStreamDecoders.get(decoderType);
            if (dc != null) {
                return dc;
            }
        }

        return null;

    }

    @Sensitive
    private byte[] convertPayloadToByteArray() {
        return convertPayloadToByteArray(false);
    }

    @Sensitive
    private byte[] convertPayloadToByteArray(boolean ControlFrame) {
        int size;
        WsByteBuffer[] buffers;

        if (!ControlFrame) {
            buffers = messageReader.getMessagePayload();
            size = messageReader.getMessageCompletePayloadSize();
        } else {
            buffers = messageReader.getMessagePayload_Control();
            size = messageReader.getMessageCompletePayloadSize_Control();
        }

        byte result[] = new byte[size];

        int start = 0;
        int length = 0;
        // read all the data into one byte array
        if (buffers != null) {
            for (WsByteBuffer buf : buffers) {
                length = buf.limit() - buf.position();
                buf.get(result, start, length);
                start += length;
            }
        }

        return result;
    }

    @Sensitive
    private String convertPayloadToString() throws CharacterCodingException {
        WsByteBuffer[] buffers = messageReader.getMessagePayload();
        int size = messageReader.getMessageCompletePayloadSize();
        byte result[] = new byte[size];

        int start = 0;
        int length = 0;
        // read all the data into one byte array
        for (WsByteBuffer buf : buffers) {
            length = buf.limit() - buf.position();
            buf.get(result, start, length);
            start += length;
        }

        String payload = "";
        if (result.length > 0) {
            payload = Utils.uTF8byteArrayToString(result);
        }

        return payload;
    }

    @Sensitive
    private String convertMessageToString() throws CharacterCodingException {
        String result = null;
        byte ba[] = convertPayloadToByteArray();

        // create a string from the byte array
        if (ba.length > 0) {
            result = Utils.uTF8byteArrayToString(ba);
        } else {
            result = "";
        }

        return result;
    }

    @Sensitive
    private ByteBuffer convertMessageToByteBuffer() {
        return convertMessageToByteBuffer(false);
    }

    @Sensitive
    private ByteBuffer convertMessageToByteBuffer(boolean controlFrame) {

        int size;
        WsByteBuffer[] buffers;
        ByteBuffer result = null;

        if (!controlFrame) {
            buffers = messageReader.getMessagePayload();
            size = messageReader.getMessageCompletePayloadSize();
        } else {
            buffers = messageReader.getMessagePayload_Control();
            size = messageReader.getMessageCompletePayloadSize_Control();
        }

        // allocate a buffer not from the pool, so it is exactly the right capacity
        result = ByteBuffer.allocate(size);

        // put all the payload buffers into this new buffer
        for (WsByteBuffer buf : buffers) {
            result.put(buf.getWrappedByteBuffer());
        }
        //flip the new buffer, so it will be readable for user code
        result.flip();

        return result;
    }

    private InputStream convertMessageToInputStream() {
        byte[] ba = convertPayloadToByteArray();

        ByteArrayInputStream is = new ByteArrayInputStream(ba);

        return is;
    }

    private Reader convertMessageToReader() throws CharacterCodingException {

        byte[] ba = convertPayloadToByteArray();

        // I don't think the decoder passed into input stream is as stringent with UTF-8
        // as this method below.. maybe consider calling this to validate, just seems expensive.
        // Utils.uTF8byteArrayToString(ba);

        ByteArrayInputStream is = new ByteArrayInputStream(ba);

        CharsetDecoder d = Utils.UTF8_CHARSET.newDecoder();
        InputStreamReader isr = new InputStreamReader(is, d);

        return isr;
    }

    private Class<?> getMessageHandlerConsumerClass(MessageHandler mh) {
        ArrayList<Type> interfaces = new ArrayList<Type>();
        Utils.getAllInterfaces(mh.getClass(), interfaces);
        Object ta[] = interfaces.toArray();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "ta[]: " + Arrays.toString(ta));
        }
        for (Object t : ta) {
            if (((Type) t) instanceof ParameterizedType) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, t + " is instanceof ParameterizedType");
                }
                ParameterizedType pt = (ParameterizedType) t;
                Type rawType = pt.getRawType();
                if (rawType instanceof Class) {
                    Class<?> clazz = (Class<?>) rawType;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "rawType: " + clazz.getName());
                    }
                    // make sure we are looking at the desired interface
                    if ((clazz.equals(MessageHandler.Whole.class)) || (clazz.equals(MessageHandler.Partial.class))) {
                        Type ta2[] = pt.getActualTypeArguments();
                        // should only be one actual type
                        if (ta2.length == 1) {
                            Type t2 = ta2[0];
                            if (t2 instanceof Class) {
                                Class<?> clazz2 = (Class<?>) t2;
                                if (tc.isDebugEnabled()) {
                                    Tr.debug(tc, "Found match: " + clazz2);
                                }
                                return clazz2;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    public void destroy(Exception e) {

        if (binaryDecoders != null) {
            destroyDecoders(binaryDecoders);
        }
        if (textDecoders != null) {
            destroyDecoders(textDecoders);
        }
        if (binaryStreamDecoders != null) {
            destroyDecoders(binaryStreamDecoders);
        }
        if (textStreamDecoders != null) {
            destroyDecoders(textStreamDecoders);
        }

    }

    private void destroyDecoders(Map<?, ArrayList<Decoder>> _decoders) {
        Iterator<?> it = _decoders.entrySet().iterator();
        while (it.hasNext()) {
            @SuppressWarnings("unchecked")
            Map.Entry<?, Decoder> entry = (Entry<?, Decoder>) it.next();
            ArrayList<Decoder> dcList = (ArrayList<Decoder>) entry.getValue();
            for (int i = 0; i < dcList.size(); i++) {
                dcList.get(i).destroy();
            }

            it.remove();
        }

    }

    public void addMessageHandler(MessageHandler handler) throws IllegalStateException {
        determineAndSetMessageHandler(null, handler);
    }

    public <T> void addMessageHandler(Class<T> clazz, Whole<T> handler) {
        determineAndSetMessageHandler(clazz, handler);
    }

    public <T> void addMessageHandler(Class<T> clazz, Partial<T> handler) {
        determineAndSetMessageHandler(clazz, handler);
    }

    public Set<MessageHandler> getMessageHandlers() {
        Set<MessageHandler> set = new HashSet<MessageHandler>();

        if (appTextMessageHandler != null) {
            set.add(appTextMessageHandler);
        } else if (appTextPartialMessageHandler != null) {
            set.add(appTextPartialMessageHandler);
        }

        if (appBinaryMessageHandler != null) {
            set.add(appBinaryMessageHandler);
        } else if (appBinaryPartialMessageHandler != null) {
            set.add(appBinaryPartialMessageHandler);
        }

        if (appPongMessageHandler != null) {
            set.add(appPongMessageHandler);
        }

        return set;
    }

    public void removeMessageHandler(MessageHandler handler) {

        if (handler == null) {
            return;
        }

        // can only remove it, if it is equal to the one we have
        if (handler.equals(appTextMessageHandler)) {
            appTextMessageHandler = null;
        } else if (handler.equals(appBinaryMessageHandler)) {
            appBinaryMessageHandler = null;
        } else if (handler.equals(appTextPartialMessageHandler)) {
            appTextPartialMessageHandler = null;
        } else if (handler.equals(appBinaryPartialMessageHandler)) {
            appBinaryPartialMessageHandler = null;
        } else if (handler.equals(appPongMessageHandler)) {
            appPongMessageHandler = null;
        }
    }

    public void cancelRead() {
        tcpReadContext.read(1, null, true, TCPRequestContext.IMMED_TIMEOUT);
    }

    private <T> DATA_TYPE determineAndSetMessageHandler(Class<T> cl, MessageHandler _mh) throws IllegalStateException, IllegalArgumentException {

        boolean isPartial = false;
        Class<?> clazz = null;

        if (cl == null) {
            clazz = getMessageHandlerConsumerClass(_mh);
        } else {
            clazz = cl;
        }

        if (MessageHandler.Partial.class.isAssignableFrom(_mh.getClass())) {
            isPartial = true;
        }

        // can not add a text message handler if there already is one
        if ((clazz.equals(String.class)) || (clazz.equals(Reader.class))) {
            if ((annotatedTextMethodPresent) || (appTextMessageHandler != null) || (appTextPartialMessageHandler != null)) {
                IllegalStateException x = new IllegalStateException();
                throw x;
            }

            if (isPartial) {
                appTextPartialMessageHandler = _mh;
                appTextPartialMessageHandlerClass = clazz;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "appTextPartialMessageHandler set for: " + _mh);
                }
            } else {
                appTextMessageHandler = _mh;
                appTextMessageHandlerClass = clazz;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "appTextMessageHandler set for: " + _mh);
                }
            }
            return DATA_TYPE.TEXT;
        }

        // can not add a binary message handler if there already is one
        if ((clazz.equals(ByteBuffer.class)) || (clazz.equals(byte[].class)) || (clazz.equals(InputStream.class))) {
            if ((annotatedBinaryMethodPresent) || (appBinaryMessageHandler != null) || (appBinaryPartialMessageHandler != null)) {
                IllegalStateException x = new IllegalStateException();
                throw x;
            }

            if (isPartial) {
                appBinaryPartialMessageHandler = _mh;
                appBinaryPartialMessageHandlerClass = clazz;
            } else {
                appBinaryMessageHandler = _mh;
                appBinaryMessageHandlerClass = clazz;
            }
            return DATA_TYPE.BINARY;
        }

        if (clazz.equals(PongMessage.class)) {
            // can not add a poing message handler if there already is one
            if ((annotatedPongMethodPresent) || (appPongMessageHandler != null)) {
                IllegalStateException x = new IllegalStateException();
                throw x;
            }
            appPongMessageHandler = _mh;
            appPongMessageHandlerClass = clazz;
            return DATA_TYPE.PONG;
        }

        DATA_TYPE dt = findDecoderTypeFromAppDecoderType(clazz);

        if (dt == DATA_TYPE.TEXT) {
            appTextMessageHandler = _mh;
            appTextMessageHandlerClass = clazz;
            return DATA_TYPE.TEXT;
        }

        if (dt == DATA_TYPE.BINARY) {
            appBinaryMessageHandler = _mh;
            appBinaryMessageHandlerClass = clazz;
            return DATA_TYPE.BINARY;
        }

        // could not find a match for this message handler, most common case would be a Partial MessageHandler that was not one of the valid values
        IllegalArgumentException iae = new IllegalArgumentException();
        throw iae;

    }

    private DATA_TYPE findDecoderTypeFromAppDecoderType(Class<?> typeToLookFor) {
        List<Class<? extends Decoder>> decoders = endpointConfig.getDecoders();

        // no decoders, that's easy
        if (decoders == null) {
            return null;
        }

        for (Class<? extends Decoder> decoder : decoders) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "looking at decoder: " + decoder);
            }
            // try to find a decoder with the same generic type as that of the type we are looking for.
            ArrayList<Type> interfaces = new ArrayList<Type>();
            Utils.getAllInterfaces(decoder, interfaces);
            Object ta[] = interfaces.toArray();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "ta[]: " + Arrays.toString(ta));
            }
            for (Object t : ta) {
                Class<?> clazz = Utils.getCodingClass((Type) t);
                if (clazz != null) {

                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "clazz " + clazz.getName());
                    }
                    if (clazz.equals(typeToLookFor)) {
                        if ((Decoder.Text.class.isAssignableFrom(decoder))
                            || (Decoder.TextStream.class.isAssignableFrom(decoder))) {

                            return DATA_TYPE.TEXT;
                        }
                        if ((Decoder.Binary.class.isAssignableFrom(decoder))
                            || (Decoder.BinaryStream.class.isAssignableFrom(decoder))) {
                            return DATA_TYPE.BINARY;
                        }
                    }
                }

            }
        }

        return null;
    }

    private Decoder findWillDecodeDecoder(ArrayList<Decoder> dcList, Object message) {
        Decoder decoder = null;
        boolean willDecode = false;
        for (int i = 0; i < dcList.size(); i++) {
            decoder = dcList.get(i);
            if (decoder instanceof Decoder.Text) {
                willDecode = ((Decoder.Text) decoder).willDecode((String) message);
                if (willDecode) {
                    break;
                }
            } else if (decoder instanceof Decoder.Binary) {
                willDecode = ((Decoder.Binary) decoder).willDecode((ByteBuffer) message);
                if (willDecode) {
                    break;
                }
            }
        }
        if (willDecode)
            return decoder;
        else
            return null;
    }

    public void resetReader() {
        if (messageReader != null) {
            messageReader.reset();
        }
    }

}
