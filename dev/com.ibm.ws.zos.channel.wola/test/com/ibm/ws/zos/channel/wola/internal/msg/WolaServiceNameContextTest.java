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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.Test;

import com.ibm.ws.zos.channel.wola.internal.ByteBufferVector;
import com.ibm.ws.zos.channel.wola.internal.natv.CodepageUtils;

/**
 *
 */
public class WolaServiceNameContextTest {

    private void putEyeCatcher(ByteBuffer bb, byte[] eyeCatcher) {
        int oldPosition = bb.position();
        bb.position(WolaMessageContext.EyeCatcherOffset);
        bb.put(eyeCatcher);
        bb.position(oldPosition);
    }

    /**
     * This should really be a generic context test.
     */
    @Test
    public void testValidHeader() throws Exception {

        ByteBuffer bb1 = ByteBuffer.allocate(32);
        putEyeCatcher(bb1, WolaServiceNameContext.EyeCatcher);
        bb1.putInt(WolaMessageContext.ContextIdOffset, WolaMessageContextId.BBOASNC_Identifier.nativeValue);
        bb1.putInt(WolaMessageContext.ContextLenOffset, 16);

        WolaMessageContext.verifyRawData(new ByteBufferVector(bb1.array()), 0);
    }

    /**
     *
     */
    @Test(expected = WolaMessageParseException.class)
    public void testInvalidHeaderIncomplete() throws Exception {

        ByteBuffer bb1 = ByteBuffer.allocate(19); // bad
        putEyeCatcher(bb1, WolaServiceNameContext.EyeCatcher);
        bb1.putInt(WolaMessageContext.ContextIdOffset, WolaMessageContextId.BBOASNC_Identifier.nativeValue);
        bb1.putInt(WolaMessageContext.ContextLenOffset, 4);

        new WolaServiceNameContext(new ByteBufferVector(bb1.array()), 0, bb1.remaining());
    }

    /**
     *
     */
    @Test(expected = WolaMessageParseException.class)
    public void testInvalidHeaderBadNameLength() throws Exception {

        ByteBuffer bb1 = ByteBuffer.allocate(32);
        putEyeCatcher(bb1, WolaServiceNameContext.EyeCatcher);
        bb1.putInt(WolaMessageContext.ContextIdOffset, WolaMessageContextId.BBOASNC_Identifier.nativeValue);
        bb1.putInt(WolaMessageContext.ContextLenOffset, 16);
        bb1.putShort(WolaServiceNameContext.NameLengthOffset, (short) 20); // bad, overflows buffer

        new WolaServiceNameContext(new ByteBufferVector(bb1.array()), 0, bb1.remaining());
    }

