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

import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;

public class MessageReader {

    private static final TraceComponent tc = Tr.register(MessageReader.class);

    /** starting size of the pending buffer array */
    private static final int BUFFER_ARRAY_INITIAL_SIZE = 10;
    /** starting minimum growth size of the pending buffer array */
    private static final int BUFFER_ARRAY_GROWTH_SIZE = 10;

    boolean shouldReadMaskedData = true;

    boolean needNewFrameProcessor = false;

    public enum FSeqState {

        EXPECTING_NEW,
        EXPECTING_PARTIAL_OR_LAST,
        FIRST_AND_LAST,
        FIRST_OF_MULTIPLE,
        MIDDLE_OF_MULTIPLE,
        LAST_OF_MULTIPLE
    }

    FrameReadProcessor frameProcessor = new FrameReadProcessor();
    FSeqState frameSequenceState = FSeqState.EXPECTING_NEW;

    WsByteBuffer nextMessageBuf = null;

    OpcodeType firstFrameOpcodeType = null;

    WsByteBuffer[] payloadBuffers = null;
    int payloadCountOfBuffers = 0;
    int messageCompletePayloadSize = 0;

    // since control messages can occur inside Read Message, we need separate buffers at the message layer
    WsByteBuffer[] payloadBuffers_Control = null;
    int payloadCountOfBuffers_Control = 0;
    int messageCompletePayloadSize_Control = 0;
    OpcodeType controlOpcodeType = null;

    FrameReadProcessor[] fpList = new FrameReadProcessor[BUFFER_ARRAY_INITIAL_SIZE];
    int countOfIOFrames = 0;

    WsocReadCallback callback = null;
    WsocConnLink connLink = null;

    public MessageReader() {}

    public void initialize(WsocReadCallback _cb, WsocConnLink _link, boolean _shouldReadMaskedData) {
        callback = _cb;
        connLink = _link;
        shouldReadMaskedData = _shouldReadMaskedData;
        frameProcessor.initialize(shouldReadMaskedData);
    }

    public void resetControlFrameParameters(boolean releaseBuffers) {
        payloadCountOfBuffers_Control = 0;
        messageCompletePayloadSize_Control = 0;

        if (releaseBuffers) {
            frameProcessor.releaseBuffers();
        }
    }

