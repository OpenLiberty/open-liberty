/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.io.async;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.tcpchannel.internal.TCPChannelMessageConstants;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;

/**
 * The CompletionKey is a data object for communicating the data between Java
 * and Native code when making IO requests. It identifies a particular operation
 * via its channel identifier and call identifier, and carries the return code
 * and number of bytes affected by a successful operation.
 * 
 */
public class CompletionKey {
    private static final TraceComponent tc = Tr.register(CompletionKey.class,
                                                         TCPChannelMessageConstants.TCP_TRACE_NAME,
                                                         TCPChannelMessageConstants.TCP_BUNDLE);

    /*
     * The following indexes map out the structure of the DirectByteBuffer which
     * is used to transfer data to/from the native code - it is mapped as an
     * array of long values (through a LongBuffer view of the ByteBuffer), with
     * indexes as defined by this list
     */
    private static final int CHANNEL_ID_INDEX = 0;
    private static final int CALL_ID_INDEX = 1;
    private static final int RETURN_CODE_INDEX = 2;
    private static final int BYTES_AFFECTED_INDEX = 3;
    private static final int NATIVE_STRUCTURE_INDEX = 4; // used for IO requests
    private static final int JIT_BUFFER_USED = 4; // used for IOCompletion notifications
    private static final int RETURN_STATUS_INDEX = 5;
    private static final int FIRST_BUFFER_INDEX = 6;

    /*
     * The NATIVE_STRUCTURE_INDEX field is an element provided to allow the
     * native code in the async io library to allocate and attach a block of
     * native memory to this data structure for use during io operations. It is
     * expected that the native code will be called to initialize this field
     * before the CompletionKey is first used and that the native code will
     * again be called when the CompletionKey is disposed.
     */

    /*
     * Although the async natives give us the results back as longs, we can only
     * handle int lengths through ByteBuffers. This is potentially lossy for the
     * bytes affected, but not for the completion key (as we passed that in as
     * an int).
     * 
     * Impl: This jlong[] will be mapped directly to a IOCBTYPE in the async
     * natives, any changes to this class impl must be reflected in the native
     * and vice versa.
     */
    private ByteBuffer rawData;
    private LocalByteBuffer stagingByteBuffer;

    private int bufferCount = 0;
    private long channelIdentifier;
    private long callIdentifier;

    /**
     * Constructor.
     */
    CompletionKey() {
        this(0, 0, 1);
    }

    /**
     * Create a "long" Completion Key which includes data relating to data
     * buffers.
     * 
     * @param channelIdentifier a long value holding the channelID
     * @param callIdentifier a long value holding the callID
     * @param bufferCount a count of the number of data buffers to include
     */
    public CompletionKey(long channelIdentifier, long callIdentifier, int bufferCount) {
        if (bufferCount < 1)
            throw new IllegalArgumentException("Buffer count cannot be < 0 !");
        int bufferLength = 8 * (FIRST_BUFFER_INDEX + (bufferCount * 2));

        this.rawData = this.allocateDirect(bufferLength);
        this.stagingByteBuffer = new LocalByteBuffer(bufferLength);

        this.rawData.order(ByteOrder.nativeOrder());

        this.stagingByteBuffer.putLong(CHANNEL_ID_INDEX * 8, channelIdentifier);
        this.stagingByteBuffer.putLong(CALL_ID_INDEX * 8, callIdentifier);
        this.stagingByteBuffer.putLong(BYTES_AFFECTED_INDEX * 8, -1L);
        this.stagingByteBuffer.putLong(RETURN_CODE_INDEX * 8, 0);
        this.stagingByteBuffer.putLong(NATIVE_STRUCTURE_INDEX * 8, 0);
        this.stagingByteBuffer.putLong(RETURN_STATUS_INDEX * 8, 0);

        this.channelIdentifier = channelIdentifier;
        this.callIdentifier = callIdentifier;
        this.bufferCount = bufferCount;
    }

    protected ByteBuffer allocateDirect(int bufferLength) {
        WsByteBufferPoolManager wsByteBufferManager = ChannelFrameworkFactory.getBufferManager();
        return wsByteBufferManager.allocateDirect(bufferLength).getWrappedByteBufferNonSafe();
    }

    /**
     * Gets the native address of the start of the byte buffer which holds the
     * data for this CompletionKey.
     * 
     * @return long
     */
    public long getAddress() {
        return AbstractAsyncChannel.getBufAddress(this.rawData);
    }

