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
package com.ibm.ws.zos.core.utils.internal;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.Test;

/**
 *
 */
public class DoubleGutterImplTest {

    /**
     *
     */
    @Test
    public void testZeroAddress() {

        String expected = "  data_address=00000000_00000000, data_length=8";
        String result = new DoubleGutterImpl().asDoubleGutter(0L, "blahblah".getBytes());
        assertEquals(expected, result);
    }

    /**
     *
     */
    @Test
    public void testNullByteArray() {

        String expected = "  data_address=00000000_00000001, data_length=0";
        String result = new DoubleGutterImpl().asDoubleGutter(1L, (byte[]) null);
        assertEquals(expected, result);
    }

    /**
     *
     */
    @Test
    public void testNullByteBuffer() {

        String expected = "  data_address=00000000_00000001, data_length=0";
        String result = new DoubleGutterImpl().asDoubleGutter(1L, (ByteBuffer) null);
        assertEquals(expected, result);
    }

    /**
     *
     */
    @Test
    public void testByteArray() {

        String expected = "  data_address=00000000_00000001, data_length=24" + "\n"
                          + "  +--------------------------------------------------------------------------+" + "\n"
                          + "  |OSet| A=0000000000000001 Length=0000018 |     EBCDIC     |     ASCII      |" + "\n"
                          + "  +----+-----------------------------------+----------------+----------------+" + "\n"
                          + "  |0000|626C6168 626C6168 626C6168 626C6168|.%/..%/..%/..%/.|blahblahblahblah|" + "\n"
                          + "  |0010|626C6168 626C6168                  |.%/..%/.        |blahblah        |" + "\n"
                          + "  +--------------------------------------------------------------------------+";

        String result = new DoubleGutterImpl().asDoubleGutter(1L, "blahblahblahblahblahblah".getBytes());
        assertEquals(expected, result);
    }

    /**
     *
     */
    @Test
    public void testByteBuffer() {

        String expected = "  data_address=00000000_00000001, data_length=8" + "\n"
                          + "  +--------------------------------------------------------------------------+" + "\n"
                          + "  |OSet| A=0000000000000001 Length=0000008 |     EBCDIC     |     ASCII      |" + "\n"
                          + "  +----+-----------------------------------+----------------+----------------+" + "\n"
                          + "  |0000|626C6168 626C6168                  |.%/..%/.        |blahblah        |" + "\n"
                          + "  +--------------------------------------------------------------------------+";

        ByteBuffer bb = ByteBuffer.wrap("blahblah".getBytes());
        assertEquals(0, bb.position());

        String result = new DoubleGutterImpl().asDoubleGutter(1L, bb);
        assertEquals(expected, result);

        assertEquals(0, bb.position()); // Verify the byteBuffer's position wasn't changed.
    }

    /**
     *
     */
    @Test
    public void testByteArrayAndByteBufferAreEquivalent() {

        byte[] bytes = "blahblahblah".getBytes();
        String result1 = new DoubleGutterImpl().asDoubleGutter(1L, bytes);
        String result2 = new DoubleGutterImpl().asDoubleGutter(1L, ByteBuffer.wrap(bytes));

        assertEquals(result1, result2);
    }

}
