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
package com.ibm.ws.zos.channel.wola.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import org.junit.Test;

import com.ibm.ws.zos.channel.wola.internal.msg.WolaMessage;
import com.ibm.ws.zos.channel.wola.internal.msg.WolaMessageParseException;

/**
 * Unit tests for WOLAMessageParser.
 *
 * WOLA message format, for reference.
 *
 * int[] rawData = new int[] {
 * 0xc2c2d6c1, // (0x00) - 'BBOA'
 * 0xd4e2c700, // (0x04) - 'MSG'0
 * 0x00000000, // (0x08) - amsgver (short), _rsvd1 (short)
 * 0x00000080, // (0x0c) - totalMessageSize
 * 0x00000000, // (0x10) - flagByte1 - flagByte4
 * 0x00000000, // (0x14) - requestType
 * 0x00000000, // (0x18) - returnCode
 * 0x00000000, // (0x1c) - reasonCode
 * 0x00000000, // (0x20) - dataAreaOffset
 * 0x00000000, // (0x24) - dataAreaLength
 * 0x00000000, // (0x28) - contextAreaOffset
 * 0x00000000, // (0x2c) - contextAreaLength
 * 0x00000000, // (0x30) - MVS user ID (byte[8])
 * 0x00000000, // (0x34) - MVS user ID
 * 0x00000000, // (0x38) - JCA connection ID (struct bboastoid - byte[12])
 * 0x00000000, // (0x3c) - JCA connection ID
 * 0x00000000, // (0x40) - JCA connection ID
 * 0x00000000, // (0x44) - WOLA name part 2 (byte[8])
 * 0x00000000, // (0x48) - WOLA name part 2
 * 0x00000000, // (0x4c) - WOLA name part 3 (byte[8])
 * 0x00000000, // (0x50) - WOLA name part 3
 * 0x00000000, // (0x54) - work type
 * 0x00000000, // (0x58) - reserved (byte[40])
 * 0x00000000, // (0x5c) - reserved
 * 0x00000000, // (0x60) - reserved
 * 0x00000000, // (0x64) - reserved
 * 0x00000000, // (0x68) - reserved
 * 0x00000000, // (0x6c) - reserved
 * 0x00000000, // (0x70) - reserved
 * 0x00000000, // (0x74) - reserved
 * 0x00000000, // (0x78) - reserved
 * 0x00000000 // (0x7c) - reserved
 * };
 *
 */
public class WOLAMessageParserTest {

    /**
     * Test complete headers.
     */
    @Test
    public void testIsHeaderCompleteYes() {

        // Complete header in a single buffer.
        ByteBuffer wolaHeader = ByteBuffer.allocate(WolaMessage.HeaderSize);
        wolaHeader.putLong(WolaMessage.EyeCatcherOffset, WolaMessage.BBOAMSG_EYE);

        assertTrue(new WOLAMessageParser(null, wolaHeader).isHeaderComplete());

        // Complete header across multiple buffers
        final ByteBuffer wolaHeader1 = ByteBuffer.allocate(WolaMessage.HeaderSize - 20);
        final ByteBuffer wolaHeader2 = ByteBuffer.allocate(10);
        ByteBuffer wolaHeader3 = ByteBuffer.allocate(10);
        wolaHeader1.putLong(WolaMessage.EyeCatcherOffset, WolaMessage.BBOAMSG_EYE);
        WOLAMessageLeftovers prevData = new WOLAMessageLeftovers(new ByteBufferVector(wolaHeader1.array()).append(wolaHeader2.array()), false, false);

        assertTrue(new WOLAMessageParser(prevData, wolaHeader3).isHeaderComplete());
    }

