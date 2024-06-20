/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal.msg;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.nio.ByteBuffer;
import java.util.List;

import org.junit.Test;

import com.ibm.ws.zos.channel.wola.internal.ByteBufferVector;
import com.ibm.ws.zos.channel.wola.internal.natv.CodepageUtils;

/**
 *
 */
public class WolaMessageTest {

    /**
     *
     */
    @Test
    public void testNoArgCTOR() {
        WolaMessage wolaMessage = new WolaMessage();
        ByteBufferVector rawData = wolaMessage.getRawData();
        assertEquals(WolaMessage.BBOAMSG_EYE, rawData.getLong(0));

        wolaMessage.setRequestId(1);
        assertEquals(1, rawData.getInt(WolaMessage.RequestIdOffset));
        assertEquals(1, wolaMessage.getRequestId());

        // Create another WolaMessage and verify its data is independent of the first one.
        WolaMessage wolaMessage2 = new WolaMessage();
        ByteBufferVector rawData2 = wolaMessage2.getRawData();
        assertEquals(WolaMessage.BBOAMSG_EYE, rawData2.getLong(0));
        assertEquals(0, wolaMessage2.getRequestId());

        // Verify the first WolaMessage's data did not change.
        assertEquals(1, wolaMessage.getRequestId());
    }

    /**
     *
     */
    @Test
    public void testGetHeaderTemplate() {

        ByteBuffer bb = ByteBuffer.wrap(WolaMessage.getHeaderTemplate());
        assertEquals(WolaMessage.BBOAMSG_EYE, bb.getLong(WolaMessage.EyeCatcherOffset));
        assertEquals(WolaMessage.BBOAMSG_VERSION_2, bb.getShort(WolaMessage.AmsgverOffset));
        assertEquals(WolaMessage.HeaderSize, bb.getInt(WolaMessage.TotalMessageSizeOffset));
        assertEquals(WolaMessage.WOLA_MESSAGE_TYPE_REQUEST, bb.getInt(WolaMessage.MessageTypeOffset));
        assertEquals(0, bb.getInt(WolaMessage.RequestIdOffset));
    }

    /**
     *
     */
    @Test
    public void testGetAndSetRequestId() {
        WolaMessage wolaMessage = new WolaMessage();
        ByteBufferVector rawData = wolaMessage.getRawData();

        assertEquals(0, rawData.getInt(WolaMessage.RequestIdOffset));
        assertEquals(0, wolaMessage.getRequestId());
        wolaMessage.setRequestId(1);
        assertEquals(1, rawData.getInt(WolaMessage.RequestIdOffset));
        assertEquals(1, wolaMessage.getRequestId());
    }

