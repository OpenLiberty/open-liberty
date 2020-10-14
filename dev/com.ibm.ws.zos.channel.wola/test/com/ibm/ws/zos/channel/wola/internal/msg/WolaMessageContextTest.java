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
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.Test;

import com.ibm.ws.zos.channel.wola.internal.ByteBufferVector;

/**
 *
 */
public class WolaMessageContextTest {

    /**
     *
     */
    @Test
    public void testValidHeader() throws Exception {

        ByteBuffer bb1 = ByteBuffer.allocate(32);
        bb1.putInt(WolaMessageContext.ContextIdOffset, WolaMessageContextId.BBOASNC_Identifier.nativeValue);
        bb1.putInt(WolaMessageContext.ContextLenOffset, 16);

        WolaMessageContext.verifyRawData(new ByteBufferVector(bb1.array()), 0);
    }

    /**
     *
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidHeaderBadContextId() throws Exception {

        ByteBuffer bb1 = ByteBuffer.allocate(32);
        bb1.putInt(WolaMessageContext.ContextIdOffset, 0); // Bad context id.
        bb1.putInt(WolaMessageContext.ContextLenOffset, 16);

        WolaMessageContext.verifyRawData(new ByteBufferVector(bb1.array()), 0);
    }

    /**
     *
     */
    @Test(expected = WolaMessageParseException.class)
    public void testInvalidHeaderBadLength() throws Exception {

        ByteBuffer bb1 = ByteBuffer.allocate(32);
        bb1.putInt(WolaMessageContext.ContextIdOffset, WolaMessageContextId.BBOASNC_Identifier.nativeValue);
        bb1.putInt(WolaMessageContext.ContextLenOffset, 17); // Bad length - overflows buffer.

        WolaMessageContext.verifyRawData(new ByteBufferVector(bb1.array()), 0);
    }

    /**
     *
     */
    @Test(expected = WolaMessageParseException.class)
    public void testInvalidHeaderIncomplete() throws Exception {

        ByteBuffer bb1 = ByteBuffer.allocate(WolaMessageContext.HeaderSize - 1); // too small
        bb1.putInt(WolaMessageContext.ContextIdOffset, WolaMessageContextId.BBOASNC_Identifier.nativeValue);

        WolaMessageContext.verifyRawData(new ByteBufferVector(bb1.array()), 0);
    }

    /**
     * Implementation of a context.
     */
    private static class DummyContext extends WolaMessageContext {
        private final byte[] bytes;

        private DummyContext(ByteBufferVector bbv) {
            super(bbv, 0, bbv.getLength());
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
    public void testGetContextId() throws Exception {

        ByteBuffer bb1 = ByteBuffer.allocate(32);
        bb1.putInt(WolaMessageContext.ContextIdOffset, WolaMessageContextId.BBOASNC_Identifier.nativeValue);
        bb1.putInt(WolaMessageContext.ContextLenOffset, 16);

        WolaMessageContext context = new DummyContext(new ByteBufferVector(bb1.array()));
        assertEquals(WolaMessageContextId.BBOASNC_Identifier, context.getContextId());
    }

    /**
     *
     */
    @Test
    public void testGetRawData() throws Exception {

        ByteBuffer bb1 = ByteBuffer.allocate(32);
        bb1.putInt(WolaMessageContext.ContextIdOffset, WolaMessageContextId.BBOASNC_Identifier.nativeValue);
        bb1.putInt(WolaMessageContext.ContextLenOffset, 16);

        byte[] ba1 = bb1.array();
        ByteBufferVector bbv = new ByteBufferVector(ba1);
        WolaMessageContext context = new DummyContext(bbv);

        assertTrue(Arrays.equals(ba1, context.getBytes()));
    }
}
