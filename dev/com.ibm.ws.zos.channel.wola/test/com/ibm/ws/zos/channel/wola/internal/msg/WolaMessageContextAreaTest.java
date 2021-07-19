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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.List;

import org.junit.Test;

import com.ibm.ws.zos.channel.wola.internal.ByteBufferVector;
import com.ibm.ws.zos.channel.wola.internal.natv.CodepageUtils;

/**
 *
 */
public class WolaMessageContextAreaTest {

    /**
     *
     */
    @Test
    public void testValidHeader() throws Exception {

        ByteBuffer bb = ByteBuffer.allocate(512);
        bb.putLong(WolaMessageContextArea.EyeCatcherOffset, WolaMessageContextArea.EyeCatcher);
        bb.putInt(WolaMessageContextArea.NumContextsOffset, 0);

        WolaMessageContextArea wolaMessageContextArea = new WolaMessageContextArea(new ByteBufferVector(bb.array()), 0, bb.remaining());
        List<WolaMessageContext> contexts = wolaMessageContextArea.getWolaMessageContexts();

        assertNotNull(contexts);
        assertTrue(contexts.isEmpty());
    }

    /**
     *
     */
    @Test(expected = WolaMessageParseException.class)
    public void testInvalidHeaderBadEye() throws Exception {

        ByteBuffer bb = ByteBuffer.allocate(512);
        bb.putLong(WolaMessageContextArea.EyeCatcherOffset, WolaMessageContextArea.EyeCatcher + 1); // Bad eyecatcher

        new WolaMessageContextArea(new ByteBufferVector(bb.array()), 0, bb.remaining());
    }

    /**
     *
     */
    @Test(expected = WolaMessageParseException.class)
    public void testInvalidHeaderIncomplete() throws Exception {

        ByteBuffer bb = ByteBuffer.allocate(WolaMessageContextArea.HeaderSize - 1); // Incomplete header
        bb.putLong(WolaMessageContextArea.EyeCatcherOffset, WolaMessageContextArea.EyeCatcher); // good eyecatcher

        new WolaMessageContextArea(new ByteBufferVector(bb.array()), 0, bb.remaining());
    }

    /**
     *
     */
    @Test
    public void testGetWolaMessageContexts() throws Exception {

        // Create the context area header.  We only support deserializing one context type right now.
        ByteBuffer bb = ByteBuffer.allocate(512);
        bb.putLong(0, WolaMessageContextArea.EyeCatcher);
        bb.putInt(WolaMessageContextArea.NumContextsOffset, 1);

        // Create message context raw data
        ByteBuffer bb1 = ByteBuffer.allocate(32);
        bb1.putInt(WolaMessageContext.ContextIdOffset, WolaMessageContextId.BBOASNC_Identifier.nativeValue);
        bb1.putInt(WolaMessageContext.ContextLenOffset, 16);

        // Add the contexts after the area header
        bb.position(WolaMessageContextArea.HeaderSize);
        bb.put(bb1);

        // Must reset the buffer before processing it
        bb.rewind();

        WolaMessageContextArea wolaMessageContextArea = new WolaMessageContextArea(new ByteBufferVector(bb.array()), 0, bb.remaining());
        List<WolaMessageContext> contexts = wolaMessageContextArea.getWolaMessageContexts();

        assertNotNull(contexts);
        assertEquals(1, contexts.size());

        WolaMessageContext context = wolaMessageContextArea.getWolaMessageContext(WolaMessageContextId.BBOASNC_Identifier);
        assertNotNull(context);
        assertEquals(WolaMessageContextId.BBOASNC_Identifier, context.getContextId());

        context = wolaMessageContextArea.getWolaMessageContext(WolaMessageContextId.BBOACORC_Identifier);
        assertNull(context);

        // Test caching
        assertSame(wolaMessageContextArea.getWolaMessageContexts(), wolaMessageContextArea.getWolaMessageContexts());
        assertSame(wolaMessageContextArea.getWolaMessageContext(WolaMessageContextId.BBOASNC_Identifier),
                   wolaMessageContextArea.getWolaMessageContext(WolaMessageContextId.BBOASNC_Identifier));
    }

    /**
     * Test the default CTOR, which should create an empty context area.
     */
    @Test
    public void testWolaMessageContextAreaCTOR() throws Exception {

        WolaMessageContextArea wolaMessageContextArea = new WolaMessageContextArea();
        ByteBuffer bb = ByteBuffer.wrap(wolaMessageContextArea.getRawData().toByteArray());

        assertEquals(WolaMessageContextArea.EyeCatcher, bb.getLong(WolaMessageContextArea.EyeCatcherOffset));
        assertEquals(0, bb.getInt(WolaMessageContextArea.NumContextsOffset));
        assertEquals(WolaMessageContextArea.HeaderSize, bb.remaining());

        assertNull(wolaMessageContextArea.getWolaMessageContext(WolaMessageContextId.BBOACORC_Identifier));
    }