    /**
     * Sets the address and length of a buffer with a specified index.
     * 
     * @param address of the buffer
     * @param length of the buffer in bytes
     * @param index of the buffer to set, where 0 is the first buffer
     * @throws IllegalArgumentException if the index value is <0 or >= bufferCount
     */
    public void setBuffer(long address, long length, int index) {
        if ((index < 0) || (index >= this.bufferCount)) {
            throw new IllegalArgumentException();
        }

        this.stagingByteBuffer.putLong((FIRST_BUFFER_INDEX + (2 * index)) * 8, address);
        this.stagingByteBuffer.putLong((FIRST_BUFFER_INDEX + (2 * index) + 1) * 8, length);
    }

    /**
     * Returns the number of buffer elements that can be accommodated by this
     * CompletionKey.
     * 
     * @return int
     */
    public int getBufferCount() {
        return this.bufferCount;
    }

    /**
     * Returns the number of bytes affected (read/written) for the completed
     * operation identified by this data.
     * 
     * @return long
     */
    public long getBytesAffected() {
        return this.stagingByteBuffer.getLong(BYTES_AFFECTED_INDEX * 8);
    }

    /**
     * Sets the number of bytes affected (read/written) for the completed
     * operation identified by this data.
     * 
     * @param count the number of bytes.
     */
    public void setBytesAffected(int count) {
        this.stagingByteBuffer.putLong(BYTES_AFFECTED_INDEX * 8, count);
    }

    /**
     * Returns the call identifier for the operation relating to this data.
     * 
     * @return long
     */
    public long getCallIdentifier() {
        return this.stagingByteBuffer.getLong(CALL_ID_INDEX * 8);
    }

    /**
     * Sets the call identifier for the operation relating to this data.
     * 
     * @param callid
     */
    public void setCallIdentifier(long callid) {
        this.callIdentifier = callid;
        this.stagingByteBuffer.putLong(CALL_ID_INDEX * 8, callid);
    }

    /**
     * Returns the channel identifier for the operation relating to this data.
     * 
     * @return long
     */
    public long getChannelIdentifier() {
        return this.stagingByteBuffer.getLong(CHANNEL_ID_INDEX * 8);
    }

    /**
     * Returns the operation result status.
     * 
     * @return long
     */
    public long getReturnStatus() {
        return this.stagingByteBuffer.getLong(RETURN_STATUS_INDEX * 8);
    }

    /**
     * Returns the operation return status.
     * 
     * @param status
     */
    public void setReturnStatus(int status) {
        this.stagingByteBuffer.putLong(RETURN_STATUS_INDEX * 8, status);
    }