    /**
     *
     */
    @Test
    public void testGetServiceName() throws Exception {

        ByteBuffer bb1 = ByteBuffer.allocate(32);
        putEyeCatcher(bb1, WolaServiceNameContext.EyeCatcher);
        bb1.putInt(WolaMessageContext.ContextIdOffset, WolaMessageContextId.BBOASNC_Identifier.nativeValue);
        bb1.putInt(WolaMessageContext.ContextLenOffset, 16);
        bb1.putShort(WolaServiceNameContext.NameLengthOffset, (short) 12);

        bb1.position(WolaServiceNameContext.NameOffset);
        bb1.put(new byte[] { (byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84, // a, b, c, d (EBCDIC)
                             (byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84, // a, b, c, d
                             (byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84 }); // a, b, c, d
        bb1.rewind();

        WolaServiceNameContext wolaServiceNameContext = new WolaServiceNameContext(new ByteBufferVector(bb1.array()), 0, bb1.remaining());

        assertEquals("abcdabcdabcd", wolaServiceNameContext.getServiceName());
        assertSame(wolaServiceNameContext.getServiceName(), wolaServiceNameContext.getServiceName()); // test caching.
    }

    /**
     *
     */
    @Test
    public void testGetServiceNameBytes() throws Exception {

        ByteBuffer bb1 = ByteBuffer.allocate(32);
        putEyeCatcher(bb1, WolaServiceNameContext.EyeCatcher);
        bb1.putInt(WolaMessageContext.ContextIdOffset, WolaMessageContextId.BBOASNC_Identifier.nativeValue);
        bb1.putInt(WolaMessageContext.ContextLenOffset, 16);
        bb1.putShort(WolaServiceNameContext.NameLengthOffset, (short) 12);

        bb1.position(WolaServiceNameContext.NameOffset);
        byte[] serviceNameBytes = new byte[] { (byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84, // a, b, c, d (EBCDIC)
                                               (byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84, // a, b, c, d
                                               (byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84 }; // a, b, c, d
        bb1.put(serviceNameBytes);
        bb1.rewind();

        WolaServiceNameContext wolaServiceNameContext = new WolaServiceNameContext(new ByteBufferVector(bb1.array()), 0, bb1.remaining());

        assertTrue(Arrays.equals(serviceNameBytes, wolaServiceNameContext.getServiceNameAsEBCDICBytes()));
        assertSame(wolaServiceNameContext.getServiceNameAsEBCDICBytes(), wolaServiceNameContext.getServiceNameAsEBCDICBytes()); // test caching.
    }

    /**
     *
     */
    @Test
    public void testServiceNameCTOR() throws Exception {

        String serviceName = "blahblah";
        WolaServiceNameContext wolaServiceNameContext = new WolaServiceNameContext(serviceName);

        int serviceNameContextLen = WolaServiceNameContext.NameOffset + serviceName.length();

        ByteBuffer bb = ByteBuffer.wrap(wolaServiceNameContext.getBytes());
        assertEquals(ByteBuffer.wrap(WolaServiceNameContext.EyeCatcher).getLong(), bb.getLong(WolaMessageContext.EyeCatcherOffset));
        assertEquals(WolaMessageContextId.BBOASNC_Identifier.nativeValue, bb.getInt(WolaMessageContext.ContextIdOffset));

        // Note: the contextLen doesn't include the context header
        assertEquals(serviceNameContextLen - WolaMessageContext.HeaderSize, bb.getInt(WolaMessageContext.ContextLenOffset));
        assertEquals(serviceName.length(), bb.getShort(WolaServiceNameContext.NameLengthOffset));

        byte[] serviceNameBytes = new byte[serviceName.length()];
        ((ByteBuffer) bb.duplicate().position(WolaServiceNameContext.NameOffset)).get(serviceNameBytes);
        assertEquals(serviceName, new String(serviceNameBytes, CodepageUtils.EBCDIC));
    }

    /**
     *
     */
    @Test
    public void testServiceNameMaxLength() throws Exception {

        String serviceName = "xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx."
                             + "xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx."
                             + "xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx."
                             + "xxxxxxxxx.xxxxxx";

        assertEquals(256, serviceName.length());

        WolaServiceNameContext wolaServiceNameContext = new WolaServiceNameContext(serviceName);
        int serviceNameContextLen = WolaServiceNameContext.NameOffset + serviceName.length();

        ByteBuffer bb = ByteBuffer.wrap(wolaServiceNameContext.getBytes());
        assertEquals(ByteBuffer.wrap(WolaServiceNameContext.EyeCatcher).getLong(), bb.getLong(WolaMessageContext.EyeCatcherOffset));
        assertEquals(WolaMessageContextId.BBOASNC_Identifier.nativeValue, bb.getInt(WolaMessageContext.ContextIdOffset));

        // Note: the contextLen doesn't include the context header
        assertEquals(serviceNameContextLen - WolaMessageContext.HeaderSize, bb.getInt(WolaMessageContext.ContextLenOffset));
        assertEquals(serviceName.length(), bb.getShort(WolaServiceNameContext.NameLengthOffset));

        byte[] serviceNameBytes = new byte[serviceName.length()];
        ((ByteBuffer) bb.duplicate().position(WolaServiceNameContext.NameOffset)).get(serviceNameBytes);
        assertEquals(serviceName, new String(serviceNameBytes, CodepageUtils.EBCDIC));
    }

    /**
     *
     */
    @Test(expected = IllegalArgumentException.class)
    public void testServiceNameTooLong() throws Exception {

        String serviceName = "xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx."
                             + "xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx."
                             + "xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx.xxxxxxxxx."
                             + "xxxxxxxxx.xxxxxxx";

        assertEquals(257, serviceName.length());

        new WolaServiceNameContext(serviceName);
    }

}