    /**
     *
     */
    @Test
    public void testAddWolaMessageContext() throws Exception {

        WolaMessageContextArea wolaMessageContextArea = new WolaMessageContextArea();

        String serviceName = "hello";
        wolaMessageContextArea.addWolaMessageContext(new WolaServiceNameContext(serviceName));

        // The contextAreaLen is equal to the context area headersize + the length of the
        // added context.
        int contextAreaLen = WolaMessageContextArea.HeaderSize + WolaServiceNameContext.NameOffset + serviceName.length();

        // The context begins just after the context area header.
        int serviceNameContextOffset = WolaMessageContextArea.HeaderSize;
        int serviceNameContextLen = WolaServiceNameContext.NameOffset + serviceName.length();

        ByteBuffer bb = ByteBuffer.wrap(wolaMessageContextArea.getRawData().toByteArray());

        // Verify the raw header data
        assertEquals(contextAreaLen, bb.remaining());
        assertEquals(WolaMessageContextArea.EyeCatcher, bb.getLong(WolaMessageContextArea.EyeCatcherOffset));
        assertEquals(1, bb.getInt(WolaMessageContextArea.NumContextsOffset));

        // Verify the raw context data
        assertEquals(ByteBuffer.wrap(WolaServiceNameContext.EyeCatcher).getLong(), bb.getLong(serviceNameContextOffset + WolaMessageContext.EyeCatcherOffset));
        assertEquals(WolaMessageContextId.BBOASNC_Identifier.nativeValue, bb.getInt(serviceNameContextOffset + WolaMessageContext.ContextIdOffset));

        // Note: the contextLen doesn't include the context header
        assertEquals(serviceNameContextLen - WolaMessageContext.HeaderSize, bb.getInt(serviceNameContextOffset + WolaMessageContext.ContextLenOffset));
        assertEquals(serviceName.length(), bb.getShort(serviceNameContextOffset + WolaServiceNameContext.NameLengthOffset));

        byte[] serviceNameBytes = new byte[serviceName.length()];
        ((ByteBuffer) bb.duplicate().position(serviceNameContextOffset + WolaServiceNameContext.NameOffset)).get(serviceNameBytes);
        assertEquals(serviceName, new String(serviceNameBytes, CodepageUtils.EBCDIC));

        assertNull(wolaMessageContextArea.getWolaMessageContext(WolaMessageContextId.BBOACORC_Identifier));
        assertNotNull(wolaMessageContextArea.getWolaMessageContext(WolaMessageContextId.BBOASNC_Identifier));
    }

    /**
     * Implementation of a context.
     */
    private static class DummyContext extends WolaMessageContext {
        private final byte[] bytes;

        private DummyContext(byte[] contextEye, WolaMessageContextId contextId) {
            super(contextEye, contextId);
            ByteBufferVector bbv = new ByteBufferVector(new byte[WolaMessageContext.HeaderSize]);
            fillHeader(bbv);
            bytes = bbv.toByteArray();
        }

        @Override
        byte[] getBytes() {
            return bytes;
        }

    };

    /**
     *
     */
    @Test
    public void testAddMultipleWolaMessageContext() throws Exception {

        WolaMessageContextArea wolaMessageContextArea = new WolaMessageContextArea();
        byte[] contextEye1 = new byte[] { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H' };
        byte[] contextEye2 = new byte[] { 'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I' };

        // Just add some dummy contexts.
        wolaMessageContextArea.addWolaMessageContext(new DummyContext(contextEye1, WolaMessageContextId.BBOATXC_Identifier));
        wolaMessageContextArea.addWolaMessageContext(new DummyContext(contextEye2, WolaMessageContextId.BBOACORC_Identifier));

        // The contextAreaLen is equal to the context area headersize + the length of the
        // added contexts.
        int contextAreaLen = WolaMessageContextArea.HeaderSize
                             + WolaMessageContext.HeaderSize
                             + WolaMessageContext.HeaderSize;

        ByteBuffer bb = ByteBuffer.wrap(wolaMessageContextArea.getRawData().toByteArray());

        // Verify the raw header data
        assertEquals(contextAreaLen, bb.remaining());
        assertEquals(WolaMessageContextArea.EyeCatcher, bb.getLong(WolaMessageContextArea.EyeCatcherOffset));
        assertEquals(2, bb.getInt(WolaMessageContextArea.NumContextsOffset));

        assertNotNull(wolaMessageContextArea.getWolaMessageContext(WolaMessageContextId.BBOATXC_Identifier));
        assertNotNull(wolaMessageContextArea.getWolaMessageContext(WolaMessageContextId.BBOACORC_Identifier));
        assertNull(wolaMessageContextArea.getWolaMessageContext(WolaMessageContextId.BBOASNC_Identifier));
    }

}
