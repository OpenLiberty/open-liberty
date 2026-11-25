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

package com.ibm.ws.zos.channel.wola.internal.natv;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Common super-class for various objects whose data is stored in a backing byte array.
 * This class provides common methods, such as reading/writing from/to the array.
 *
 * Objects whose data is populated in native code, e.g. RegistryToken, use this class.
 *
 */
public class ByteBufferBacked<T extends ByteBufferBacked> {

    /**
     * The raw byte data.
     */
    private byte[] rawData = null;

    /**
     * A ByteBuffer wrapped around the rawData.
     */
    private ByteBuffer rawDataBuffer = null;

    /**
     * CTOR.
     *
     * @param rawDataLength - The length of the backing byte array.
     */
    public ByteBufferBacked(int rawDataLength) {
        rawData = new byte[rawDataLength];
        rawDataBuffer = ByteBuffer.wrap(rawData);
    }

    /**
     * @return the backing byte array.
     */
    public byte[] getBytes() {
        return rawData;
    }

    /**
     * Put the given long into the rawData buffer at the given index.
     *
     * @return this
     */
    public T putLong(int index, long value) {
        rawDataBuffer.putLong(index, value);
        return (T) this;
    }

    /**
     * @return the long at the given index in the rawData buffer.
     */
    public long getLong(int index) {
        return rawDataBuffer.getLong(index);
    }

    /**
     * @return the int at the given index.
     */
    public int getInt(int index) {
        return rawDataBuffer.getInt(index);
    }

    /**
     * @return the int at the given index, as a hex string.
     */
    @Trivial
    public String getIntAsHexString(int index) {
        return String.format("%1$08x", rawDataBuffer.getInt(index));
    }

    /**
     * @return true if the rawData for the two objects matches, byte for byte; false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof ByteBufferBacked) {
            ByteBufferBacked that = (ByteBufferBacked) o;
            return Arrays.equals(this.rawData, that.rawData);
        }
        return false;
    }

    /**
     * Note: subclasses should override this method if their objects may be
     * inserted into a hash map.
     *
     * @return An arbitrary constant (42).
     */
    @Override
    public int hashCode() {
        return 42;
    }

    /**
     * @return Stringified backing array in hex.
     */
    @Override
    public String toString() {

        StringBuffer sb = new StringBuffer(this.getClass().getName() + ":x");

        // Note: current impl assumes rawData.length is a multiple of 4.
        //       If not, then some leftover bytes at the end won't get printed.
        for (int i = 0; i <= rawData.length - 4; i += 4) {
            sb.append(getIntAsHexString(i) + ".");
        }

        return sb.toString();
    }
}