    /**
     *
     */
    @Test
    public void testValidMessageGetServiceName() throws Exception {

        int serviceNameContextLen = 16; // size of WolaServiceNameContext data (4 for flags+name len, 12 for name)
        int fullMessageSize = WolaMessage.HeaderSize
                              + WolaMessageContextArea.HeaderSize
                              + WolaMessageContext.HeaderSize
                              + serviceNameContextLen;

        // Build the wola message header
        ByteBuffer bb1 = ByteBuffer.allocate(WolaMessage.HeaderSize);
        bb1.putLong(WolaMessage.EyeCatcherOffset, WolaMessage.BBOAMSG_EYE);
        bb1.putInt(WolaMessage.TotalMessageSizeOffset, fullMessageSize);
        bb1.putInt(WolaMessage.ContextAreaOffsetOffset, WolaMessage.HeaderSize);
        bb1.putInt(WolaMessage.ContextAreaLengthOffset, WolaMessageContextArea.HeaderSize + WolaMessageContext.HeaderSize + serviceNameContextLen);

        // Build the context area
        ByteBuffer bb2 = ByteBuffer.allocate(WolaMessageContextArea.HeaderSize);
        bb2.putLong(WolaMessageContextArea.EyeCatcherOffset, WolaMessageContextArea.EyeCatcher);
        bb2.putInt(WolaMessageContextArea.NumContextsOffset, 1);

        // Build a context
        ByteBuffer bb3 = ByteBuffer.allocate(WolaMessageContext.HeaderSize + serviceNameContextLen);
        bb3.position(WolaMessageContext.EyeCatcherOffset);
        bb3.put(WolaServiceNameContext.EyeCatcher);
        bb3.rewind();
        bb3.putInt(WolaMessageContext.ContextIdOffset, WolaMessageContextId.BBOASNC_Identifier.nativeValue);
        bb3.putInt(WolaMessageContext.ContextLenOffset, serviceNameContextLen);
        bb3.putShort(WolaServiceNameContext.NameLengthOffset, (short) 12);
        bb3.position(WolaServiceNameContext.NameOffset);
        bb3.put("abcdefghijkl".getBytes(CodepageUtils.EBCDIC));
        bb3.rewind();

        // Put 'em together
        WolaMessage wolaMessage = new WolaMessage(new ByteBufferVector(bb1.array()).append(bb2.array()).append(bb3.array()));
        WolaMessageContextArea wolaMessageContextArea = wolaMessage.getWolaMessageContextArea();

        assertNotNull(wolaMessageContextArea);

        List<WolaMessageContext> wolaMessageContexts = wolaMessageContextArea.getWolaMessageContexts();
        assertNotNull(wolaMessageContexts);
        assertEquals(1, wolaMessageContexts.size());

        WolaMessageContext context = wolaMessageContextArea.getWolaMessageContext(WolaMessageContextId.BBOASNC_Identifier);
        assertNotNull(context);
        assertEquals(WolaMessageContextId.BBOASNC_Identifier, context.getContextId());

        assertSame(context, wolaMessage.getWolaMessageContext(WolaMessageContextId.BBOASNC_Identifier));
        assertSame(wolaMessageContextArea, wolaMessage.getWolaMessageContextArea());

        assertEquals("abcdefghijkl", wolaMessage.getServiceName());
    }

