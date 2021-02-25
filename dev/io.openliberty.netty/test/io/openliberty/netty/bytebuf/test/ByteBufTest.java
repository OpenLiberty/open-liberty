package io.openliberty.netty.bytebuf.test;

/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import test.common.SharedOutputManager;

/**
 * Briefly test access and basic working of underlying ByteBuf code from
 * io.openliberty.io.netty in the OpenLiberty context.
 */
public class ByteBufTest {
    /**  */
    private static final int DATA_SIZE = 256;
    /**  */
    private static final ByteBuf HEAP = Unpooled.buffer(DATA_SIZE);
    private static final ByteBuf DIRECT = UnpooledByteBufAllocator.DEFAULT.directBuffer(DATA_SIZE);
    private static final CompositeByteBuf COMP = Unpooled.compositeBuffer();

    private final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    /**
     * Test the various pooled buffers.
     * Test a non-pooled buffer.
     * Test the refcount impl.
     * Test the FileChannel wrapping buffer.
     * Run all the API tests against the input buffer.
     * Test indirect slices and backing array usage paths.
     * Test bytebuffer equals() method.
     */

    @Test
    public void heapBuffer() {
        ByteBuf b = HEAP;
        if (b.hasArray()) {
            byte[] array = b.array();
            assertEquals(array.length, DATA_SIZE);
        } else {
            fail("should have heap array");
        }
    }

    @Test
    public void directBuffer() {
        ByteBuf b = DIRECT;
        if (!b.hasArray()) {
            assertEquals(b.capacity(), DATA_SIZE);
        } else {
            fail("should not have heap array");
        }
    }

    @Test
    public void compositeBuffer() {
        ByteBuf b1 = HEAP;
        ByteBuf b2 = DIRECT;
        COMP.addComponents(b1, b2);
        COMP.writeZero(DATA_SIZE);
        assertEquals(COMP.capacity(), b1.capacity() + b2.capacity() - DATA_SIZE);
    }

}
