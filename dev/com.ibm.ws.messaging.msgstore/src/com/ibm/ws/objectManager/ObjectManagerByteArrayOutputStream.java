package com.ibm.ws.objectManager;

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

import com.ibm.ws.objectManager.utils.Trace;
import com.ibm.ws.objectManager.utils.Tracing;

/**
 * @author Andrew_Banks
 * 
 *         Allows direct acess to the buffer without copying it.
 */
class ObjectManagerByteArrayOutputStream
                extends java.io.ByteArrayOutputStream
{
    private static final Class cclass = ObjectManagerByteArrayOutputStream.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_LOG);

    // The number of bytes to release when this Object has been written to its ObjectStore. 
    private int releaseSize;

    ObjectManagerByteArrayOutputStream()
    {
        super();
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this,
                        cclass,
                        "<init>");
            trace.exit(this,
                       cclass,
                       "<init>");
        }
    } // ObjectManagerByteArrayOutputStream().

    ObjectManagerByteArrayOutputStream(int size) {
        super(size);
        final String methodName = "<init>";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { new Integer(size) });
            trace.exit(this,
                       cclass,
                       methodName);
        }
    } // ObjectManagerByteArrayOutputStream().

    /*
     * @returns the underlying buffer.
     */
    byte[] getBuffer()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this,
                        cclass,
                        "getBuffer");
            trace.exit(this,
                       cclass,
                       "getBuffer",
                       "return length="
                                       + buf.length);
        }
        return buf;
    } // Method getBuffer.

    /*
     * @returns the count of bytes currently written to the underlying buffer.
     */
    int getCount()
    {
        return count;
    } // getCount().

    /**
     * @param releaseSize The number of bytes to lease when the Object has been stored.
     */
    final void setReleaseSize(int releaseSize) {
        this.releaseSize = releaseSize;
    }

    /**
     * @return int The number of bytes to lease when the Object has been stored.
     */
    final int getReleaseSize() {
        return releaseSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.ByteArrayOutputStream#reset()
     */
    public void reset() {
        super.reset();
        releaseSize = 0;
    } // reset().

    /**
     * Writes an <code>int</code> to the byte array as four
     * bytes, high byte first. If no exception is thrown, the counter
     * <code>written</code> is incremented by <code>4</code>.
     * 
     * @param value to be written.
     * @see java.io.DataOutput#writeInt(int)
     */
    final void writeInt(int value)
    {
        byte writeBuffer[] = new byte[4];
        writeBuffer[0] = (byte) (value >>> 24);
        writeBuffer[1] = (byte) (value >>> 16);
        writeBuffer[2] = (byte) (value >>> 8);
        writeBuffer[3] = (byte) (value >>> 0);
        write(writeBuffer, 0, 4);
    } // writeInt().

    /**
     * Writes a <code>long</code> to the byte array as eight
     * bytes, high byte first. In no exception is thrown, the counter
     * <code>written</code> is incremented by <code>8</code>.
     * 
     * @param value to be written.
     * @see java.io.DataOutput#writeLong(long)
     */
    final void writeLong(long value)
    {
        byte writeBuffer[] = new byte[8];
        writeBuffer[0] = (byte) (value >>> 56);
        writeBuffer[1] = (byte) (value >>> 48);
        writeBuffer[2] = (byte) (value >>> 40);
        writeBuffer[3] = (byte) (value >>> 32);
        writeBuffer[4] = (byte) (value >>> 24);
        writeBuffer[5] = (byte) (value >>> 16);
        writeBuffer[6] = (byte) (value >>> 8);
        writeBuffer[7] = (byte) (value >>> 0);
        write(writeBuffer, 0, 8);
    } // writeLong().

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return new String("ObjectManagerByteArrayOutputStream"
                          + "/" + count
                          + "/" + Integer.toHexString(hashCode()));
    } // toString().
} // class ObjectManagerByteArrayOutputStream.
