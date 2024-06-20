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

package com.ibm.ws.zos.channel.wola.internal.msg;

import java.nio.ByteBuffer;

import com.ibm.ws.zos.channel.wola.internal.ByteBufferVector;
import com.ibm.ws.zos.channel.wola.internal.natv.CodepageUtils;

/**
 * Represents a WOLA message.
 *
 * !!! NOTE !!!: The format of the WOLA message must be kept in sync with
 * com.ibm.zos.native/include/server_wola_message.h
 */
public class WolaMessage {

    /**
     * The size of the WOLA message header (sans context data and app data).
     */
    public static final int HeaderSize = 0x80;

    /**
     * WOLA message eyecatcher ('BBOAMSG ' in EBCDIC).
     */
    public static final long BBOAMSG_EYE = CodepageUtils.getEbcdicBytesAsLong("BBOAMSG "); // 0xc2c2d6c1d4e2c740L;

    /**
     * Offset to the eyecatcher field within the message.
     */
    public static final int EyeCatcherOffset = 0;

    /**
     * Offset to the amsgver field.
     */
    public static final int AmsgverOffset = 0x08;

    /**
     * Constant value for the amsgver field.
     */
    public static final short BBOAMSG_VERSION_2 = 2;

    /**
     * Offset to the messageType field.
     */
    public static final int MessageTypeOffset = 0x0a;

    /**
     * Constant values for the messageType field.
     */
    public static final int WOLA_MESSAGE_TYPE_REQUEST = 0;
    public static final int WOLA_MESSAGE_TYPE_RESPONSE = 1;

    /**
     * Offset to the totalMessageSize field within the message.
     * TotalMessageSize includes header, contexts, and data.
     */
    public static final int TotalMessageSizeOffset = 0x0c;

    /**
     * Offset to the flag word.
     */
    public static final int FlagWordOffset = 0x10;

    /**
     * Mask to extract the exception flag from the flag word.
     */
    public static final int FlagWordExceptionMask = 0x00000001;

    /**
     * Offset to the requestId field.
     */
    public static final int RequestIdOffset = 0x14;

    /**
     * Offset to the returnCode field in the message. This is reported
     * back to the client service.
     */
    public static final int ReturnCodeOffset = 0x18;

    /**
     * Offset to the reasonCode field in the message. This is reported
     * back to the client service.
     */
    public static final int ReasonCodeOffset = 0x1C;

    /**
     * Offset to dataAreaOffset field within the message.
     * This field gives the offset to the actual data area within the message
     * (which usually follows the context area). The data area contains the
     * request payload -- i.e. the app's request parms/response data.
     */
    public static final int DataAreaOffsetOffset = 0x20;

    /**
     * Offset to the dataAreaLength field within the message.
     * This field gives the length of the data area.
     */
    public static final int DataAreaLengthOffset = 0x24;

    /**
     * Offset to the contextAreaOffset field within the message.
     * This field gives the offset to the actual contextArea (which usually
     * immediately follows the WOLA message header).
     */
    public static final int ContextAreaOffsetOffset = 0x28;

    /**
     * Offset to the contextAreaLength field within the message.
     */
    public static final int ContextAreaLengthOffset = 0x2c;

    /**
     * Offset to the mvsUserId field (an 8-byte char field).
     */
    public static final int MvsUserIdOffset = 0x30;

    /**
     * Offset to the jcaConnectionId.managedConnectionId field.
     */
    public static final int JcaConnectionIdManagedConnectionIdOffset = 0x40;

    /**
     * A template message header used for building new WolaMessages.
     */
    private static byte[] headerTemplate;

    /**
     * The raw message data.
     */
    private final ByteBufferVector rawData;

    /**
     * The cached WolaMessageContextArea mapping object.
     */
    private WolaMessageContextArea wolaMessageContextArea;

    /**
     * The cached dataArea.
     */
    private byte[] dataArea;

    /**
     * CTOR.
     *
     * @param data
     */
    public WolaMessage(ByteBufferVector data) {
        this.rawData = data;
    }

    /**
     * CTOR. Constructs a new message using the default header template.
     */
    public WolaMessage() {
        this(new ByteBufferVector(getHeaderTemplate()));
    }

    /**
     * @return A read-only ByteBuffer containing header data that can be used to
     *         quickly prime a new WolaMessage.
     */
    protected static byte[] getHeaderTemplate() {

        if (headerTemplate == null) {
            ByteBuffer newTemplate = ByteBuffer.allocate(WolaMessage.HeaderSize);
            newTemplate.putLong(WolaMessage.EyeCatcherOffset, WolaMessage.BBOAMSG_EYE);
            newTemplate.putShort(WolaMessage.AmsgverOffset, WolaMessage.BBOAMSG_VERSION_2);
            newTemplate.putInt(WolaMessage.TotalMessageSizeOffset, WolaMessage.HeaderSize);

            headerTemplate = newTemplate.array(); // Don't assign until the template is built.
        }

        // Return a copy of the template.  Callers will set into the header and we don't want to
        // corrupt the original.
        byte[] headerTemplateCopy = new byte[headerTemplate.length];
        System.arraycopy(headerTemplate, 0, headerTemplateCopy, 0, headerTemplate.length);
        return headerTemplateCopy;
    }

