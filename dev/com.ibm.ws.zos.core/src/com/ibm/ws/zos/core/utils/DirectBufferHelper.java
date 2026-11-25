/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.core.utils;

import java.nio.ByteBuffer;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * This class is intended to manage a set of overlapping 2GB {@code DirectByteBuffer}s mapped
 * on demand for each GB of native memory that is referenced.
 */
@Trivial
public interface DirectBufferHelper {

    /**
     * The number of bytes in 1 GB.
     */
    public static final long GIGABYTE = 1L << 30;

    /**
     * Get a {@code ByteBuffer} based at {@code address} of the specified
     * length. The buffer is returned with the platform's native byte order.
     * This method can be used as an alternative to <code>NewDirectByteBuffer</code>
     * calls in native to create a direct {@code ByteBuffer} to map an address.
     *
     * @param address the base of the buffer
     * @param length  the length of the buffer
     *
     * @return a direct {@code ByteBuffer} that maps the requested region
     */
    public ByteBuffer getSlice(long address, int length);

    /**
     * Read the value of the byte at the specified address.
     *
     * @param address the address of the byte
     *
     * @return the byte at the specified address
     */
    public byte get(long address);

    /**
     * Perform a bulk get of the data at the specified address into the
     * provided byte array. The length of the provided array determines
     * the number of bytes to be read.
     *
     * @param address the address of the data to read
     * @param dest    the target byte array
     *
     * @see #get(long, byte[], int, int)
     */
    public void get(long address, byte[] dest);

    /**
     * Perform a bulk read of the data at the specified address into the
     * provided byte array at the specified offset for the specified length.
     *
     * @param address the address of the data to read
     * @param dest    the target byte array
     * @param offset  the offset into the target byte array
     * @param length  the number of bytes to transfer
     */
    public void get(long address, byte[] dest, int offset, int length);

    /**
     * Get the value of the {@code char} at the specified address. The read
     * is done with the platform's native byte order.
     *
     * @param address the address of the {@code char} to read
     *
     * @return the value of the {@code char} at the specified address
     */
    public char getChar(long address);

    /**
     * Get the value of the {@code double} at the specified address. The read
     * is done with the platform's native byte order.
     *
     * @param address the address of the {@code double} to read
     *
     * @return the value of the {@code double} at the specified address
     */
    public double getDouble(long address);

    /**
     * Get the value of the {@code float} at the specified address. The read
     * is done with the platform's native byte order.
     *
     * @param address the address of the {@code float} to read
     *
     * @return the value of the {@code float} at the specified address
     */
    public float getFloat(long address);

    /**
     * Get the value of the {@code int} at the specified address. The read
     * is done with the platform's native byte order.
     *
     * @param address the address of the {@code int} to read
     *
     * @return the value of the {@code int} at the specified address
     */
    public int getInt(long address);

    /**
     * Get the value of the {@code long} at the specified address. The read
     * is done with the platform's native byte order.
     *
     * @param address the address of the {@code long} to read
     *
     * @return the value of the {@code long} at the specified address
     */
    public long getLong(long address);

    /**
     * Get the value of the {@code short} at the specified address. The read
     * is done with the platform's native byte order.
     *
     * @param address the address of the {@code short} to read
     *
     * @return the value of the {@code short} at the specified address
     */
    public short getShort(long address);

    /**
     * Get or create a direct {@code ByteBuffer} segment that is
     * based at the specified address.
     *
     * @param address the base address of the buffer
     *
     * @return the direct {@code ByteBuffer} mapping an area at the
     *         specified address
     */
    public ByteBuffer getSegment(long address);

}
