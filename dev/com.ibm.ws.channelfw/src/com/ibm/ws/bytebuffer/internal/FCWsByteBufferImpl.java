/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.bytebuffer.internal;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.FileChannel.MapMode;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;

/**
 * ByteBuffer representation wrapping a FileChannel.
 */
public class FCWsByteBufferImpl extends WsByteBufferImpl {

    private static final long serialVersionUID = 4029999822511452951L;

    private static final String CLASS_NAME = FCWsByteBufferImpl.class.getName();

    private int status = 0;
    private int fcLimit = 0;
    private int fcSize = 0;
    private FileChannel fc = null;

    private static final TraceComponent tc = Tr.register(FCWsByteBufferImpl.class,
                                                         MessageConstants.WSBB_TRACE_NAME,
                                                         MessageConstants.WSBB_BUNDLE);

    /**
     * Constructor.
     */
    public FCWsByteBufferImpl() {
        // default constructor for Externalizable
    }

    /**
     * Check whether this FC buffer is still enabled for the FileChannel usage.
     * 
     * @return boolean
     */
    private final boolean isFCEnabled() {
        return ((status & WsByteBuffer.STATUS_TRANSFER_TO) != 0);
    }

    /**
     * Constructor.
     * 
     * @param _fc the filechannel being wrapped
     */
    public FCWsByteBufferImpl(FileChannel _fc) {
        this.fc = _fc;
        this.status = WsByteBuffer.STATUS_TRANSFER_TO;

        try {
            this.fcLimit = (int) fc.size();
            this.fcSize = fcLimit;
        } catch (IOException ioe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "got IOException in FCWsByteBufferImpl: " + ioe);
            }
            FFDCFilter.processException(ioe, CLASS_NAME + ".ctor(FileChannel)", "57", this);
            throw new RuntimeException(ioe);
        }
        this.oByteBuffer = null;
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#getType()
     */
    @Override
    public int getType() {
        return WsByteBuffer.TYPE_FCWsByteBuffer;
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#getStatus()
     */
    @Override
    public int getStatus() {
        return this.status;
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#setStatus(int)
     */
    @Override
    public void setStatus(int value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setStatus(int) : " + value);
        }

        // check if the buffer should be converted before setting the status
        if (value == STATUS_BUFFER) {
            convertBufferIfNeeded();
        }
        this.status = value;
    }

    /**
     * Return the FileChannel object that is representing this WsByteBufferImpl.
     * 
     * @return FileChannel
     */
    public FileChannel getFileChannel() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getFileChannel(): " + fc);
        }
        return this.fc;
    }

    /**
     * If the buffer has not already been converted from a TRANSFER_TO buffer back
     * to the more common base BUFFER, then do so now.
     * 
     */
    private void convertBufferIfNeeded() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "convertBufferIfNeeded status: " + status);
        }

        if (isFCEnabled()) {

            // TRANSFER_TO status is currently on, so turn if OFF
            status = status & (~WsByteBuffer.STATUS_TRANSFER_TO);
            // and turn on BUFFER status
            status = status | WsByteBuffer.STATUS_BUFFER;

            try {
                // save so we can restore the current position
                int bufPosition = (int) fc.position();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "creating a MappedByteBuffer from the FileChannel. position: " + 0 + "  size: " + fc.size());
                }

                // map entire FileChannel buffer to ByteBuffer
                try {
                    oByteBuffer = fc.map(MapMode.PRIVATE, 0, fc.size());
                } catch (NonWritableChannelException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "FileChannel is readonly");
                    }
                    oByteBuffer = fc.map(MapMode.READ_ONLY, 0, fc.size());
                    setReadOnly(true);
                }

                // The mapped byte buffer returned by this method will have a
                // position of zero and a limit and capacity of size;
                // its mark will be undefined.
                // The buffer and the mapping that it represents will
                // remain valid until the buffer itself is garbage-collected.

                // restore the position and limit
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "set MappedByteBuffer position to: " + bufPosition + " limit to: " + fcLimit);
                }
                position(bufPosition);
                limit(fcLimit);
            } catch (IOException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "got IOException: " + e);
                }
                FFDCFilter.processException(e, CLASS_NAME + ".convertBufferIfNeeded", "112", this);
                throw new RuntimeException(e);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "convertBufferIfNeeded status: " + status);
        }
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#release()
     */
    @Override
    public void release() {
        // once this buffer is done, close the FileChannel that it wrapped
        // to avoid conflicts later
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Closing FileChannel: " + this.fc);
            }
            this.fc.close();
        } catch (IOException ioe) {
            // no ffdc required
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Error closing filechannel: " + ioe);
            }
        }
        super.release();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#array()
     */
    @Override
    public byte[] array() {
        convertBufferIfNeeded();
        return super.array();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#arrayOffset()
     */
    @Override
    public int arrayOffset() {
        convertBufferIfNeeded();
        return super.arrayOffset();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#capacity()
     */
    @Override
    public int capacity() {
        if (isFCEnabled()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "FileChannel capacity: " + this.fcSize);
            }
            return this.fcSize;
        }
        return super.capacity();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#clear()
     */
    @Override
    public WsByteBuffer clear() {
        convertBufferIfNeeded();
        return super.clear();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#compact()
     */
    @Override
    public WsByteBuffer compact() {
        convertBufferIfNeeded();
        return super.compact();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Object obj) {
        convertBufferIfNeeded();
        return super.compareTo(((WsByteBuffer) obj).getWrappedByteBufferNonSafe());
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#duplicate()
     */
    @Override
    public WsByteBuffer duplicate() {
        convertBufferIfNeeded();
        return super.duplicate();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#flip()
     */
    @Override
    public WsByteBuffer flip() {
        convertBufferIfNeeded();
        return super.flip();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#get()
     */
    @Override
    public byte get() {
        convertBufferIfNeeded();
        return super.get();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#get(byte[])
     */
    @Override
    public WsByteBuffer get(byte[] dst) {
        convertBufferIfNeeded();
        return super.get(dst);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#get(byte[], int, int)
     */
    @Override
    public WsByteBuffer get(byte[] dst, int offset, int length) {
        convertBufferIfNeeded();
        return super.get(dst, offset, length);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#get(int)
     */
    @Override
    public byte get(int index) {
        convertBufferIfNeeded();
        return super.get(index);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#getChar()
     */
    @Override
    public char getChar() {
        convertBufferIfNeeded();
        return super.getChar();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#getChar(int)
     */
    @Override
    public char getChar(int index) {
        convertBufferIfNeeded();
        return super.getChar(index);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#getDouble()
     */
    @Override
    public double getDouble() {
        convertBufferIfNeeded();
        return super.getDouble();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#getDouble(int)
     */
    @Override
    public double getDouble(int index) {
        convertBufferIfNeeded();
        return super.getDouble(index);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#getFloat()
     */
    @Override
    public float getFloat() {
        convertBufferIfNeeded();
        return super.getFloat();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#getFloat(int)
     */
    @Override
    public float getFloat(int index) {
        convertBufferIfNeeded();
        return super.getFloat(index);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#getInt()
     */
    @Override
    public int getInt() {
        convertBufferIfNeeded();
        return super.getInt();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#getInt(int)
     */
    @Override
    public int getInt(int index) {
        convertBufferIfNeeded();
        return super.getInt(index);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#getLong()
     */
    @Override
    public long getLong() {
        convertBufferIfNeeded();
        return super.getLong();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#getLong(int)
     */
    @Override
    public long getLong(int index) {
        convertBufferIfNeeded();
        return super.getLong(index);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#getShort()
     */
    @Override
    public short getShort() {
        convertBufferIfNeeded();
        return super.getShort();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#getShort(int)
     */
    @Override
    public short getShort(int index) {
        convertBufferIfNeeded();
        return super.getShort(index);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#getWrappedByteBuffer()
     */
    @Override
    public ByteBuffer getWrappedByteBuffer() {
        convertBufferIfNeeded();
        return super.getWrappedByteBuffer();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#getWrappedByteBufferNonSafe()
     */
    @Override
    public ByteBuffer getWrappedByteBufferNonSafe() {
        convertBufferIfNeeded();
        return super.getWrappedByteBufferNonSafe();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#hasArray()
     */
    @Override
    public boolean hasArray() {
        convertBufferIfNeeded();
        return super.hasArray();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#hasRemaining()
     */
    @Override
    public boolean hasRemaining() {
        if (isFCEnabled()) {
            int remaining = this.fcLimit - position();
            if (remaining < 0) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "hasRmeaining, less than 0");
                }
                RuntimeException rte = new RuntimeException("FCWsByteBufferImpl.hasRemaining(): value is less than 0");
                FFDCFilter.processException(rte, CLASS_NAME + ".hasRemaining()", "1115", this);
                throw rte;
            }
            return (remaining > 0);
        }
        return super.hasRemaining();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#isDirect()
     */
    @Override
    public boolean isDirect() {
        convertBufferIfNeeded();
        return super.isDirect();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#isReadOnly()
     */
    @Override
    public boolean isReadOnly() {
        convertBufferIfNeeded();
        return super.isReadOnly();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#limit()
     */
    @Override
    public int limit() {
        if (isFCEnabled()) {
            return this.fcLimit;
        }
        return super.limit();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#limit(int)
     */
    @Override
    public WsByteBuffer limit(int newLimit) {
        if (isFCEnabled()) {
            String errorMsg = null;
            if (newLimit > fcSize) {
                errorMsg = "Requested value for the WsByteBuffer limit was greater than the capacity";
            } else if (newLimit < 0) {
                errorMsg = "Requested value for the WsByteBuffer limit was less than 0";
            }
            if (errorMsg != null) {
                IllegalArgumentException iae = new IllegalArgumentException(errorMsg);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, errorMsg);
                }
                FFDCFilter.processException(iae, CLASS_NAME + ".limit(int)", "625", this);
                throw iae;
            }
            // set our FC limit
            this.fcLimit = newLimit;
            return this;
        }
        return super.limit(newLimit);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#mark()
     */
    @Override
    public WsByteBuffer mark() {
        convertBufferIfNeeded();
        return super.mark();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#order()
     */
    @Override
    public ByteOrder order() {
        convertBufferIfNeeded();
        return super.order();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#order(java.nio.ByteOrder)
     */
    @Override
    public WsByteBuffer order(ByteOrder bo) {
        convertBufferIfNeeded();
        return super.order(bo);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#position()
     */
    @Override
    public int position() {
        // if we are in transferTo mode, then this is the FileChannel position
        if (isFCEnabled()) {
            try {
                return (int) fc.position();
            } catch (IOException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception in position(): " + e);
                }
                FFDCFilter.processException(e, CLASS_NAME + ".position", "656", this);
                throw new RuntimeException(e);
            }
        }
        return super.position();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#position(int)
     */
    @Override
    public WsByteBuffer position(int p) {
        // if we are in transferTo mode, then this is the FileChannel position
        if (isFCEnabled()) {
            try {
                this.fc.position(p);
                return this;
            } catch (IOException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception in position(int): " + e);
                }
                FFDCFilter.processException(e, CLASS_NAME + ".position(int)", "687", this);
                throw new RuntimeException(e);
            }
        }
        return super.position(p);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#put(byte)
     */
    @Override
    public WsByteBuffer put(byte b) {
        convertBufferIfNeeded();
        return super.put(b);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#put(byte[])
     */
    @Override
    public WsByteBuffer put(byte[] src) {
        convertBufferIfNeeded();
        return super.put(src);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#put(byte[], int, int)
     */
    @Override
    public WsByteBuffer put(byte[] src, int offset, int length) {
        convertBufferIfNeeded();
        return super.put(src, offset, length);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#put(java.nio.ByteBuffer)
     */
    @Override
    public WsByteBuffer put(ByteBuffer src) {
        convertBufferIfNeeded();
        return super.put(src);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#put(int, byte)
     */
    @Override
    public WsByteBuffer put(int index, byte b) {
        convertBufferIfNeeded();
        return super.put(index, b);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#put(com.ibm.wsspi.bytebuffer.WsByteBuffer)
     */
    @Override
    public WsByteBuffer put(WsByteBuffer src) {
        convertBufferIfNeeded();
        return super.put(src);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#put(com.ibm.wsspi.bytebuffer.WsByteBuffer[])
     */
    @Override
    public WsByteBuffer put(WsByteBuffer[] src) {
        convertBufferIfNeeded();
        return super.put(src);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#putChar(char)
     */
    @Override
    public WsByteBuffer putChar(char value) {
        convertBufferIfNeeded();
        return super.putChar(value);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#putChar(char[])
     */
    @Override
    public WsByteBuffer putChar(char[] values) {
        convertBufferIfNeeded();
        return super.putChar(values);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#putChar(char[], int, int)
     */
    @Override
    public WsByteBuffer putChar(char[] values, int off, int len) {
        convertBufferIfNeeded();
        return super.putChar(values, off, len);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#putChar(int, char)
     */
    @Override
    public WsByteBuffer putChar(int index, char value) {
        convertBufferIfNeeded();
        return super.putChar(index, value);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#putDouble(double)
     */
    @Override
    public WsByteBuffer putDouble(double value) {
        convertBufferIfNeeded();
        return super.putDouble(value);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#putDouble(int, double)
     */
    @Override
    public WsByteBuffer putDouble(int index, double value) {
        convertBufferIfNeeded();
        return super.putDouble(index, value);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#putFloat(float)
     */
    @Override
    public WsByteBuffer putFloat(float value) {
        convertBufferIfNeeded();
        return super.putFloat(value);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#putFloat(int, float)
     */
    @Override
    public WsByteBuffer putFloat(int index, float value) {
        convertBufferIfNeeded();
        return super.putFloat(index, value);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#putInt(int)
     */
    @Override
    public WsByteBuffer putInt(int value) {
        convertBufferIfNeeded();
        return super.putInt(value);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#putInt(int, int)
     */
    @Override
    public WsByteBuffer putInt(int index, int value) {
        convertBufferIfNeeded();
        return super.putInt(index, value);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#putLong(int, long)
     */
    @Override
    public WsByteBuffer putLong(int index, long value) {
        convertBufferIfNeeded();
        return super.putLong(index, value);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#putLong(long)
     */
    @Override
    public WsByteBuffer putLong(long value) {
        convertBufferIfNeeded();
        return super.putLong(value);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#putShort(int, short)
     */
    @Override
    public WsByteBuffer putShort(int index, short value) {
        convertBufferIfNeeded();
        return super.putShort(index, value);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#putShort(short)
     */
    @Override
    public WsByteBuffer putShort(short value) {
        convertBufferIfNeeded();
        return super.putShort(value);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#putString(java.lang.String)
     */
    @Override
    public WsByteBuffer putString(String value) {
        convertBufferIfNeeded();
        return super.putString(value);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#readExternal(java.io.ObjectInput)
     */
    @Override
    public void readExternal(ObjectInput s) throws IOException, ClassNotFoundException {
        // writeExternal has already mapped filechannel content into a real buffer
        // so on reading it back, we just set up as though convert() was already
        // called
        this.status = WsByteBuffer.STATUS_BUFFER;
        super.readExternal(s);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "readExternal(ObjectInput)");
        }
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#remaining()
     */
    @Override
    public int remaining() {
        if (isFCEnabled()) {
            int remaining = this.fcLimit - position();
            if (remaining < 0) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "remaining is less than 0");
                }
                RuntimeException rte = new RuntimeException("FCWsByteBufferImpl.remaining(): value is less than 0");
                FFDCFilter.processException(rte, CLASS_NAME + ".remaining()", "1115", this);
                throw rte;
            }
            return remaining;
        }
        return super.remaining();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#reset()
     */
    @Override
    public WsByteBuffer reset() {
        convertBufferIfNeeded();
        return super.reset();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#rewind()
     */
    @Override
    public WsByteBuffer rewind() {
        convertBufferIfNeeded();
        return super.rewind();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#setByteBuffer(java.nio.ByteBuffer)
     */
    @Override
    public void setByteBuffer(ByteBuffer buffer) {
        convertBufferIfNeeded();
        super.setByteBuffer(buffer);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#setByteBufferNonSafe(java.nio.ByteBuffer)
     */
    @Override
    public void setByteBufferNonSafe(ByteBuffer buffer) {
        convertBufferIfNeeded();
        super.setByteBufferNonSafe(buffer);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#setDirectShadowBuffer(java.nio.ByteBuffer)
     */
    @Override
    public void setDirectShadowBuffer(ByteBuffer buffer) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setDirectShadowBuffer(ByteBuffer)");
        }
        convertBufferIfNeeded();
        super.setDirectShadowBuffer(buffer);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#slice()
     */
    @Override
    public WsByteBuffer slice() {
        convertBufferIfNeeded();
        return super.slice();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append(getClass().getSimpleName());
        sb.append('@');
        sb.append(Integer.toHexString(hashCode()));
        sb.append(" [");
        if (isFCEnabled()) {
            if (null == this.fc) {
                sb.append("null");
            } else {
                sb.append(this.fc.toString());
                try {
                    sb.append(" pos=").append(this.fc.position());
                } catch (IOException x) {
                    sb.append(" pos=error");
                }
                sb.append(" lim=").append(this.fcLimit);
                sb.append(" cap=").append(this.fcSize);
            }
            sb.append(" status=").append(this.status);
        } else {
            sb.append(super.toString());
        }
        sb.append(']');
        return sb.toString();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#writeExternal(java.io.ObjectOutput)
     */
    @Override
    public void writeExternal(ObjectOutput s) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "writeExternal(ObjectOutput)");
        }
        convertBufferIfNeeded();
        super.writeExternal(s);
    }

}