    /**
     * @return the rawData backing buffer vector
     */
    protected ByteBufferVector getRawData() {
        return rawData;
    }

    /**
     * Parse and return the dataArea from this WOLA Message.
     *
     * Note: the data area is parsed once and cached here in an instance field.
     *
     * @return The data area
     *
     * @throws WolaMessageParseException if the context area is invalid.
     */
    public byte[] getDataArea() {

        if (dataArea == null) {

            int dataAreaOffset = rawData.getInt(WolaMessage.DataAreaOffsetOffset);
            int dataAreaLen = rawData.getInt(WolaMessage.DataAreaLengthOffset);

            // Make sure the message actually has a data area
            if (dataAreaOffset != 0 && dataAreaLen != 0) {
                byte[] localDataArea = new byte[dataAreaLen];
                rawData.get(dataAreaOffset, localDataArea);
                dataArea = localDataArea;
            } else {
                // No data area.  Return an empty buffer.
                dataArea = new byte[0];
            }
        }

        return dataArea;
    }

    /**
     * @return The service name in the service name context of this WOLA message
     *
     * @throws WolaMessageParseException if the context could not be parsed
     */
    public String getServiceName() throws WolaMessageParseException {

        WolaServiceNameContext context = (WolaServiceNameContext) getWolaMessageContext(WolaMessageContextId.BBOASNC_Identifier);

        return context.getServiceName();
    }

    /**
     * @return The service name in the service name context of tihs WOLA message, as EBCDIC bytes.
     *
     * @throws WolaMessageParseException if the context could not be parsed
     */
    public byte[] getServiceNameAsEBCDICBytes() throws WolaMessageParseException {

        WolaServiceNameContext context = (WolaServiceNameContext) getWolaMessageContext(WolaMessageContextId.BBOASNC_Identifier);

        return context.getServiceNameAsEBCDICBytes();
    }

    /**
     * @param contextId - The ID of the context to get
     *
     * @return the WolaMessageContext for the given ID.
     *
     * @throws WolaMessageParseException if the contexts could not be parsed
     */
    protected WolaMessageContext getWolaMessageContext(WolaMessageContextId contextId) throws WolaMessageParseException {

        WolaMessageContextArea contextArea = getWolaMessageContextArea();

        return (contextArea != null) ? contextArea.getWolaMessageContext(contextId) : null;
    }

    /**
     * Parse and return the WolaMessageContextArea for this WOLA Message.
     *
     * Note: the context area is parsed once and cached here in an instance field.
     *
     * @return The WolaMessageContextArea mapping object for this WOLA message.
     *
     * @throws WolaMessageParseException if the context area is invalid.
     */
    protected WolaMessageContextArea getWolaMessageContextArea() throws WolaMessageParseException {

        if (wolaMessageContextArea == null) {

            int contextAreaOffset = rawData.getInt(WolaMessage.ContextAreaOffsetOffset);
            int contextAreaLen = rawData.getInt(WolaMessage.ContextAreaLengthOffset);

            // Make sure the message actually has a context area
            if (contextAreaOffset != 0 && contextAreaLen != 0) {
                wolaMessageContextArea = new WolaMessageContextArea(rawData, contextAreaOffset, contextAreaLen);
            }
        }

        return wolaMessageContextArea;
    }

    /**
     * @return The mvsUserId field
     */
    public String getMvsUserId() {
        return rawData.getString(WolaMessage.MvsUserIdOffset, 8, CodepageUtils.EBCDIC).trim();
    }

    /**
     * @param mvsUserId The userId to put in the message
     *
     * @return this
     */
    public WolaMessage setMvsUserId(String mvsUserId) {
        rawData.putStringFieldPadded(WolaMessage.MvsUserIdOffset, 8, mvsUserId, CodepageUtils.EBCDIC);
        return this;
    }

    /**
     * @return The messageType field.
     */
    public int getMessageType() {
        return rawData.getUnsignedShort(WolaMessage.MessageTypeOffset);
    }

    /**
     * @return true if this WolaMessage represents a request (vs a response).
     */
    public boolean isRequest() {
        return getMessageType() == WolaMessage.WOLA_MESSAGE_TYPE_REQUEST;
    }

    /**
     * See if the exception flag is set on the message.
     *
     * @return true if the exception flag is set, false if not.
     */
    public boolean isException() {
        return (rawData.getInt(WolaMessage.FlagWordOffset) & WolaMessage.FlagWordExceptionMask) == WolaMessage.FlagWordExceptionMask;
    }