    /**
     * Test incomplete headers.
     */
    @Test
    public void testIsHeaderCompleteNo() {

        // Incomplete header in a single buffer
        ByteBuffer wolaHeader = ByteBuffer.allocate(WolaMessage.HeaderSize - 1);
        wolaHeader.putLong(WolaMessage.EyeCatcherOffset, WolaMessage.BBOAMSG_EYE);

        assertFalse(new WOLAMessageParser(null, wolaHeader).isHeaderComplete());

        // Incomplete header across multiple buffers
        final ByteBuffer wolaHeader1 = ByteBuffer.allocate(WolaMessage.HeaderSize - 20);
        final ByteBuffer wolaHeader2 = ByteBuffer.allocate(5);
        ByteBuffer wolaHeaderIncomplete3 = ByteBuffer.allocate(5);
        wolaHeader1.putLong(WolaMessage.EyeCatcherOffset, WolaMessage.BBOAMSG_EYE);
        WOLAMessageLeftovers prevDataIncomplete = new WOLAMessageLeftovers(new ByteBufferVector(wolaHeader1.array()).append(wolaHeader2.array()), false, false);

        assertFalse(new WOLAMessageParser(prevDataIncomplete, wolaHeaderIncomplete3).isHeaderComplete());
    }

    /**
     * Test invalid eyecatcher.
     */
    @Test(expected = WolaMessageParseException.class)
    public void testInvalidEyecatcher() throws Exception {

        ByteBuffer wolaHeader = ByteBuffer.allocate(WolaMessage.HeaderSize);
        wolaHeader.putLong(WolaMessage.EyeCatcherOffset, 0x0102030405060708L);

        new WOLAMessageParser(null, wolaHeader).isMessageComplete();
    }

    /**
     * Test a complete message.
     */
    @Test
    public void testIsMessageCompleteYes() throws Exception {

        int totalMessageSize = WolaMessage.HeaderSize + 50;

        // Complete message in a single buffer.
        ByteBuffer wolaHeader = ByteBuffer.allocate(totalMessageSize);
        wolaHeader.putLong(WolaMessage.EyeCatcherOffset, WolaMessage.BBOAMSG_EYE);
        wolaHeader.putInt(WolaMessage.TotalMessageSizeOffset, totalMessageSize);

        assertTrue(new WOLAMessageParser(null, wolaHeader).isMessageComplete());

        // Complete message across multiple buffers
        final ByteBuffer wolaHeader1 = ByteBuffer.allocate(WolaMessage.HeaderSize);
        final ByteBuffer wolaHeader2 = ByteBuffer.allocate(totalMessageSize - WolaMessage.HeaderSize);
        wolaHeader1.putLong(WolaMessage.EyeCatcherOffset, WolaMessage.BBOAMSG_EYE);
        wolaHeader1.putInt(WolaMessage.TotalMessageSizeOffset, totalMessageSize);

        assertTrue(new WOLAMessageParser(new WOLAMessageLeftovers(new ByteBufferVector(wolaHeader1.array()), false, false), wolaHeader2).isMessageComplete());
    }

    /**
     * Test incomplete message.
     */
    @Test
    public void testIsMessageCompleteNo() throws Exception {

        int totalMessageSize = WolaMessage.HeaderSize + 50;

        // Incomplete message in a single buffer
        ByteBuffer wolaHeader = ByteBuffer.allocate(WolaMessage.HeaderSize);
        wolaHeader.putLong(WolaMessage.EyeCatcherOffset, WolaMessage.BBOAMSG_EYE);
        wolaHeader.putInt(WolaMessage.TotalMessageSizeOffset, totalMessageSize);

        assertFalse(new WOLAMessageParser(null, wolaHeader).isMessageComplete());

        // Incomplete message across multiple buffers
        final ByteBuffer wolaHeader1 = ByteBuffer.allocate(WolaMessage.HeaderSize);
        final ByteBuffer wolaHeader2 = ByteBuffer.allocate(totalMessageSize - WolaMessage.HeaderSize - 10);
        wolaHeader1.putLong(WolaMessage.EyeCatcherOffset, WolaMessage.BBOAMSG_EYE);
        wolaHeader1.putInt(WolaMessage.TotalMessageSizeOffset, totalMessageSize);

        assertFalse(new WOLAMessageParser(new WOLAMessageLeftovers(new ByteBufferVector(wolaHeader1.array()), false, false), wolaHeader2).isMessageComplete());
    }