    public void reset() {

        if (nextMessageBuf == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "reset called - no left over buffers.");
            }
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "reset called - but will still process data left over for the next message.");
            }
        }

        resetControlFrameParameters(false);

        frameSequenceState = FSeqState.EXPECTING_NEW;
        firstFrameOpcodeType = null;
        payloadCountOfBuffers = 0;
        messageCompletePayloadSize = 0;

        if (countOfIOFrames > 0) {
            for (int i = 0; i < countOfIOFrames; i++) {
                if (fpList[i] != null) {
                    fpList[i].reset(true);
                }
            }
        } else {
            frameProcessor.reset(true);
        }

        fpList = new FrameReadProcessor[BUFFER_ARRAY_INITIAL_SIZE];
        countOfIOFrames = 0;

    }

    public void setFrameSequenceState(FSeqState x) {

        frameSequenceState = x;

    }

    public MessageReadInfo processRead(TCPReadRequestContext rrc, boolean txtPartialAvailable, boolean binaryPartialAvailable,
                                       boolean anticipatingCloseFrame) throws FrameFormatException, WsocBufferException, MaxMessageException {
        // return true if a full message has been read in, otherwise false

        WsByteBuffer currentBuf = null;
        if (rrc != null) {
            currentBuf = rrc.getBuffer();
        }
        if (currentBuf == null) {
            currentBuf = nextMessageBuf;
            nextMessageBuf = null;
        } else {
            currentBuf.flip();
        }
        int nextMessagePosition;
        FrameState frameState;

        if (needNewFrameProcessor) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "creating a new FrameReadProcessor");
            }
            frameProcessor = new FrameReadProcessor();
            frameProcessor.initialize(shouldReadMaskedData);
            needNewFrameProcessor = false;
        }

        nextMessagePosition = frameProcessor.processNextBuffer(currentBuf);
        frameState = frameProcessor.getFrameState();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "frame state is: " + frameState + " frameSequenceState is: " + frameSequenceState + " nextMessagePosition is: " + nextMessagePosition);
        }

        if (nextMessagePosition >= 0) {
            // the buffer contains the last part of the current message, and at least the start of the next,
            // so slice off the new buffer so it can be processed separately
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "slicing buffer, since this buffer contains end of old message and start of the next");
            }
            int oldPosition = currentBuf.position();
            currentBuf.position(nextMessagePosition);
            nextMessageBuf = currentBuf.slice();

            currentBuf.position(oldPosition);
            currentBuf.limit(nextMessagePosition);
        } else {
            // no new message is starting inside this buffer.
            nextMessageBuf = null;
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "frameSequenceState before: " + frameSequenceState);
        }

        if ((frameSequenceState == FSeqState.EXPECTING_NEW) || (frameSequenceState == FSeqState.EXPECTING_PARTIAL_OR_LAST)) {
            if ((frameState == FrameState.FIND_PAYLOAD) || (frameState == FrameState.PAYLOAD_COMPLETE)) {
                // need to verify format data is valid
                verifyAndSetFrameVariables(frameProcessor);
            }
        }

        processMaxMessageSize(frameProcessor.getPayloadLength());

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "frameSequenceState after: " + frameSequenceState);
        }

        // if we are reading for a close frame, and the message (control frame or not) being read is too big, then it is an error
        if ((anticipatingCloseFrame) && (frameProcessor.getPayloadLength() >= 126)) {
            MessageReadInfo mri = new MessageReadInfo(MessageReadInfo.State.CLOSE_FRAME_ERROR, firstFrameOpcodeType, (nextMessagePosition >= 0));
            return mri;
        }

        if (frameState == FrameState.PAYLOAD_COMPLETE) {
            // if we have read in all the payload for this frame, then unmask the data
            if (shouldReadMaskedData) {
                frameProcessor.unmaskPayload();
            }

            if (!frameProcessor.getControlFrame()) {
                // store this frameProcessor for later use, once all the frames for this message are obtained
                // grow the array if needed
                if (countOfIOFrames >= fpList.length) {
                    // need to grow the array
                    int originalSize = fpList.length;
                    FrameReadProcessor[] temp = new FrameReadProcessor[originalSize + BUFFER_ARRAY_GROWTH_SIZE];
                    System.arraycopy(fpList, 0, temp, 0, originalSize); // parameters are source to destination
                    fpList = temp;
                }

                fpList[countOfIOFrames] = frameProcessor;
                countOfIOFrames++;
                needNewFrameProcessor = true;

                if ((frameSequenceState == FSeqState.FIRST_AND_LAST) || (frameSequenceState == FSeqState.LAST_OF_MULTIPLE)) {
                    // if this is the last frame for this message, then get the array of byte buffers that hold the payload data for all the frames
                    gatherUpAllFramesAndPayload();
                    MessageReadInfo info = new MessageReadInfo(MessageReadInfo.State.COMPLETE, firstFrameOpcodeType, (nextMessagePosition >= 0));
                    return info;

                } else {
                    if (countOfIOFrames == 1) {
                        // Only gather up frames if partial method is available....
                        if ((firstFrameOpcodeType == OpcodeType.TEXT_WHOLE) && txtPartialAvailable) {
                            gatherUpAllFramesAndPayload();
                        } else if ((firstFrameOpcodeType == OpcodeType.BINARY_WHOLE) && binaryPartialAvailable) {
                            gatherUpAllFramesAndPayload();
                        }
                        MessageReadInfo info = new MessageReadInfo(MessageReadInfo.State.PARTIAL_COMPLETE, firstFrameOpcodeType, (nextMessagePosition >= 0));
                        return info;
                    }
                }

            } else {

                // we have a control frame inside an ongoing message.  Control Messages can not be fragmented, but consist of 1 frame
                gatherUpPayload_Control();
                if ((frameSequenceState != FSeqState.FIRST_AND_LAST) && (frameSequenceState != FSeqState.EXPECTING_NEW)) {
                    // this is a control frame inside another message
                    OpcodeType ot = frameProcessor.getControlOpcodeType();
                    MessageReadInfo info = new MessageReadInfo(MessageReadInfo.State.CONTROL_MESSAGE_EMBEDDED, ot, (nextMessagePosition >= 0));
                    needNewFrameProcessor = true;
                    return info;

                }

                MessageReadInfo info = new MessageReadInfo(MessageReadInfo.State.COMPLETE, firstFrameOpcodeType, (nextMessagePosition >= 0));
                needNewFrameProcessor = true;
                return info;
            }

        }

        // a complete message has not been read in
        MessageReadInfo info = new MessageReadInfo(MessageReadInfo.State.FRAME_INCOMPLETE, firstFrameOpcodeType, (nextMessagePosition >= 0));
        return info;
    }

    public void removeFirstFrameFromProcessor() {
        // if a partial message handler processed the first (and up to now the only) frame of a non-complete message, then it needs to be
        // removed from the frame list
        fpList[0].reset(true);
        countOfIOFrames = 0;
        payloadCountOfBuffers = 0;
        messageCompletePayloadSize = 0;
    }

    // This is never called, except in an exception case.  frame buffers should be cleaned up when reset is called on the FrameReadProcessor
    public void releaseBuffers() {
        // release buffers is desired
        if (payloadBuffers != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "release payload buffers.  number to release is: " + payloadCountOfBuffers);
            }
            // if the payload buffers have been set into here, then release those
            for (int i = 0; i < payloadCountOfBuffers; i++) {
                payloadBuffers[i].release();
            }
            // avoid double releasing by resetting these objects and variables
            payloadBuffers = null;
            payloadCountOfBuffers = 0;
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "release buffers from frame list.  number to release is: " + countOfIOFrames);
            }
            // payload buffers haven't been set into here yet, so release buffers on each frame processor
            if (countOfIOFrames > 0) {
                for (int i = 0; i < countOfIOFrames; i++) {
                    fpList[i].releaseBuffers();
                }
            } else {
                frameProcessor.releaseBuffers();
            }
        }
    }

    @Sensitive
    public WsByteBuffer[] getMessagePayload() {
        return payloadBuffers;
    }

    public int getMessageCompletePayloadSize() {
        return messageCompletePayloadSize;
    }

    @Sensitive
    public WsByteBuffer[] getMessagePayload_Control() {
        return payloadBuffers_Control;
    }

    public int getMessageCompletePayloadSize_Control() {
        return messageCompletePayloadSize_Control;
    }

    private WsByteBufferPoolManager getBufferManager() {
        return ServiceManager.getBufferPoolManager();
    }

    private void gatherUpPayload_Control() {

        int countOfBuffers = frameProcessor.getFrameBufferListSize();

        if (countOfBuffers > 0) {
            // need to concatenate the lists together
            payloadBuffers_Control = new WsByteBuffer[countOfBuffers];

            payloadCountOfBuffers_Control = 0;
            for (int j = 0; j < countOfBuffers; j++) {
                payloadBuffers_Control[payloadCountOfBuffers_Control] = frameProcessor.getBufferAtIndex(j);
                payloadCountOfBuffers_Control++;
            }

            // Special Debug. printOutBuffers(payloadBuffers);

            // Sum up the total number read in for later use
            // at this point, only payload data should be between position and limit.
            for (WsByteBuffer buf : payloadBuffers_Control) {
                if (buf != null) {
                    this.messageCompletePayloadSize_Control += (buf.limit() - buf.position());
                }
            }
        }
    }

    private void gatherUpAllFramesAndPayload() {

        int countOfBuffers = 0;

        // count how many buffers are in all the frames
        for (int i = 0; i < countOfIOFrames; i++) {
            countOfBuffers += fpList[i].getFrameBufferListSize();
        }

        if (countOfBuffers > 0) {
            // need to concatenate the lists together
            payloadBuffers = new WsByteBuffer[countOfBuffers];

            payloadCountOfBuffers = 0;
            for (int i = 0; i < countOfIOFrames; i++) {
                int size = fpList[i].getFrameBufferListSize();
                for (int j = 0; j < size; j++) {
                    payloadBuffers[payloadCountOfBuffers] = fpList[i].getBufferAtIndex(j);
                    payloadCountOfBuffers++;
                }
            }

            // Special Debug. printOutBuffers(payloadBuffers);

            // Sum up the total number read in for later use
            // at this point, only payload data should be between position and limit.
            for (WsByteBuffer buf : payloadBuffers) {
                if (buf != null) {
                    this.messageCompletePayloadSize += (buf.limit() - buf.position());
                }
            }

        }
    }

    private void verifyAndSetFrameVariables(FrameReadProcessor fp) throws FrameFormatException {

        boolean embeddedControlFrame = false;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "frameSequenceState upon entry is: " + frameSequenceState);
        }

        if ((fp.getControlFrame()
             && (frameSequenceState != FSeqState.FIRST_AND_LAST) && (frameSequenceState != FSeqState.EXPECTING_NEW))) {
            embeddedControlFrame = true;
        }

        if (!embeddedControlFrame) {
            // ---------- fin processing ----------
            byte fin = fp.getFin();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "fin is: " + fin);
            }
            if (fin == 1) {
                if (frameSequenceState == FSeqState.EXPECTING_NEW) {
                    // this message 1 frame only
                    frameSequenceState = FSeqState.FIRST_AND_LAST;

                } else if ((frameSequenceState == FSeqState.FIRST_OF_MULTIPLE) || (frameSequenceState == FSeqState.MIDDLE_OF_MULTIPLE)
                           || (frameSequenceState == FSeqState.EXPECTING_PARTIAL_OR_LAST)) {
                    // this is the last frame for this multi-frame message
                    frameSequenceState = FSeqState.LAST_OF_MULTIPLE;

                } else {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Exception occurred while reading an incoming WebSocket message in ServerEndpoint class " + connLink.getEndpoint().getClass().getName()
                                     + " because of an error processing FIN of 1.");
                    }

                    String msg = Tr.formatMessage(tc,
                                                  "fin1.processing.error",
                                                  connLink.getEndpoint().getClass().getName());
                    FrameFormatException ffe = new FrameFormatException(msg);
                    throw ffe;
                }

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "frameSequenceState is now: " + frameSequenceState);
                }

            } else {
                // fin is 0
                if (frameSequenceState == FSeqState.EXPECTING_NEW) {
                    // this frame is the first one in a multi-frame message
                    frameSequenceState = FSeqState.FIRST_OF_MULTIPLE;

                } else if (frameSequenceState == FSeqState.FIRST_OF_MULTIPLE) {
                    // this frame is the second frame of a multi-frame message
                    frameSequenceState = FSeqState.MIDDLE_OF_MULTIPLE;

                } else if ((frameSequenceState != FSeqState.MIDDLE_OF_MULTIPLE) && (frameSequenceState != FSeqState.EXPECTING_PARTIAL_OR_LAST)) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Exception occurred while reading an incoming WebSocket message in ServerEndpoint class " + connLink.getEndpoint().getClass().getName()
                                     + " because of an error processing FIN of 0.");
                    }
                    String msg = Tr.formatMessage(tc,
                                                  "fin0.processing.error",
                                                  connLink.getEndpoint().getClass().getName());
                    FrameFormatException ffe = new FrameFormatException(msg);
                    throw ffe;
                }
            }

            // ---------- opcode processing ----------
            byte opcode = fp.getOpcode();

            // if the opcode is between 3-7 or B-F then it is invalid
            if (((opcode >= 3) && (opcode <= 7)) || ((opcode >= 0x0B) && (opcode <= 0x0F))) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception occurred while reading an incoming WebSocket message in ServerEndpoint class " + connLink.getEndpoint().getClass().getName()
                                 + " because of an invalid opcode " + opcode + " in the message frame.");
                }
                String msg = Tr.formatMessage(tc,
                                              "invalid.opcode",
                                              connLink.getEndpoint().getClass().getName(), opcode);

                FrameFormatException ffe = new FrameFormatException(msg);
                throw ffe;
            }

            // if unfragmented message, then opcode can't be continue
            if ((opcode == 0) && (frameSequenceState == FSeqState.FIRST_AND_LAST)) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception occurred while reading an incoming WebSocket message in ServerEndpoint class " + connLink.getEndpoint().getClass().getName()
                                 + " because of an invalid continue opcode with unfragmented message.");
                }
                String msg = Tr.formatMessage(tc,
                                              "invalid.continue.opcode",
                                              connLink.getEndpoint().getClass().getName());
                FrameFormatException ffe = new FrameFormatException(msg);
                throw ffe;
            }

            // if opcode is not continue, then this must be the first frame of the message
            if ((opcode != 0) && (frameSequenceState != FSeqState.FIRST_AND_LAST) && (frameSequenceState != FSeqState.FIRST_OF_MULTIPLE)) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception occurred while reading an incoming WebSocket message in ServerEndpoint class " + connLink.getEndpoint().getClass().getName()
                                 + " because of an invalid non-zero opcode on non-first frame. opcode: " + opcode);
                }
                String msg = Tr.formatMessage(tc,
                                              "invalid.nonzero.opcode",
                                              connLink.getEndpoint().getClass().getName());
                FrameFormatException ffe = new FrameFormatException(msg);
                throw ffe;
            }

            // if this is the first frame, store the opcode type for later processing
            if (opcode == 0x01)
                firstFrameOpcodeType = OpcodeType.TEXT_WHOLE;
            else if (opcode == 0x02)
                firstFrameOpcodeType = OpcodeType.BINARY_WHOLE;
            else if (opcode == 0x08)
                firstFrameOpcodeType = OpcodeType.CONNECTION_CLOSE;
            else if (opcode == 0x09)
                firstFrameOpcodeType = OpcodeType.PING;
            else if (opcode == 0x0A)
                firstFrameOpcodeType = OpcodeType.PONG;

        }

        // ---------- mask processing ----------
        byte maskFlag = fp.getMaskFlag();

        // mask flag must be set on messages coming into the server, but not on messages coming into the client side, of the websocket api
        if ((maskFlag == 0x00) && (shouldReadMaskedData)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception occurred while reading an incoming WebSocket message in ServerEndpoint class " + connLink.getEndpoint().getClass().getName()
                             + " because the mask flag is not set correctly in the message frame.  maskFlag: 0x00 serverSide: true");
            }
            String msg = Tr.formatMessage(tc,
                                          "incorrect.maskflag",
                                          connLink.getEndpoint().getClass().getName());
            FrameFormatException ffe = new FrameFormatException(msg);
            throw ffe;
        } else if ((maskFlag == 0x01) && (!shouldReadMaskedData)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception occurred while reading an incoming WebSocket message in ServerEndpoint class " + connLink.getEndpoint().getClass().getName()
                             + " Mask Flag is not set to a correct value.  maskFlag: 0x01 serverSide: false");
            }
            String msg = Tr.formatMessage(tc,
                                          "invalid.maskflag.value",
                                          connLink.getEndpoint().getClass().getName());
            FrameFormatException ffe = new FrameFormatException(msg);
            throw ffe;
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "frameSequenceState upon exit is: " + frameSequenceState);
        }

    }

    /*
     * maxMessageSize attribute in @OnMessage: Specifies the maximum size of message in bytes that the method this annotates will be able to process, or -1 to indicate that there
     * is no maximum. The default is -1. This
     * attribute only applies when the annotation is used to process whole messages, not to those methods that process messages in parts or use a stream or reader parameter to
     * handle the incoming message. If the incoming whole message exceeds this limit, then the implementation generates an error and closes the connection using the reason that the
     * message was too big.
     */
    private void processMaxMessageSize(long payLoadSize) throws MaxMessageException {
        AnnotatedEndpoint ae = null;
        Long maxMessageSize = Constants.DEFAULT_MAX_MSG_SIZE;

        if (!(connLink.getEndpoint() instanceof AnnotatedEndpoint)) {
            if (firstFrameOpcodeType == OpcodeType.BINARY_WHOLE || firstFrameOpcodeType == OpcodeType.BINARY_PARTIAL_FIRST
                || firstFrameOpcodeType == OpcodeType.BINARY_PARTIAL_CONTINUATION || firstFrameOpcodeType == OpcodeType.BINARY_PARTIAL_LAST) {
                maxMessageSize = (long) connLink.getMaxBinaryMessageBufferSize();
            } else if (firstFrameOpcodeType == OpcodeType.TEXT_WHOLE || firstFrameOpcodeType == OpcodeType.TEXT_PARTIAL_FIRST
                       || firstFrameOpcodeType == OpcodeType.TEXT_PARTIAL_CONTINUATION || firstFrameOpcodeType == OpcodeType.TEXT_PARTIAL_LAST) {
                maxMessageSize = (long) connLink.getMaxTextMessageBufferSize();
            }

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "processMaxMessageSize: Not AE. payLoadSize passed in: " + payLoadSize + " maxMessageSize: " + maxMessageSize);
            }

            // TODO: will have to use English message for now, needs to be translated in the next release
            if ((maxMessageSize != -1) && (payLoadSize > maxMessageSize)) {
                // String reasonPhrase = Tr.formatMessage(tc, "invalid.message.toobig", "MessageHandler", payLoadSize, maxMessageSize, "onMessage");
                // also, this message needs be less than 123, the max for a control frame.
                String reasonPhrase = "Invalid incoming WebSocket message. Message is too big. Message size: " +
                                      payLoadSize + " but max message size for this Session is: " + maxMessageSize;
                throw new MaxMessageException(reasonPhrase);
            }
            return;
        } else {
            ae = (AnnotatedEndpoint) connLink.getEndpoint();

        }

        MethodData methodData = null;
        EndpointMethodHelper epMethodHelper = null;
        if (firstFrameOpcodeType == OpcodeType.BINARY_WHOLE) {
            epMethodHelper = ae.getOnMessageBinaryMethod();
        } else if (firstFrameOpcodeType == OpcodeType.TEXT_WHOLE) {
            epMethodHelper = ae.getOnMessageTextMethod();
        }
        if (epMethodHelper != null) {
            methodData = epMethodHelper.getMethodData();
        } else {
            return;
        }

        //get user defined maxMessageSize in @OnMessage annotation if there is one
        maxMessageSize = methodData.getMaxMessageSize();
        if (maxMessageSize == Constants.ANNOTATED_UNDEFINED_MAX_MSG_SIZE) {
            // if user did not define a max size with an annotation, get the size from the session
            if (firstFrameOpcodeType == OpcodeType.BINARY_WHOLE || firstFrameOpcodeType == OpcodeType.BINARY_PARTIAL_FIRST
                || firstFrameOpcodeType == OpcodeType.BINARY_PARTIAL_CONTINUATION || firstFrameOpcodeType == OpcodeType.BINARY_PARTIAL_LAST) {
                maxMessageSize = (long) connLink.getMaxBinaryMessageBufferSize();
            } else if (firstFrameOpcodeType == OpcodeType.TEXT_WHOLE || firstFrameOpcodeType == OpcodeType.TEXT_PARTIAL_FIRST
                       || firstFrameOpcodeType == OpcodeType.TEXT_PARTIAL_CONTINUATION || firstFrameOpcodeType == OpcodeType.TEXT_PARTIAL_LAST) {
                maxMessageSize = (long) connLink.getMaxTextMessageBufferSize();
            }
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "processMaxMessageSize: Is AE. payLoadSize passed in: " + payLoadSize + " maxMessageSize: " + maxMessageSize);
        }
        Class<?> inputType = methodData.getMessageType();

        //if message is in parts, don't check for maxMessageSize, per API doc
        if (inputType.equals(String.class) || inputType.equals(ByteBuffer.class) || inputType.equals(byte[].class)) { //if message is return in parts, don't check for maxMessageSize
            int booleanIndex = methodData.getMsgBooleanPairIndex();
            if (booleanIndex >= 0) {
                return;
            }
        } else if (inputType.equals(Reader.class)) { //if message is Reader type, don't check for maxMessageSize, per API doc
            return;
        } else if (inputType.equals(InputStream.class)) { //if message is InputStream type, don't check for maxMessageSize, per API doc
            return;
        }
        //if payload size is greater than maxMessageSize, throw MaxMessageException which calls onClose() method of ServerEndpoint config
        if ((maxMessageSize != -1) && (payLoadSize > maxMessageSize)) {
            if (firstFrameOpcodeType == OpcodeType.BINARY_WHOLE) {
                epMethodHelper = ae.getOnMessageBinaryMethod();
            } else if (firstFrameOpcodeType == OpcodeType.TEXT_WHOLE) {
                epMethodHelper = ae.getOnMessageTextMethod();
            }
            String reasonPhrase = Tr.formatMessage(tc,
                                                   "invalid.message.toobig",
                                                   epMethodHelper.getMethod().getDeclaringClass().getName(), payLoadSize, maxMessageSize,
                                                   epMethodHelper.getMethod().getName());

            throw new MaxMessageException(reasonPhrase);
        } else {
            return;
        }
    }
}