    /**
     * @param requestId - The requestId to set in the message
     *
     * @return this
     */
    public WolaMessage setRequestId(int requestId) {
        rawData.putInt(WolaMessage.RequestIdOffset, requestId);
        return this;
    }

    /**
     * @return the requestId.
     */
    public int getRequestId() {
        return rawData.getInt(WolaMessage.RequestIdOffset);
    }

    /**
     * @return String representation (ByteBufferVector data).
     */
    @Override
    public String toString() {
        return super.toString() + rawData.toString();
    }

    /**
     * Build a WOLA response message. The message is primed with this WolaMessage's
     * header data. The dataArea and contextArea are both null'ed out.
     *
     * @return a wola response message
     */
    public WolaMessage buildResponse() {

        byte[] header = new byte[WolaMessage.HeaderSize];

        // Copy over the request's message header
        rawData.get(0, header);

        // Null out context area and data area offsets
        ByteBufferVector bbv = new ByteBufferVector(header);
        bbv.putInt(WolaMessage.ContextAreaOffsetOffset, 0);
        bbv.putInt(WolaMessage.ContextAreaLengthOffset, 0);
        bbv.putInt(WolaMessage.DataAreaOffsetOffset, 0);
        bbv.putInt(WolaMessage.DataAreaLengthOffset, 0);

        // Set total length
        bbv.putInt(WolaMessage.TotalMessageSizeOffset, WolaMessage.HeaderSize);

        return new WolaMessage(bbv);
    }

    /**
     * Append a data area to this WolaMessage using the given responseBytes data.
     *
     * Note: the resposneBytes should be fully built before appending to
     * the WolaMessage. Changes made to the responseBytes *after* appending
     * will NOT be reflected in the WolaMessage.
     *
     * @param responseBytes - The app's response data
     *
     * @return this
     */
    public WolaMessage appendDataArea(byte[] responseBytes) {

        if (responseBytes != null && responseBytes.length > 0) {
            byte[] responseBytesCopy = new byte[responseBytes.length];
            System.arraycopy(responseBytes, 0, responseBytesCopy, 0, responseBytes.length);

            rawData.putInt(WolaMessage.DataAreaOffsetOffset, rawData.getLength());
            rawData.putInt(WolaMessage.DataAreaLengthOffset, responseBytes.length);

            rawData.append(responseBytesCopy);

            rawData.putInt(WolaMessage.TotalMessageSizeOffset, rawData.getLength());
        }

        return this;
    }

    /**
     * Append the given WolaMessageContextArea to this WolaMessage.
     *
     * Note: the WolaMessageContextArea should be fully built before appending to
     * the WolaMessage. Changes made to WolaMessageContextArea *after* appending
     * will NOT be reflected in the WolaMessage.
     *
     * @param wolaMessageContextArea - must not be null
     *
     * @throws WolaMessageParseException if the context area is badly formatted
     *
     * @return this
     */
    public WolaMessage appendContextArea(WolaMessageContextArea wolaMessageContextArea) throws WolaMessageParseException {

        if (this.wolaMessageContextArea != null) {
            throw new IllegalArgumentException("Cannot append context area.  A context area is already appended to this message");
        }

        // Verify the rawData is good.
        wolaMessageContextArea.verifyRawData();

        // Add it to the end of the message.
        rawData.putInt(WolaMessage.ContextAreaOffsetOffset, rawData.getLength());
        rawData.putInt(WolaMessage.ContextAreaLengthOffset, wolaMessageContextArea.getRawData().getLength());

        // Add all the WolaMessageContextArea's rawData buffers to the WolaMessage's rawData buffers.
        rawData.append(wolaMessageContextArea.getRawData().toByteArray());

        rawData.putInt(WolaMessage.TotalMessageSizeOffset, rawData.getLength());

        this.wolaMessageContextArea = wolaMessageContextArea;

        return this;
    }

    /**
     * Set the returnCode/reasonCode.
     *
     * @return this
     */
    public WolaMessage setReturnCodeReasonCode(int rc, int rsn) {

        rawData.putInt(WolaMessage.ReturnCodeOffset, rc);
        rawData.putInt(WolaMessage.ReasonCodeOffset, rsn);
        return this;
    }

    /**
     * @return the entire WolaMessage as a single ByteBuffer.
     */
    public ByteBuffer toByteBuffer() {
        return ByteBuffer.wrap(rawData.toByteArray());
    }

    /**
     * Note: The jcaConnectionId field is mapped in native as a 12 byte area
     * containing the 8-byte server stoken followed by the managedConnectionId (an int).
     * This method sets only the managedConnectionId portion of the field.
     *
     * @param jcaConnectionId
     *
     * @return this
     */
    public WolaMessage setJcaConnectionId(int jcaConnectionId) {
        rawData.putInt(WolaMessage.JcaConnectionIdManagedConnectionIdOffset, jcaConnectionId);
        return this;
    }

}