    /**
     * Returns the result code for the operation where the operation has
     * completed.
     * 
     * @return the platform-specific return code.
     */
    public int getReturnCode() {
        long returnCode = this.stagingByteBuffer.getLong(RETURN_CODE_INDEX * 8);
        if (returnCode > Integer.MAX_VALUE) {
            AsyncException ae = new AsyncException("Return code value invalid");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Invalid value returned for return code, exception: " + ae.getMessage());
            }
            FFDCFilter.processException(ae, getClass().getName(), "227", this);
            return Integer.MAX_VALUE;
        }
        return (int) returnCode;
    }

    /**
     * Sets indication that JIT buffers are being used.
     */
    public void setJITBufferUsed() {
        this.stagingByteBuffer.putLong(JIT_BUFFER_USED * 8, 1);
    }

    /**
     * Returns whether or not the operation used the JIT buffer completed.
     * 
     * @return the platform-specific return code.
     */
    public boolean wasJITBufferUsed() {
        return (1 == this.stagingByteBuffer.getLong(JIT_BUFFER_USED * 8));
    }

    /**
     * Sets the platform-specific return code for the operation identified by
     * the channel and call id.
     * 
     * @param rc
     */
    public void setReturnCode(int rc) {
        this.stagingByteBuffer.putLong(RETURN_CODE_INDEX * 8, rc);
    }

    protected void expandBuffers(int count) {
        if (count < 1)
            throw new IllegalArgumentException("Buffer count cannot be < 0 !");
        int bufferLength = 8 * (FIRST_BUFFER_INDEX + (count * 2));
        // save native structure data from old structure
        long nativeStruct = this.stagingByteBuffer.getLong(NATIVE_STRUCTURE_INDEX * 8);

        // Use optimized ByteBuffer manager
        //this.rawData = ByteBuffer.allocateDirect(bufferLength);
        this.rawData = this.allocateDirect(bufferLength);

        this.rawData.order(ByteOrder.nativeOrder());

        // reset all values in the new structure with data from original
        this.stagingByteBuffer = new LocalByteBuffer(bufferLength);
        this.stagingByteBuffer.putLong(CHANNEL_ID_INDEX * 8, this.channelIdentifier);
        this.stagingByteBuffer.putLong(CALL_ID_INDEX * 8, callIdentifier);
        this.stagingByteBuffer.putLong(BYTES_AFFECTED_INDEX * 8, -1L);
        this.stagingByteBuffer.putLong(RETURN_CODE_INDEX * 8, 0);
        this.stagingByteBuffer.putLong(NATIVE_STRUCTURE_INDEX * 8, nativeStruct);
        this.stagingByteBuffer.putLong(RETURN_STATUS_INDEX * 8, 0);

        this.bufferCount = count;
    }

    protected void reset() {
        this.stagingByteBuffer.putLong(BYTES_AFFECTED_INDEX * 8, -1L);
        this.stagingByteBuffer.putLong(RETURN_CODE_INDEX * 8, 0);
        this.stagingByteBuffer.putLong(NATIVE_STRUCTURE_INDEX * 8, 0);
        this.stagingByteBuffer.putLong(RETURN_STATUS_INDEX * 8, 0);
    }

    protected void initializePoolEntry(long _channelIdentifier, long _callIdentifier) {
        this.stagingByteBuffer.putLong(CHANNEL_ID_INDEX * 8, _channelIdentifier);
        this.stagingByteBuffer.putLong(CALL_ID_INDEX * 8, _callIdentifier);
        this.stagingByteBuffer.putLong(BYTES_AFFECTED_INDEX * 8, -1L);
        this.stagingByteBuffer.putLong(RETURN_CODE_INDEX * 8, 0);
        this.stagingByteBuffer.putLong(NATIVE_STRUCTURE_INDEX * 8, 0);
        this.stagingByteBuffer.putLong(RETURN_STATUS_INDEX * 8, 0);

        this.channelIdentifier = _channelIdentifier;
        this.callIdentifier = _callIdentifier;
    }

    /**
     * Returns a printable representation of the receiver suitable for display
     * to a user.
     * 
     * @return the formatted string.
     */
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(128);
        buffer.append(this.getClass().getName());
        buffer.append("[channel id=").append(getChannelIdentifier());
        buffer.append(", call id=").append(getCallIdentifier());
        buffer.append(", rc=").append(getReturnCode());
        buffer.append(", bytes=").append(getBytesAffected());
        buffer.append(", Native address/JIT used=").append(this.stagingByteBuffer.getLong(NATIVE_STRUCTURE_INDEX * 8));
        buffer.append("]\n");
        return buffer.toString();
    }

    protected void postNativePrep() {
        this.rawData.position(0);
        this.rawData.get(this.stagingByteBuffer.data);
    }

    protected void preNativePrep() {
        this.rawData.clear();
        this.rawData.put(this.stagingByteBuffer.data);
    }

    /**
     * Inner class taking a byte array and abstracting it out in a ByteBuffer-like way.
     */
    @Trivial
    private final static class LocalByteBuffer {

        /** The byte array we are mapping onto. */
        private byte[] data = null;

        /** Constructor */
        LocalByteBuffer(int size) {
            this.data = new byte[size];
        }

        /** Absolute get (long). Caller must verify index/length is valid. */
        long getLong(int index) {
            return (((long) (data[index + 7] & 0xFF)) << 0) +
                   (((long) (data[index + 6] & 0xFF)) << 8) +
                   (((long) (data[index + 5] & 0xFF)) << 16) +
                   (((long) (data[index + 4] & 0xFF)) << 24) +
                   (((long) (data[index + 3] & 0xFF)) << 32) +
                   (((long) (data[index + 2] & 0xFF)) << 40) +
                   (((long) (data[index + 1] & 0xFF)) << 48) +
                   (((long) (data[index + 0])) << 56);
        }

        /** Absolute put (long). Caller must verify index/length is valid. */
        void putLong(int index, long value) {
            data[index + 7] = (byte) (value >>> 0);
            data[index + 6] = (byte) (value >>> 8);
            data[index + 5] = (byte) (value >>> 16);
            data[index + 4] = (byte) (value >>> 24);
            data[index + 3] = (byte) (value >>> 32);
            data[index + 2] = (byte) (value >>> 40);
            data[index + 1] = (byte) (value >>> 48);
            data[index + 0] = (byte) (value >>> 56);
        }
    };
}