    /**
     * Test parseMessage
     */
    @Test
    public void testParseMessage() throws Exception {

        int totalMessageSize = WolaMessage.HeaderSize + 50;

        // Complete message in a single buffer.
        ByteBuffer wolaMessage = ByteBuffer.allocate(totalMessageSize);
        wolaMessage.putLong(WolaMessage.EyeCatcherOffset, WolaMessage.BBOAMSG_EYE);
        wolaMessage.putInt(WolaMessage.TotalMessageSizeOffset, totalMessageSize);

        WOLAMessageParser wolaMessageParser = new WOLAMessageParser(null, wolaMessage);

        assertNotNull(wolaMessageParser.parseMessage());
        assertNull(wolaMessageParser.getLeftovers());
    }

    /**
     * Test parseMessage with an incomplete message.
     */
    @Test
    public void testParseMessageIncompleteMessage() throws Exception {

        int totalMessageSize = WolaMessage.HeaderSize + 50;

        // Incomplete message in a single buffer.
        ByteBuffer wolaHeader = ByteBuffer.allocate(WolaMessage.HeaderSize);
        wolaHeader.putLong(WolaMessage.EyeCatcherOffset, WolaMessage.BBOAMSG_EYE);
        wolaHeader.putInt(WolaMessage.TotalMessageSizeOffset, totalMessageSize);

        WOLAMessageParser wolaMessageParser = new WOLAMessageParser(null, wolaHeader);

        assertNull(wolaMessageParser.parseMessage());
        assertNotNull(wolaMessageParser.getLeftovers());
        assertEquals(WolaMessage.HeaderSize, wolaMessageParser.getLeftovers().getByteBuffers().getLength());
    }

    /**
     * Test parseMessage with invalid eyecatcher.
     */
    @Test(expected = WolaMessageParseException.class)
    public void testParseMessageInvalidEyecatcher() throws Exception {

        ByteBuffer wolaHeader = ByteBuffer.allocate(WolaMessage.HeaderSize);
        wolaHeader.putLong(WolaMessage.EyeCatcherOffset, 0x0102030405060708L);

        WOLAMessageParser wolaMessageParser = new WOLAMessageParser(null, wolaHeader);
        wolaMessageParser.parseMessage();
    }

    /**
     * Test parseMessage with a complete message + some leftovers.
     * Note: I don't think this is a valid WOLA scenario, but test it anyway.
     */
    @Test
    public void testParseMessageCompleteMessageWithLeftovers() throws Exception {

        int totalMessageSize = WolaMessage.HeaderSize + 50;

        // Complete message in a single buffer.
        ByteBuffer wolaMessage = ByteBuffer.allocate(totalMessageSize + 10);
        wolaMessage.putLong(WolaMessage.EyeCatcherOffset, WolaMessage.BBOAMSG_EYE);
        wolaMessage.putInt(WolaMessage.TotalMessageSizeOffset, totalMessageSize);

        WOLAMessageParser wolaMessageParser = new WOLAMessageParser(null, wolaMessage);

        assertNotNull(wolaMessageParser.parseMessage());
        assertNotNull(wolaMessageParser.getLeftovers());
        assertEquals(10, wolaMessageParser.getLeftovers().getByteBuffers().getLength());
    }

    /**
     * Test multiple calls to parseMessage, passing along leftover data from previous calls.
     */
    @Test
    public void testParseMessageWithLeftovers() throws Exception {

        int totalMessageSize = WolaMessage.HeaderSize + 50;

        // First read returns only the header.
        ByteBuffer wolaMessage1 = ByteBuffer.allocate(WolaMessage.HeaderSize);
        wolaMessage1.putLong(WolaMessage.EyeCatcherOffset, WolaMessage.BBOAMSG_EYE);
        wolaMessage1.putInt(WolaMessage.TotalMessageSizeOffset, totalMessageSize);

        WOLAMessageParser wolaMessageParser1 = new WOLAMessageParser(null, wolaMessage1);

        // Nothing to parse yet.
        assertNull(wolaMessageParser1.parseMessage());

        // Second read returns the message body
        ByteBuffer wolaMessage2 = ByteBuffer.allocate(totalMessageSize - WolaMessage.HeaderSize);

        // Pass in leftovers from first read.
        WOLAMessageParser wolaMessageParser2 = new WOLAMessageParser(wolaMessageParser1.getLeftovers(), wolaMessage2);

        assertNotNull(wolaMessageParser2.parseMessage());
        assertNull(wolaMessageParser2.getLeftovers());
    }

}