    /**
     *
     */
    @Test
    public void testToByteBuffer() {
        // Build the wola message header
        ByteBuffer bb1 = ByteBuffer.allocate(WolaMessage.HeaderSize);
        bb1.putLong(WolaMessage.EyeCatcherOffset, WolaMessage.BBOAMSG_EYE);
        bb1.putInt(WolaMessage.TotalMessageSizeOffset, WolaMessage.HeaderSize);

        WolaMessage wolaMessage = new WolaMessage(new ByteBufferVector(bb1.array()));

        assertEquals(WolaMessage.HeaderSize, wolaMessage.getRawData().getInt(WolaMessage.TotalMessageSizeOffset));
        assertEquals(0, wolaMessage.getRawData().getInt(WolaMessage.DataAreaOffsetOffset));
        assertEquals(0, wolaMessage.getRawData().getInt(WolaMessage.DataAreaLengthOffset));

        // Append the data area
        byte[] dataArea = new byte[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j' };
        wolaMessage.appendDataArea(dataArea);

        int fullMessageLength = WolaMessage.HeaderSize + dataArea.length;

        assertEquals(fullMessageLength, wolaMessage.getRawData().getInt(WolaMessage.TotalMessageSizeOffset));
        assertEquals(WolaMessage.HeaderSize, wolaMessage.getRawData().getInt(WolaMessage.DataAreaOffsetOffset));
        assertEquals(dataArea.length, wolaMessage.getRawData().getInt(WolaMessage.DataAreaLengthOffset));

        // Convert to byte buffer
        ByteBuffer bb2 = wolaMessage.toByteBuffer();

        assertEquals(0, bb2.position());
        assertEquals(fullMessageLength, bb2.remaining());
        assertEquals(WolaMessage.BBOAMSG_EYE, bb2.getLong(0));
        assertEquals(fullMessageLength, bb2.getInt(WolaMessage.TotalMessageSizeOffset));
        assertEquals(dataArea[0], bb2.get(WolaMessage.HeaderSize));

        byte[] newDataArea = new byte[bb2.getInt(WolaMessage.DataAreaLengthOffset)];
        bb2.position(bb2.getInt(WolaMessage.DataAreaOffsetOffset));
        bb2.get(newDataArea);
        assertArrayEquals(dataArea, newDataArea);
    }

    /**
     *
     */
    @Test
    public void testBuildResponse() {

        // Build the wola message header + context area.
        int fullMessageSize = WolaMessage.HeaderSize + WolaMessageContextArea.HeaderSize;
        ByteBuffer bb1 = ByteBuffer.allocate(WolaMessage.HeaderSize);
        bb1.putLong(WolaMessage.EyeCatcherOffset, WolaMessage.BBOAMSG_EYE);
        bb1.putInt(WolaMessage.TotalMessageSizeOffset, fullMessageSize);
        bb1.putInt(WolaMessage.ContextAreaOffsetOffset, WolaMessage.HeaderSize);
        bb1.putInt(WolaMessage.ContextAreaLengthOffset, WolaMessageContextArea.HeaderSize + WolaMessageContext.HeaderSize);

        // Build the context area
        ByteBuffer bb2 = ByteBuffer.allocate(WolaMessageContextArea.HeaderSize);
        bb2.putLong(WolaMessageContextArea.EyeCatcherOffset, WolaMessageContextArea.EyeCatcher);
        bb2.putInt(WolaMessageContextArea.NumContextsOffset, 0);

        WolaMessage wolaMessage = new WolaMessage(new ByteBufferVector(bb1.array()).append(bb2.array()));

        WolaMessage wolaMessageResponse = wolaMessage.buildResponse();
        ByteBufferVector bbv = wolaMessageResponse.getRawData();

        assertEquals(WolaMessage.BBOAMSG_EYE, bbv.getLong(0));
        assertEquals(WolaMessage.HeaderSize, bbv.getInt(WolaMessage.TotalMessageSizeOffset));
        assertEquals(0, bbv.getInt(WolaMessage.ContextAreaOffsetOffset));
        assertEquals(0, bbv.getInt(WolaMessage.ContextAreaLengthOffset));
        assertEquals(0, bbv.getInt(WolaMessage.DataAreaOffsetOffset));
        assertEquals(0, bbv.getInt(WolaMessage.DataAreaLengthOffset));
    }

    /**
     *
     */
    @Test
    public void testAppendDataArea() {
        // Build the wola message header
        ByteBuffer bb1 = ByteBuffer.allocate(WolaMessage.HeaderSize);
        bb1.putLong(WolaMessage.EyeCatcherOffset, WolaMessage.BBOAMSG_EYE);
        bb1.putInt(WolaMessage.TotalMessageSizeOffset, WolaMessage.HeaderSize);

        WolaMessage wolaMessage = new WolaMessage(new ByteBufferVector(bb1.array()));

        assertEquals(WolaMessage.HeaderSize, wolaMessage.getRawData().getInt(WolaMessage.TotalMessageSizeOffset));
        assertEquals(0, wolaMessage.getRawData().getInt(WolaMessage.DataAreaOffsetOffset));
        assertEquals(0, wolaMessage.getRawData().getInt(WolaMessage.DataAreaLengthOffset));

        // Append the data area
        int dataAreaLength = 20;
        wolaMessage.appendDataArea(new byte[dataAreaLength]);

        assertEquals(WolaMessage.HeaderSize + dataAreaLength, wolaMessage.getRawData().getInt(WolaMessage.TotalMessageSizeOffset));
        assertEquals(WolaMessage.HeaderSize, wolaMessage.getRawData().getInt(WolaMessage.DataAreaOffsetOffset));
        assertEquals(dataAreaLength, wolaMessage.getRawData().getInt(WolaMessage.DataAreaLengthOffset));
    }

    /**
     *
     */
    @Test
    public void testSetReturnCodeReasonCode() {
        // Build the wola message header
        ByteBuffer bb1 = ByteBuffer.allocate(WolaMessage.HeaderSize);
        bb1.putLong(WolaMessage.EyeCatcherOffset, WolaMessage.BBOAMSG_EYE);
        bb1.putInt(WolaMessage.TotalMessageSizeOffset, WolaMessage.HeaderSize);

        WolaMessage wolaMessage = new WolaMessage(new ByteBufferVector(bb1.array()));

        assertEquals(WolaMessage.HeaderSize, wolaMessage.getRawData().getInt(WolaMessage.TotalMessageSizeOffset));
        assertEquals(0, wolaMessage.getRawData().getInt(WolaMessage.ReturnCodeOffset));
        assertEquals(0, wolaMessage.getRawData().getInt(WolaMessage.ReasonCodeOffset));

        wolaMessage.setReturnCodeReasonCode(8, 34);

        assertEquals(WolaMessage.HeaderSize, wolaMessage.getRawData().getInt(WolaMessage.TotalMessageSizeOffset));
        assertEquals(8, wolaMessage.getRawData().getInt(WolaMessage.ReturnCodeOffset));
        assertEquals(34, wolaMessage.getRawData().getInt(WolaMessage.ReasonCodeOffset));
    }

    /**
     *
     */
    @Test
    public void testGetDataArea() {
        // Build the wola message header
        ByteBuffer bb1 = ByteBuffer.allocate(WolaMessage.HeaderSize);
        bb1.putLong(WolaMessage.EyeCatcherOffset, WolaMessage.BBOAMSG_EYE);
        bb1.putInt(WolaMessage.TotalMessageSizeOffset, WolaMessage.HeaderSize);

        WolaMessage wolaMessage = new WolaMessage(new ByteBufferVector(bb1.array()));

        assertEquals(WolaMessage.HeaderSize, wolaMessage.getRawData().getInt(WolaMessage.TotalMessageSizeOffset));
        assertEquals(0, wolaMessage.getRawData().getInt(WolaMessage.DataAreaOffsetOffset));
        assertEquals(0, wolaMessage.getRawData().getInt(WolaMessage.DataAreaLengthOffset));

        // Append the data area
        byte[] dataArea = new byte[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j' };
        wolaMessage.appendDataArea(dataArea);

        int fullMessageLength = WolaMessage.HeaderSize + dataArea.length;

        assertEquals(fullMessageLength, wolaMessage.getRawData().getInt(WolaMessage.TotalMessageSizeOffset));
        assertEquals(WolaMessage.HeaderSize, wolaMessage.getRawData().getInt(WolaMessage.DataAreaOffsetOffset));
        assertEquals(dataArea.length, wolaMessage.getRawData().getInt(WolaMessage.DataAreaLengthOffset));

        byte[] dataAreaArray = wolaMessage.getDataArea();

        assertEquals(dataArea.length, dataAreaArray.length);
        assertArrayEquals(dataArea, dataAreaArray);

        assertSame(dataAreaArray, wolaMessage.getDataArea()); // Test caching
    }

    /**
     *
     */
    @Test
    public void testGetMvsUserId() throws Exception {
        final String mvsUserId = "MSTONE1";
        ByteBuffer bb = ByteBuffer.allocate(WolaMessage.HeaderSize);
        bb.position(WolaMessage.MvsUserIdOffset);
        bb.put((mvsUserId + "\0").getBytes(CodepageUtils.EBCDIC));
        bb.rewind();

        WolaMessage wolaMessage = new WolaMessage(new ByteBufferVector(bb.array()));

        assertEquals(mvsUserId, wolaMessage.getMvsUserId());
    }

    /**
     *
     */
    @Test
    public void testGetMvsUserIdPadded() throws Exception {
        final String mvsUserId = "MSTONE1";
        ByteBuffer bb = ByteBuffer.allocate(WolaMessage.HeaderSize);
        bb.position(WolaMessage.MvsUserIdOffset);
        bb.put((mvsUserId + " ").getBytes(CodepageUtils.EBCDIC));
        bb.rewind();

        WolaMessage wolaMessage = new WolaMessage(new ByteBufferVector(bb.array()));

        assertEquals(mvsUserId, wolaMessage.getMvsUserId());
    }

    /**
     *
     */
    @Test
    public void testSetMvsUserId() throws Exception {
        final String mvsUserId = "MSTONE1";
        WolaMessage wolaMessage = new WolaMessage();
        wolaMessage.setMvsUserId(mvsUserId);

        assertEquals(mvsUserId, wolaMessage.getMvsUserId());

        byte[] bytes = new byte[8];
        wolaMessage.getRawData().get(WolaMessage.MvsUserIdOffset, bytes);

        assertArrayEquals(CodepageUtils.getEbcdicBytesPadded(mvsUserId, 8), bytes);
    }

    /**
     *
     */
    @Test
    public void testSetJcaConnectionId() throws Exception {
        WolaMessage wolaMessage = new WolaMessage().setJcaConnectionId(5);

        assertEquals(5, wolaMessage.getRawData().getInt(WolaMessage.JcaConnectionIdManagedConnectionIdOffset));
    }

    /**
     *
     */
    @Test
    public void testDefaultCTOR() {
        WolaMessage wolaMessage = new WolaMessage();

        assertEquals(WolaMessage.HeaderSize, wolaMessage.getRawData().getInt(WolaMessage.TotalMessageSizeOffset));
        assertEquals(WolaMessage.BBOAMSG_EYE, wolaMessage.getRawData().getLong(WolaMessage.EyeCatcherOffset));
        assertEquals(WolaMessage.BBOAMSG_VERSION_2, wolaMessage.getRawData().getUnsignedShort(WolaMessage.AmsgverOffset));
        assertEquals(0, wolaMessage.getRawData().getInt(WolaMessage.ContextAreaOffsetOffset));
        assertEquals(0, wolaMessage.getRawData().getInt(WolaMessage.ContextAreaLengthOffset));
    }

    /**
     *
     */
    @Test
    public void testAppendContextArea() throws Exception {

        WolaMessage wolaMessage = new WolaMessage();

        assertEquals(0, wolaMessage.getRawData().getInt(WolaMessage.ContextAreaOffsetOffset));
        assertEquals(0, wolaMessage.getRawData().getInt(WolaMessage.ContextAreaLengthOffset));

        // Append the context area
        wolaMessage.appendContextArea(new WolaMessageContextArea());

        int totalMessageSize = WolaMessage.HeaderSize + WolaMessageContextArea.HeaderSize;

        assertEquals(totalMessageSize, wolaMessage.getRawData().getInt(WolaMessage.TotalMessageSizeOffset));
        assertEquals(WolaMessage.HeaderSize, wolaMessage.getRawData().getInt(WolaMessage.ContextAreaOffsetOffset));
        assertEquals(WolaMessageContextArea.HeaderSize, wolaMessage.getRawData().getInt(WolaMessage.ContextAreaLengthOffset));
    }

    /**
     *
     */
    @Test
    public void testAppendContextAreaAndDataArea() throws Exception {

        WolaMessage wolaMessage = new WolaMessage();

        // Initially the message does not contain context area or data area.
        assertEquals(0, wolaMessage.getRawData().getInt(WolaMessage.ContextAreaOffsetOffset));
        assertEquals(0, wolaMessage.getRawData().getInt(WolaMessage.ContextAreaLengthOffset));
        assertEquals(0, wolaMessage.getRawData().getInt(WolaMessage.DataAreaOffsetOffset));
        assertEquals(0, wolaMessage.getRawData().getInt(WolaMessage.DataAreaLengthOffset));

        // Append the context area
        wolaMessage.appendContextArea(new WolaMessageContextArea());

        // Append the data area
        int dataAreaLength = 20;
        wolaMessage.appendDataArea(new byte[dataAreaLength]);

        int totalMessageSize = WolaMessage.HeaderSize + WolaMessageContextArea.HeaderSize + dataAreaLength;

        assertEquals(totalMessageSize, wolaMessage.getRawData().getInt(WolaMessage.TotalMessageSizeOffset));
        assertEquals(WolaMessage.HeaderSize, wolaMessage.getRawData().getInt(WolaMessage.ContextAreaOffsetOffset));
        assertEquals(WolaMessageContextArea.HeaderSize, wolaMessage.getRawData().getInt(WolaMessage.ContextAreaLengthOffset));

        int dataAreaOffset = WolaMessage.HeaderSize + WolaMessageContextArea.HeaderSize;
        assertEquals(dataAreaOffset, wolaMessage.getRawData().getInt(WolaMessage.DataAreaOffsetOffset));
        assertEquals(dataAreaLength, wolaMessage.getRawData().getInt(WolaMessage.DataAreaLengthOffset));
    }

}
