/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channel.ssl.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;

import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferUtils;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;
import com.ibm.wsspi.tcpchannel.TCPRequestContext;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

/**
 * This class will hold static utility methods related to managing SSL.
 */
public class SSLUtils {

    /** Trace component for WAS */
    private static final TraceComponent tc = Tr.register(SSLUtils.class,
                                                         SSLChannelConstants.SSL_TRACE_NAME,
                                                         SSLChannelConstants.SSL_BUNDLE);
    /** Name of class used in various calls to FFDC. */
    private static final String CLASS_NAME = SSLUtils.class.getCanonicalName();
    /** Empty buffer used in wrap(null, blah) calls */
    private static ByteBuffer emptyBuffer;
    static {
        // initialize the empty byte buffer for certain wrap calls
        emptyBuffer = ByteBuffer.allocate(1);
        emptyBuffer.limit(0);
    }

    /**
     * Shut down the engine for the given SSL connection link.
     *
     * @param connLink
     * @param isServer
     * @param isConnected
     */
    public static void shutDownSSLEngine(SSLConnectionLink connLink, boolean isServer, boolean isConnected) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "shutDownSSLEngine: isServer: " + isServer
                         + " isConnected: " + isConnected + " " + connLink);
        }
        SSLEngine engine = connLink.getSSLEngine();

        if (engine != null) {

            if (isServer) {
                if (!engine.isInboundDone()) {
                    if (isConnected) {
                        engine.closeOutbound();
                        WsByteBuffer buffer = allocateByteBuffer(
                                                                 engine.getSession().getPacketBufferSize(), false);
                        flushCloseDown(engine, buffer, connLink);
                        buffer.release();
                    }
                    try {
                        engine.closeInbound();
                    } catch (SSLException se) {
                        // no FFDC required, should indicate that we're closing before
                        // the remote end started the close process
                    }
                }
            } else {
                // client side closing
                if (!engine.isOutboundDone()) {
                    engine.closeOutbound();
                }
                if (isConnected) {
                    WsByteBuffer buffer = allocateByteBuffer(
                                                             engine.getSession().getPacketBufferSize(), false);
                    flushCloseDown(engine, buffer, connLink);
                    buffer.release();
                    // Note: ideally we would do a read/unwrap at this point to pull the
                    // server side CLOSE message off the network but that shouldn't be
                    // necessary
                }
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "SSLEngine is null, must be shutdown already.");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "shutDownSSLEngine");
        }
    }

    /**
     * This method is called after a call to closing the inbound or outbound side
     * of the SSL Engine. It flushes the remaining data to the network, wrapping
     * up the SSL connection. Since this method is called as a result of a
     * VC.close or VC.destroy method, the writing to the network is done synchronously.
     *
     * @param engine               SSL engine associated with connection
     * @param outputBuffer         buffer to contain data to be flushed
     * @param deviceWriteInterface handle to channel on device side where write will be handled
     */
    private static void flushCloseDown(SSLEngine engine, WsByteBuffer outputBuffer, SSLConnectionLink xConnLink) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "flushCloseDown");
        }

        TCPWriteRequestContext deviceWriteInterface = xConnLink.getDeviceWriteInterface();

        int configuredTimeoutValue = xConnLink.getChannel().getTimeoutValueInSSLClosingHandshake();
        long timeoutTimestamp = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(configuredTimeoutValue);

        try {
            Status status = null;
            do {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "before wrap: \r\n\tbuf: " + getBufferTraceInfo(outputBuffer));
                }
                // Note: there is no fear of the netBuffers being too big for JSSE.
                // Allow SSL engine to flush out close down information to the caller.
                SSLEngineResult result = engine.wrap(emptyBuffer, outputBuffer.getWrappedByteBuffer());
                int amountToWrite = result.bytesProduced();
                long amountWritten = 0;
                if (0 < amountToWrite) {
                    outputBuffer.flip();
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "after wrap: \r\n\tbuf: " + getBufferTraceInfo(outputBuffer)
                                 + "\r\n\tstatus=" + result.getStatus()
                                 + " consumed=" + result.bytesConsumed()
                                 + " produced=" + result.bytesProduced());
                }
                if (0 < amountToWrite) {
                    // Inform the device side of the new buffer to write.
                    deviceWriteInterface.setBuffer(outputBuffer);

                    while (amountWritten < amountToWrite) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                            Tr.event(tc, "want to write bytes: " + amountToWrite);
                        }

                        // Write the buffer to the device side channel.  need to return immediately so we don't hang this write.
                        // Update the amount of bytes that have been written out in our internal instance.
                        amountWritten += deviceWriteInterface.write(0, TCPRequestContext.USE_CHANNEL_TIMEOUT);

                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                            Tr.event(tc, "bytes written: " + amountWritten);
                        }

                        if (xConnLink.getChannel().getstop0Called()) {
                            // don't hold up stopping the channel for this write handshake to complete, since it may never complete.
                            break;
                        }

                        if (configuredTimeoutValue > -1 && timeoutTimestamp <= System.currentTimeMillis()) {
                            // don't hold up stopping the channel if the custom property is not set to indefinite and the
                            // designated timeout point has been reached.
                            break;
                        }

                        else if (configuredTimeoutValue > -1 && amountWritten == 0) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                                Tr.event(tc, "Did not write anything, sleeping thread before trying again.");
                            }
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                // do nothing - continue trying to write until counter reaches the defined amount.
                            }

                        }

                        else if (amountWritten == 0) {
                            // if we didn't write anything, don't hard loop, but let other threads run before trying again.
                            Thread.yield();
                        }

                    }

                    // after the sync write, clear the buffer in case there was a
                    // buffer overflow condition we need to account for, and therefore
                    // another write will be need.
                    outputBuffer.clear();
                }
                // Extract the status to see if more must be written.
                status = result.getStatus();
                if ((Status.OK == status && 0 == amountToWrite) || xConnLink.getChannel().getstop0Called()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Did not produce anything, quit now");
                        Tr.debug(tc, "status: " + status + " amountToWrite: " + amountToWrite
                                     + " amountWritten: " + amountWritten + " stop0Called: " + xConnLink.getChannel().getstop0Called());
                    }
                    break; // out of while
                } else if (configuredTimeoutValue > -1 && timeoutTimestamp <= System.currentTimeMillis()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Closing handshake write timeout amount in SSL Channel met. Quit now.");
                    }
                    break; //out of while
                }
            } while (status != Status.CLOSED);
        } catch (Exception e) {
            // No FFDC needed.
            // Exception from doing write. Nothing more can be done.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception caught closing down, " + e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "flushCloseDown");
        }
    }

    /**
     * Return the underlying ByteBuffer array associated with the input WsByteBuffer array.
     *
     * @param wsbuffers
     * @return ByteBuffer[]
     */
    public static ByteBuffer[] getWrappedByteBuffers(WsByteBuffer wsbuffers[]) {
        // Create the buffer array to return.
        ByteBuffer buffers[] = new ByteBuffer[wsbuffers.length];
        boolean foundNullBuffer = false;
        int i = 0;
        // Loop through each buffer of the input array.
        for (i = 0; i < wsbuffers.length; i++) {
            if (wsbuffers[i] != null) {
                // Populate the return buffer array with the wrapped buffers of the input array.
                buffers[i] = wsbuffers[i].getWrappedByteBuffer();
            } else {
                // Handle case where end entries of array are null.
                foundNullBuffer = true;
                break;
            }
        }
        // Check if the array needs to be shrunk down.
        if (foundNullBuffer) {
            // Need to shrink. Create temporary array to hold current contents.
            ByteBuffer tempBuffers[] = buffers;
            // Replace return buffer array with appropriately sized array.
            buffers = new ByteBuffer[i];
            for (int j = 0; j < i; j++) {
                // Populate the return buffer with only non null entries.
                buffers[j] = tempBuffers[j];
            }
        }
        return buffers;
    }

    /**
     * Shift the limit of each buffer of the input array to the capacity.
     *
     * @param buffers
     */
    public static void limitToCapacity(WsByteBuffer buffers[]) {
        // Verify buffers are not null.
        if (buffers != null) {
            // Loop through each buffer in the array.
            for (int i = 0; i < buffers.length; i++) {
                // Verify each buffer in the array is not null.
                if (buffers[i] != null) {
                    // Shift the limit to the capacity.
                    buffers[i].limit(buffers[i].capacity());
                }
            }
        }
    }

    /**
     * Shift the limit of each buffer of the input array to the capacity.
     *
     * @param buffers
     */
    public static void positionToLimit(WsByteBuffer buffers[]) {
        // Verify buffers are not null.
        if (buffers != null) {
            // Loop through each buffer in the array.
            for (int i = 0; i < buffers.length; i++) {
                // Verify each buffer in the array is not null.
                if (buffers[i] != null) {
                    // Shift the position to the limit.
                    buffers[i].position(buffers[i].limit());
                }
            }
        }
    }

    /**
     * Sort of like Buffer.flip(). The limit is set to the postion,
     * but the position is set differently. If the index of the buffer
     * is markIndex and the mark is not zero, then the position will of
     * that buffer will be set to mark. All other buffers will be flipped.
     *
     * @param buffers         buffers on which flip to mark
     * @param mark            the mark to be set on the indexed buffer in the array
     * @param markBufferIndex the index into the array where the mark should be placed
     */
    public static void flipBuffersToMark(WsByteBuffer buffers[], int mark, int markBufferIndex) {
        WsByteBuffer buffer = null;
        // Verify the input buffer array is not null.
        if (buffers != null) {
            // Loop through all the buffers in the array.
            for (int i = 0; i < buffers.length; i++) {
                // Extract each buffer.
                buffer = buffers[i];
                // Verify the buffer is non null.
                if (buffer != null) {
                    if (i == markBufferIndex && mark != 0) {
                        // Found the "marked" buffer.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "mark is " + mark);
                        }
                        buffer.limit(buffer.position());
                        buffer.position(mark);
                    } else {
                        // Not the "marked" buffer, so flip it.
                        buffer.flip();
                    }
                }
            }
        }
    }

    /**
     * Flip the input list of buffers, walking through the list until the flipped
     * amount equals the input total size, mark the rest of the buffers as empty.
     *
     * @param buffers
     * @param totalSize
     */
    public static void flipBuffers(WsByteBuffer[] buffers, int totalSize) {
        int size = 0;
        boolean overLimit = false;
        for (int i = 0; i < buffers.length && null != buffers[i]; i++) {
            if (overLimit) {
                buffers[i].limit(buffers[i].position());
            } else {
                buffers[i].flip();
                size += buffers[i].remaining();
                overLimit = (size >= totalSize);
            }
        }
    }

    /**
     * Simlified method call for copyBuffer which sets length to copy at src.remaining.
     *
     * @param src
     * @param dst
     */
    public static void copyBuffer(WsByteBuffer src, WsByteBuffer dst) {
        copyBuffer(src, dst, src.remaining());
    }

    /**
     * Copy all the contents of the source buffer into the destination buffer. The contents
     * copied from the source buffer will be from its position. The data will be
     * copied into the destination buffer starting at its current position.
     *
     * @param src
     * @param dst
     * @param length
     */
    public static void copyBuffer(WsByteBuffer src, WsByteBuffer dst, int length) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "copyBuffer: length=" + length
                         + "\r\n\tsrc: " + getBufferTraceInfo(src)
                         + "\r\n\tdst: " + getBufferTraceInfo(dst));
        }

        // Double check that there is enough space in the dst buffer.
        if ((dst.remaining() < length) || (src.remaining() < length)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "copyBuffer: Not enough space");
            }
            RuntimeException e = new RuntimeException("Attempt to copy source buffer to inadequate destination buffer");
            FFDCFilter.processException(e, CLASS_NAME, "762", src);
            throw e;
        }

        // If the input buffer has a backing array, a copy will be more efficient.
        if (src.hasArray()) {
            int newSrcPosition = src.position() + length;
            // This buffer has a backing array. Copy the byte array in bulk.
            dst.put(src.array(), src.arrayOffset() + src.position(), length);
            // Advance position of src to show data has been read.
            src.position(newSrcPosition);
        } else {
            // No backing array. Pull the data into a byte array.
            byte srcByteArray[] = new byte[length];
            src.get(srcByteArray, 0, length);
            // Copy the data into the dst buffer with a single put.
            dst.put(srcByteArray);
        }
    }

    /**
     * Allocate a ByteBuffer per the SSL config at least as big as the input size.
     *
     * @param size           Minimum size of the resulting buffer.
     * @param allocateDirect flag to indicate if allocation should be done with direct byte buffers.
     * @return Newly allocated ByteBuffer
     */
    public static WsByteBuffer allocateByteBuffer(int size, boolean allocateDirect) {
        WsByteBuffer newBuffer = null;
        // Allocate based on the input parameter.
        if (allocateDirect) {
            newBuffer = ChannelFrameworkFactory.getBufferManager().allocateDirect(size);
        } else {
            newBuffer = ChannelFrameworkFactory.getBufferManager().allocate(size);
        }
        // Shift the limit to the capacity to maximize the space available in the buffer.
        newBuffer.limit(newBuffer.capacity());
        return newBuffer;
    }

    /**
     * This method is called for tracing in various places. It returns a string that
     * represents all the buffers in the array including hashcode, position, limit, and capacity.
     *
     * @param buffers array of buffers to get debug info on
     * @return string representing the buffer array
     */
    public static String getBufferTraceInfo(WsByteBuffer buffers[]) {
        if (null == buffers) {
            return "Null buffer array";
        }
        StringBuilder sb = new StringBuilder(32 + (64 * buffers.length));
        for (int i = 0; i < buffers.length; i++) {
            sb.append("\r\n\t  Buffer [");
            sb.append(i);
            sb.append("]: ");
            getBufferTraceInfo(sb, buffers[i]);
        }
        return sb.toString();
    }

    /**
     * This method is called for tracing in various places. It returns a string that
     * represents the buffer including hashcode, position, limit, and capacity.
     *
     * @param src
     * @param buffer buffer to get debug info on
     * @return StringBuilder
     */
    public static StringBuilder getBufferTraceInfo(StringBuilder src, WsByteBuffer buffer) {
        StringBuilder sb = (null == src) ? new StringBuilder(64) : src;
        if (null == buffer) {
            return sb.append("null");
        }
        sb.append("hc=").append(buffer.hashCode());
        sb.append(" pos=").append(buffer.position());
        sb.append(" lim=").append(buffer.limit());
        sb.append(" cap=").append(buffer.capacity());
        return sb;
    }

    /**
     * This method is called for tracing in various places. It returns a string that
     * represents the buffer including hashcode, position, limit, and capacity.
     *
     * @param buffer buffer to get debug info on
     * @return string representing the buffer
     */
    public static String getBufferTraceInfo(WsByteBuffer buffer) {
        if (null == buffer) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(64);
        sb.append("hc=").append(buffer.hashCode());
        sb.append(" pos=").append(buffer.position());
        sb.append(" lim=").append(buffer.limit());
        sb.append(" cap=").append(buffer.capacity());
        return sb.toString();
    }

    /**
     * Return a String containing all the data in the buffers provided
     * between their respective positions and limits.
     *
     * @param buffers
     * @return String
     */
    public static String showBufferContents(WsByteBuffer buffers[]) {
        if (null == buffers || 0 == buffers.length) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buffers.length; i++) {
            sb.append("Buffer [");
            sb.append(i);
            if (null == buffers[i]) {
                sb.append("]: null\r\n");
            } else {
                sb.append("]: ");
                sb.append(WsByteBufferUtils.asString(buffers[i]));
                sb.append("\r\n");
            }
        }
        return sb.toString();
    }

    /**
     * Allocate a buffer array large enough to contain totalDataSize bytes, but with the limit
     * of perBufferSize bytes per buffer.
     *
     * @param requestedBufferSize  size of the buffers that will be created.
     * @param totalDataSize        minimum bytes required in resulting buffer array
     * @param allocateDirect       type of allocation to do
     * @param enforceRequestedSize specifies if each buffer must not have its limit set to requestedSize
     * @return buffer array
     */
    public static WsByteBuffer[] allocateByteBuffers(int requestedBufferSize,
                                                     long totalDataSize,
                                                     boolean allocateDirect,
                                                     boolean enforceRequestedSize) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(tc, "allocateByteBuffers");
        }

        // Allocate the first buffer.
        WsByteBuffer firstBuffer = allocateByteBuffer(requestedBufferSize, allocateDirect);
        // Check if we are obligated to stick to the requested size.
        if (enforceRequestedSize) {
            firstBuffer.limit(requestedBufferSize);
        }
        // Note, allocation can result in a buffer larger than requested. Determine the
        // number of buffers to allocated based on the resulting actual size.
        int actualBufferSize = firstBuffer.limit();
        // if the actual size is the same as the requested size, then no need to force it
        boolean enforce = enforceRequestedSize;
        if (enforce && actualBufferSize == requestedBufferSize) {
            enforce = false;
        }
        int numBuffersToAllocate = (int) (totalDataSize / actualBufferSize);
        // Still need to account for a remainder.
        if ((totalDataSize % actualBufferSize) > 0) {
            numBuffersToAllocate++;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "allocate: requestSize="
                         + requestedBufferSize + ", actualSize="
                         + actualBufferSize + ", totSize=" + totalDataSize
                         + ", numBufs=" + numBuffersToAllocate);
        }
        // Create the array of the determined size, if the size is 0, allocate at least 1
        if (numBuffersToAllocate == 0) {
            numBuffersToAllocate = 1;
        }
        WsByteBuffer newBuffers[] = new WsByteBuffer[numBuffersToAllocate];
        newBuffers[0] = firstBuffer;
        for (int i = 1; i < newBuffers.length; i++) {
            newBuffers[i] = allocateByteBuffer(requestedBufferSize, allocateDirect);
            // Check if we are obligated to stick to the requested size.
            if (enforce) {
                newBuffers[i].limit(requestedBufferSize);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(tc, "allocateByteBuffers");
        }
        return newBuffers;
    }

    /**
     * Search for any buffer in the array that has a position that isn't zero.
     * If found, return true, otherwise false.
     *
     * @param buffers
     * @return boolean
     */
    public static boolean anyPositionsNonZero(WsByteBuffer buffers[]) {
        if (buffers != null) {
            // Loop through all buffers in the array.
            for (int i = 0; i < buffers.length; i++) {
                // Verify buffer is non null and check for nonzero position
                if ((buffers[i] != null) && (buffers[i].position() != 0)) {
                    // Found a nonzero position.
                    return true;
                }
            }
        }
        // If we get this far, all positions were zero
        return false;
    }

    /**
     * Query whether this engine is currently handshaking or not.
     *
     * @param engine
     * @return boolean
     */
    public static boolean isHandshaking(SSLEngine engine) {
        return (HandshakeStatus.NOT_HANDSHAKING != engine.getHandshakeStatus());
    }

    /**
     * Handle all the tasks involved in performing the SSL handshake.
     *
     * @param connLink           The connection link associated with current connection.
     * @param netBuffer          Buffer coming off the network.
     * @param decryptedNetBuffer Buffer for results of decrypted network buffer.
     * @param encryptedAppBuffer Buffer to be sent out on network.
     * @param inResult           Output of the last call into the ssl engine. Null if it hasn't happened yet.
     * @param handshakeCallback  non null when function here should be asynchronous
     * @param fromCallback       Whether or not this method was called from the read or write callback.
     * @throws IOException
     * @throws ReadOnlyBufferException
     * @return status after handshake or null if it is being handled asynchronously.
     */
    public static SSLEngineResult handleHandshake(SSLConnectionLink connLink,
                                                  WsByteBuffer netBuffer,
                                                  WsByteBuffer decryptedNetBuffer,
                                                  WsByteBuffer encryptedAppBuffer,
                                                  SSLEngineResult inResult,
                                                  SSLHandshakeCompletedCallback handshakeCallback,
                                                  boolean fromCallback) throws IOException, ReadOnlyBufferException {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        if (bTrace && tc.isEntryEnabled()) {
            Tr.entry(tc, "handleHandshake, engine=" + connLink.getSSLEngine().hashCode());
        }
        SSLEngineResult result = inResult;

        SSLEngine engine = connLink.getSSLEngine();
        TCPReadRequestContext deviceReadContext = connLink.getDeviceReadInterface();
        TCPWriteRequestContext deviceWriteContext = connLink.getDeviceWriteInterface();
        JSSEHelper jsseHelper = connLink.getChannel().getJsseHelper();

        // check to see if any ALPN negotiator is on the classpath; if so, and if this is the first time through this
        // method for the current connection (not a callback), register the current engine and link

        int amountToWrite = 0;
        boolean firstPass = true;
        HandshakeStatus hsstatus = HandshakeStatus.NEED_WRAP;
        Status status = Status.OK;
        if (null != result) {
            hsstatus = result.getHandshakeStatus();
            status = result.getStatus();
        }
        Status initialStatus = status;
        // Handle unique case where a callback re-enters after a BUFFER_UNDERFLOW. Need to reset buffers.
        if (fromCallback && status == Status.BUFFER_UNDERFLOW) {
            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "From callback, former status BUFFER_UNDERFLOW.");
            }
            netBuffer.limit(netBuffer.position());
            netBuffer.reset();
            // just read data, so change status to UNWRAP
            hsstatus = HandshakeStatus.NEED_UNWRAP;
            status = Status.OK;
        }

        while (isHandshaking(engine) || Status.OK != status) {
            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "status=" + status + " HSstatus=" + hsstatus);
            }
            if (hsstatus == HandshakeStatus.FINISHED) {
                // This is the last part of a change in SSLChannel in order to support
                // client certificate mapping. This step cleans up the state in the Thread.
                if (connLink.getChannel().isZOS) {
                    jsseHelper.setSSLPropertiesOnThread(null);
                }
                break; // out of while
            }

            // flush data out of the engine
            if (hsstatus == HandshakeStatus.NEED_WRAP) {
                if (bTrace && tc.isEventEnabled()) {
                    Tr.event(tc, "before wrap: \r\n\tencBuf: "
                                 + getBufferTraceInfo(encryptedAppBuffer));
                }
                result = engine.wrap(emptyBuffer, encryptedAppBuffer.getWrappedByteBuffer());
                amountToWrite = result.bytesProduced();
                if (0 < amountToWrite) {
                    encryptedAppBuffer.flip();
                }
                hsstatus = result.getHandshakeStatus();
                status = result.getStatus();
                if (bTrace && tc.isEventEnabled()) {
                    Tr.event(tc, "after wrap: \r\n\tencBuf: "
                                 + getBufferTraceInfo(encryptedAppBuffer)
                                 + "\r\n\tstatus=" + status
                                 + " HSstatus=" + hsstatus
                                 + " consumed=" + result.bytesConsumed()
                                 + " produced=" + result.bytesProduced());
                }

                if (0 < amountToWrite) {
                    // have to send this to peer
                    if (bTrace && tc.isEventEnabled()) {
                        Tr.event(tc, "Write bytes: " + amountToWrite);
                    }
                    deviceWriteContext.setBuffer(encryptedAppBuffer);
                    // Determine if this is an asynchronous handshake.
                    if (handshakeCallback != null) {
                        // Do an asynchronous write.
                        SSLHandshakeIOCallback writeCallback = new SSLHandshakeIOCallback(connLink, netBuffer, decryptedNetBuffer, encryptedAppBuffer, result, handshakeCallback);
                        VirtualConnection vc = deviceWriteContext.write(amountToWrite, writeCallback,
                                                                        false, TCPRequestContext.USE_CHANNEL_TIMEOUT);
                        // Check to see if write is already done.
                        if (vc != null) {
                            // Write is already done. The callback will not be used.
                            encryptedAppBuffer.clear();
                        } else {
                            // Write is not done, wait for callback
                            if (bTrace && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Write is not done.  Callback will be used.");
                            }
                            result = null;
                            break; // out of while
                        }
                    } else {
                        // Do a synchronous write.
                        deviceWriteContext.write(amountToWrite, TCPRequestContext.USE_CHANNEL_TIMEOUT);
                        encryptedAppBuffer.clear();
                    }
                } else {
                    // No data was put in the encryptedAppBuffer. Need to clear it for future use.
                    encryptedAppBuffer.clear();
                }
            } // if NEED_WRAP

            // ok, now know something more is needed
            while (hsstatus == HandshakeStatus.NEED_TASK) {
                Runnable task = engine.getDelegatedTask();
                if (task != null) {
                    // have a blocking task, go ahead and block this thread
                    task.run();
                    // then loop around and see if we have more to send to peer
                    hsstatus = engine.getHandshakeStatus();
                    if (bTrace && tc.isDebugEnabled()) {
                        Tr.debug(tc, "After task, hsstatus=" + hsstatus);
                    }
                } else {
                    if (bTrace && tc.isDebugEnabled()) {
                        Tr.debug(tc, "No task, setting status to HS_NEED_WRAP");
                    }
                    // we were told there was something to do, but got no task
                    hsstatus = HandshakeStatus.NEED_WRAP;
                    // guess that there's some data to be sent now
                }
                // make a new result object wrapping this in case it is the last
                // thing done on this pass (to restart correctly on next)
                result = new SSLEngineResult(status, hsstatus, 0, 0);
            } // while NEED_TASK

            if (hsstatus == HandshakeStatus.NEED_UNWRAP || status == Status.BUFFER_UNDERFLOW) {
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Get ready to decrypt data, netBuf: "
                                 + getBufferTraceInfo(netBuffer));
                }

                // See if more data needs to be read, but the buffer is already full.
                if ((netBuffer.limit() == netBuffer.capacity()) //PI52845 changed remaining for limit
                    && (status == Status.BUFFER_UNDERFLOW)) {
                    // In this case, we need to grow the buffer size.
                    int size = netBuffer.capacity() + engine.getSession().getPacketBufferSize();
                    WsByteBuffer tempBuffer = allocateByteBuffer(size, false);
                    copyBuffer(netBuffer, tempBuffer, netBuffer.remaining());
                    tempBuffer.flip();
                    netBuffer.release();
                    netBuffer = tempBuffer;

                    if (handshakeCallback != null) {
                        handshakeCallback.updateNetBuffer(netBuffer);
                    }

                    if (bTrace && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Had to grow the netBuf: " + getBufferTraceInfo(netBuffer));
                    }
                }

                // See if data is already available.
                if (Status.BUFFER_UNDERFLOW == status
                    || (!netBuffer.hasRemaining() && !firstPass)
                    || (netBuffer.remaining() == netBuffer.capacity()
                        && !(firstPass && Status.BUFFER_UNDERFLOW == initialStatus))) {
                    // No (or not enough) data available. Need to read.
                    deviceReadContext.setBuffer(netBuffer);
                    // Check if there is anything in the buffer.
                    if (!netBuffer.hasRemaining() || (netBuffer.remaining() == netBuffer.capacity())) {
                        // Nothing in the buffer. Clear it, pushing back the limit to the capacity.
                        netBuffer.clear();
                        netBuffer.mark();
                        if (bTrace && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Nothing was in the buffer");
                        }
                    } else {
                        // Data is in the buffer, but more is needed; BUFFER_UNDERFLOW case.
                        // Mark the start of the data to be read. Note this isn't necessarily zero.
                        netBuffer.mark();
                        // Ensure the read starts after the data already in the buffer
                        // and can extend all the way to the end of this buffer.
                        netBuffer.position(netBuffer.limit());
                        netBuffer.limit(netBuffer.capacity());
                        if (bTrace && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Existing data in netBuf: " + getBufferTraceInfo(netBuffer));
                        }
                    }
                    // Determine if this is an asynchronous handshake.
                    if (handshakeCallback != null) {
                        // Do an asynchronous read.
                        if (bTrace && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Do async read");
                        }
                        SSLHandshakeIOCallback readCallback = new SSLHandshakeIOCallback(connLink, netBuffer, decryptedNetBuffer, encryptedAppBuffer, result, handshakeCallback);
                        VirtualConnection vc = deviceReadContext.read(
                                                                      1, readCallback, false, TCPRequestContext.USE_CHANNEL_TIMEOUT);
                        // Check to see if read is already done.
                        if (vc != null) {
                            // Read is already done. The callback will not be used.
                            // Prepare buffer for unwrap.
                            if (bTrace && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Read already done.  No callback necessary.");
                            }
                            // Prepare buffer for unwrap. Flip to the former mark.
                            netBuffer.limit(netBuffer.position());
                            netBuffer.reset();
                        } else {
                            // Read is not done. Wait for callback
                            if (bTrace && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Read is not done.  Callback will be used.");
                            }
                            result = null;
                            break;
                        }
                    } else {
                        // Do a synchronous read.
                        if (bTrace && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Do sync read");
                        }
                        long bytesIn = 0L;
                        while (bytesIn == 0L) {

                            try {
                                bytesIn = deviceReadContext.read(1, TCPRequestContext.USE_CHANNEL_TIMEOUT);
                                if (bTrace && tc.isEventEnabled()) {
                                    Tr.event(tc, "Read bytes: " + bytesIn);
                                }
                            } catch (IOException x) {
                                // give probable cause to alert users to SSL configuration
                                // (like misconfigured certificates) problems.
                                String s = "IOException receiving data during SSL Handshake. One possible cause is that authentication may not be configured correctly";
                                Exception nx = new Exception(s, x);
                                FFDCFilter.processException(nx, CLASS_NAME, "882");
                                throw x;
                            }
                        }
                        // Prepare buffer for unwrap. Flip to the former mark.
                        netBuffer.limit(netBuffer.position());
                        netBuffer.reset();
                    }
                } else {
                    // Check if we just returned from read callback.
                    if (!netBuffer.hasRemaining() && firstPass) {
                        if (bTrace && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Callback came back with data that needs to be flipped.");
                        }
                        netBuffer.flip();
                    }
                    if (bTrace && tc.isEventEnabled()) {
                        Tr.event(tc, "Data already in netBuf: " + getBufferTraceInfo(netBuffer));
                    }
                }

                // Adjust buffer to unwrap all available data.
                // Position will be nonzero in 2 cases
                // 1 - async callback finished a read and netBuffer should be flipped
                // 2 - data enough for multiple unwraps in a row was available. Don't flip.
                if ((0 != netBuffer.position())
                    && (netBuffer.limit() == netBuffer.capacity())
                    && firstPass && Status.BUFFER_UNDERFLOW != initialStatus) {
                    // Prepare buffer for unwrap. Flip to the former mark.
                    netBuffer.limit(netBuffer.position());
                    netBuffer.reset();
                }
                if (bTrace && tc.isEventEnabled()) {
                    Tr.event(tc, "before unwrap: \r\n\tnetBuf: " + getBufferTraceInfo(netBuffer)
                                 + "\r\n\tdecBuf: " + getBufferTraceInfo(decryptedNetBuffer));
                }
                result = engine.unwrap(netBuffer.getWrappedByteBuffer(),
                                       decryptedNetBuffer.getWrappedByteBuffer());
                // handshakes shouldn't produce output so no need to flip the dec buffer
                hsstatus = result.getHandshakeStatus();
                status = result.getStatus();
                if (bTrace && tc.isEventEnabled()) {
                    Tr.event(tc, "after unwrap: \r\n\tnetBuf: " + getBufferTraceInfo(netBuffer)
                                 + "\r\n\tdecBuf: " + getBufferTraceInfo(decryptedNetBuffer)
                                 + "\r\n\tstatus=" + status
                                 + " HSstatus=" + hsstatus
                                 + " consumed=" + result.bytesConsumed()
                                 + " produced=" + result.bytesProduced());
                }
                if (netBuffer.remaining() == 0) {
                    netBuffer.clear();
                }
            } // if NEED_UNWRAP || UNDERFLOW

            if (status == Status.BUFFER_OVERFLOW) {
                // This should never happen since our code ensures the buffers are the size specified
                // in the SSL engine getPackageBufferSize and getApplicationBufferSize.
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "BUFFER_OVERFLOW occured during handshake: " + hsstatus);
                }
                throw new SSLException("BUFFER_OVERFLOW occured during handshake: " + hsstatus);
            }

            if (status == Status.CLOSED) {
                // This would happen if the handshake fails, initially or during renegotiation.
                if (bTrace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Handshake terminated SSL engine: " + hsstatus);
                }
                throw new SSLException("Handshake terminated SSL engine: " + hsstatus);
            }

            firstPass = false;
        } // end of handshake while

        if (bTrace && tc.isDebugEnabled()) {
            Tr.debug(tc, "after handshake loop, status=" + status
                         + " HSstatus=" + hsstatus
                         + ", fromCallback=" + fromCallback
                         + ", engine=" + engine.hashCode()
                         + "\r\n\tnetBuf: " + getBufferTraceInfo(netBuffer)
                         + "\r\n\tdecBuf: " + getBufferTraceInfo(decryptedNetBuffer));
        }

        if (fromCallback && null != result && null != handshakeCallback) {
            // This method was called from the write or read callback.
            // Need to claim complete through handshake callback.
            handshakeCallback.complete(result);
            // Set result to null so that caller knows action will be taken asynchronously.
            result = null;
        }
        if (bTrace && tc.isEntryEnabled()) {
            Tr.exit(tc, "handleHandshake");
        }
        return result;
    }

    /**
     * The purpose of this method is to take two SSL engines and have them do an
     * SSL handshake. If an exception is thrown, then the handshake was not successful.
     *
     * @param clientEngine
     * @param serverEngine
     * @throws SSLException if handshake fails
     */
    public static void handleHandshake(SSLEngine clientEngine, SSLEngine serverEngine) throws SSLException {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        if (bTrace && tc.isEntryEnabled()) {
            Tr.entry(tc, "handleHandshake");
        }
        if (clientEngine == null || serverEngine == null) {
            throw new SSLException("Null engine found: engine1=" + clientEngine + ", engine2=" + serverEngine);
        }
        SSLEngine currentEngine = clientEngine;
        SSLEngine otherEngine = serverEngine;

        if (bTrace && tc.isDebugEnabled()) {
            Tr.debug(tc, "Parameters: engine1=" + currentEngine.hashCode()
                         + ", engine2=" + otherEngine.hashCode());
        }

        WsByteBuffer netBuffer1 = allocateByteBuffer(
                                                     currentEngine.getSession().getPacketBufferSize(), false);
        WsByteBuffer netBuffer = netBuffer1;
        WsByteBuffer decryptedNetBuffer1 = allocateByteBuffer(
                                                              currentEngine.getSession().getApplicationBufferSize(), false);
        WsByteBuffer decryptedNetBuffer = decryptedNetBuffer1;
        WsByteBuffer encryptedAppBuffer1 = allocateByteBuffer(
                                                              currentEngine.getSession().getPacketBufferSize(), false);
        WsByteBuffer encryptedAppBuffer = encryptedAppBuffer1;
        SSLEngineResult result;
        Runnable task = null;

        SSLEngine tempEngine = null;

        HandshakeStatus tempStatus = null;
        HandshakeStatus currentStatus = HandshakeStatus.NEED_WRAP;
        HandshakeStatus otherStatus = HandshakeStatus.NEED_UNWRAP;

        if (bTrace && tc.isDebugEnabled()) {
            Tr.debug(tc, "current engine= " + currentEngine.hashCode() + ", status=" + currentStatus);
        }

        // Loop until both engines are finished handshaking.
        while (isHandshaking(currentEngine) || isHandshaking(otherEngine)
               || (currentStatus != HandshakeStatus.FINISHED)
               || (otherStatus != HandshakeStatus.FINISHED)) {

            // Check if data must be written to other engine.
            if ((currentStatus == HandshakeStatus.NEED_WRAP)
                && (encryptedAppBuffer.limit() == encryptedAppBuffer.capacity())) {
                if (bTrace && tc.isEventEnabled()) {
                    Tr.event(tc, "before wrap: encBuf: " + getBufferTraceInfo(encryptedAppBuffer));
                }
                result = currentEngine.wrap(emptyBuffer, encryptedAppBuffer.getWrappedByteBuffer());
                if (0 < result.bytesProduced()) {
                    encryptedAppBuffer.flip();
                }
                currentStatus = result.getHandshakeStatus();
                if (bTrace && tc.isEventEnabled()) {
                    Tr.event(tc, "after wrap: encBuf: " + getBufferTraceInfo(encryptedAppBuffer)
                                 + "\r\n\tstatus=" + result.getStatus()
                                 + " HSstatus=" + currentStatus
                                 + " consumed=" + result.bytesConsumed()
                                 + " produced=" + result.bytesProduced());
                }
            }

            // Check if data must be read from other engine.
            else if ((currentStatus == HandshakeStatus.FINISHED
                      || currentStatus == HandshakeStatus.NEED_UNWRAP)
                     && (netBuffer.limit() != netBuffer.capacity())) {
                if (bTrace && tc.isEventEnabled()) {
                    Tr.event(tc, "before unwrap: \r\n\tnetBuf: " + getBufferTraceInfo(netBuffer)
                                 + "\r\n\tdecBuf: " + getBufferTraceInfo(decryptedNetBuffer));
                }
                result = currentEngine.unwrap(
                                              netBuffer.getWrappedByteBuffer(),
                                              decryptedNetBuffer.getWrappedByteBuffer());
                // handshakes shouldn't produce output so no need to flip the dec buffer
                currentStatus = result.getHandshakeStatus();
                if (bTrace && tc.isEventEnabled()) {
                    Tr.event(tc, "after unwrap: \r\n\tnetBuf: " + getBufferTraceInfo(netBuffer)
                                 + "\r\n\tdecBuf: " + getBufferTraceInfo(decryptedNetBuffer)
                                 + "\r\n\tstatus=" + result.getStatus()
                                 + " HSstatus=" + currentStatus
                                 + " consumed=" + result.bytesConsumed()
                                 + " produced=" + result.bytesProduced());
                }
                // Clear netBuffer for reuse if all data is drained.
                if (netBuffer.remaining() == 0) {
                    netBuffer.clear();
                }
            }

            // Handle anything extra that must be done within the engine.
            if (currentStatus == HandshakeStatus.NEED_TASK) {
                while (currentStatus == HandshakeStatus.NEED_TASK) {
                    task = currentEngine.getDelegatedTask();
                    if (task != null) {
                        if (bTrace && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Run task");
                        }
                        // have a blocking task, go ahead and block this thread
                        task.run();
                        // then loop around and see if we have more to send to peer
                        currentStatus = currentEngine.getHandshakeStatus();
                        if (bTrace && tc.isDebugEnabled()) {
                            Tr.debug(tc, "After task, handshake status=" + currentStatus);
                        }
                    } else {
                        if (bTrace && tc.isDebugEnabled()) {
                            Tr.debug(tc, "No task, setting status to HS_NEED_WRAP");
                        }
                        // we were told there was something to do, but got no task
                        currentStatus = HandshakeStatus.NEED_WRAP;
                        // guess that there's some data to be sent now
                    }
                }

                // Check if more data needs to be sent from this engine.
                if (currentStatus == HandshakeStatus.NEED_WRAP) {
                    // More data to send. Start again at the top of the loop without switching engines.
                    continue;
                }
            } // end of NEED_TASK

            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "Switching engines");
            }
            // Encryped app output data from the current engine becomes network
            // input data for the other engine.
            netBuffer = encryptedAppBuffer;

            // Save aside the engine that was used in this loop.
            tempEngine = currentEngine;
            // Assign the engine to be used in the next loop.
            currentEngine = otherEngine;
            // Save the engine used in this loop for assignment next time around.
            otherEngine = tempEngine;

            // Save aside the status that was used in this loop
            tempStatus = currentStatus;
            // Assign the status to be used in the next loop.
            currentStatus = otherStatus;
            // Save the engine used in this loop for assignment next time around.
            otherStatus = tempStatus;

            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "current engine= " + currentEngine.hashCode() + ", status=" + currentStatus);
            }
        }

        if (bTrace && tc.isEntryEnabled()) {
            Tr.exit(tc, "handleHandshake");
        }
    }

    /**
     * Create an SSL engine with the input parameters. The host and port values
     * allow the re-use of possible cached SSL session ids.
     *
     * @param context
     * @param config
     * @param host
     * @param port
     * @param connLink
     * @return SSLEngine
     */
    public static SSLEngine getOutboundSSLEngine(SSLContext context,
                                                 SSLLinkConfig config, String host, int port, SSLConnectionLink connLink) {
        // PK46069 - use engine that allows session id re-use
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getOutboundSSLEngine, host=" + host + ", port=" + port);
        }
        SSLEngine engine = context.createSSLEngine(host, port);

        configureEngine(engine, FlowType.OUTBOUND, config, connLink);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getOutboundSSLEngine, hc=" + engine.hashCode());
        }
        return engine;
    }

    /**
     * Setup the SSL engine for the given context.
     *
     * @param context  used to build the engine
     * @param type     to determine if connection is inbound or outbound
     * @param config   SSL channel configuration
     * @param connLink
     * @return SSLEngine
     */
    public static SSLEngine getSSLEngine(SSLContext context, FlowType type, SSLLinkConfig config, SSLConnectionLink connLink) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getSSLEngine");
        }
        // Create a new SSL engine for this connection.
        SSLEngine engine = context.createSSLEngine();

        configureEngine(engine, type, config, connLink);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getSSLEngine, hc=" + engine.hashCode());
        }
        return engine;
    }

    /**
     * Configure the new engine based on the input flow type and configuration.
     *
     * @param engine
     * @param type
     * @param config
     * @param connLink
     */
    private static void configureEngine(SSLEngine engine, FlowType type, SSLLinkConfig config, SSLConnectionLink connLink) {
        // Update the engine with the latest config parameters.
        SSLParameters sslParameters = engine.getSSLParameters();

        sslParameters.setCipherSuites(config.getEnabledCipherSuites(engine));

        //Set the configured protocol on the SSLEngine
        String[] protocols = config.getSSLProtocol();
        if (protocols != null) {
            sslParameters.setProtocols(protocols);
        }

        if (type == FlowType.INBOUND) {
            engine.setUseClientMode(false);
            boolean clientAuth = config.getBooleanProperty(Constants.SSLPROP_CLIENT_AUTHENTICATION);
            // When on z/OS, need to check the MutualAuthCBINDCheck property
            // to force client authentication
            //TODO: z/os
            //            if (PlatformHelperFactory.getPlatformHelper().isZOS() &&
            //            	 UserRegistryConfig.TYPE_LOCAL_OS.equals(SecurityObjectLocator.getSecurityConfig().getActiveUserRegistry().getType()))
            //            {
            //            	Boolean cbindCheckEnabled = Boolean.FALSE;
            //
            //            	//get the endpoint name off of the thread
            //                Map connectionInfo = JSSEHelper.getInstance().getInboundConnectionInfo();
            //                if (connectionInfo != null) {
            //                    String endpointName = (String) connectionInfo.get(Constants.CONNECTION_INFO_ENDPOINT_NAME);
            //                    //only enforce cbind check on web requests
            //                    if (endpointName != null && endpointName.startsWith("HTTP")) {
            //                    	if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            //                            Tr.debug(tc, "The endpoint, " + endpointName + ", is web and we are on z/OS. Proceeding to get the mutualAuthCbindCheck property.");
            //                        }
            //		                try {
            //		                    // Get cbind check flag using reflection
            //		                    final String className = "com.ibm.ws.security.zOS.NativeConfiguration";
            //		                    final Class<?> zNativeConfigClass = Class.forName(className);
            //		                    final Method getConfigMethod = zNativeConfigClass.getMethod("getConfig", (java.lang.Class[]) null);
            //		                    final Object configInstance = getConfigMethod.invoke((java.lang.Class[]) null, (java.lang.Object[]) null);
            //		                    final Method cbindCheckMethod = zNativeConfigClass.getMethod("isMutualAuthCBINDCheckEnabled", (java.lang.Class[]) null);
            //		                    cbindCheckEnabled = (Boolean) cbindCheckMethod.invoke(configInstance, (java.lang.Object[]) null);
            //		                } catch (Exception e) {
            //		                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            //		                        Tr.debug(tc, "No class, object, or method found for CBIND check", new Object[] {e});
            //		                    FFDCFilter.processException(e, CLASS_NAME, "1237");
            //		                }
            //		                if (cbindCheckEnabled.booleanValue()) {
            //		                    flag = cbindCheckEnabled.booleanValue();
            //		                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            //		                        Tr.debug(tc, "The mutualAuthCBINDCheck is true, so hard-coded setNeedClientAuth to true.");}
            //		                }
            //		            }
            //                }
            //            }

            sslParameters.setNeedClientAuth(clientAuth);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Client auth needed is " + sslParameters.getNeedClientAuth());
            }
            if (!clientAuth) {
                // Client auth set to false, check client auth supported
                boolean clientAuthSuported = config.getBooleanProperty(Constants.SSLPROP_CLIENT_AUTHENTICATION_SUPPORTED);
                sslParameters.setWantClientAuth(clientAuthSuported);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Client auth supported is " + sslParameters.getWantClientAuth());
                }
            }
            // set use cipher suite order
            boolean useCipherOrder = config.getBooleanProperty(Constants.SSLPROP_ENFORCE_CIPHER_ORDER);
            if (useCipherOrder) {
                sslParameters.setUseCipherSuitesOrder(useCipherOrder);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Use cipher order is " + sslParameters.getUseCipherSuitesOrder());
                }
            }

            //set the ssl parameters collected on the engine
            engine.setSSLParameters(sslParameters);

            // enable ALPN support if this is for an inbound connection
            AlpnSupportUtils.registerAlpnSupport(connLink, engine);

        } else {
            // Update engine with client side specific config parameters.
            engine.setUseClientMode(true);
            boolean verifyHostname = config.getBooleanProperty(Constants.SSLPROP_HOSTNAME_VERIFICATION);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "verifyHostname:  " + verifyHostname);
            }
            if (verifyHostname) {
                String peerHostname = engine.getPeerHost();
                config.getProperty(Constants.SSLPROP_SKIP_HOSTNAME_VERIFICATION_FOR_HOSTS);
                String skipHostList = config.getProperty(Constants.SSLPROP_SKIP_HOSTNAME_VERIFICATION_FOR_HOSTS);
                if (!Constants.isSkipHostnameVerificationForHosts(peerHostname, skipHostList)) {
                    sslParameters.setEndpointIdentificationAlgorithm("HTTPS"); // enable hostname verification
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Hostname verification is enabled");
                    }
                }
            }

            //set the ssl parameters collected on the engine
            engine.setSSLParameters(sslParameters);
        }

        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Calling beginHandshake on engine");
            }
            engine.beginHandshake();
        } catch (SSLException se) {
            // no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Error while starting handshake; " + se);
            }
        }
    }

    /**
     * The purpose of this method is to prepare the input buffers to be sent
     * into a call to wrap or unwrap in the JSSE considering the limitations
     * about how much data can be present in each buffer. This amount is determined
     * by the amount of data between position and limit.
     *
     * @param buffers       array of buffers being examined which will be sent into the JSSE
     * @param maxBufferSize maximum size allowed for each buffer in the array
     * @return int array of size 2. The first entry is the index of the buffer changed
     *         in the array. The second entry is the actual limit that will be saved
     *         for restoration after the call is made to wrap or unwrap in the JSSE.
     *         If it is identified that no changes are needed, null is returned.
     */
    public static int[] adjustBuffersForJSSE(WsByteBuffer[] buffers, int maxBufferSize) {
        int[] limitInfo = null;
        int dataAmount = 0;
        for (int i = 0; i < buffers.length && null != buffers[i]; i++) {
            // Valid buffer. Tally up how much data is in the buffer array so far.
            dataAmount += buffers[i].remaining();
            // Check if the max has been surpassed.
            if (dataAmount > maxBufferSize) {
                // The max has been surpassed. Save the current array index and limit.
                int savedLimit = buffers[i].limit();
                limitInfo = new int[] { i, savedLimit };
                // Adjust the limit to the max allowed.
                int overFlow = dataAmount - maxBufferSize;
                buffers[i].limit(savedLimit - overFlow);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "adjustBuffersForJSSE: buffer [" + i + "] from "
                                 + savedLimit + " to " + buffers[i].limit());
                }
                // Since this was the key buffer, break from the loop.
                break;
            } else if (dataAmount == maxBufferSize) {
                // Boundaries of buffers and max lined up perfectly. No changes required.
                break;
            }
        }
        return limitInfo;
    }

    /**
     * The purpose of this method is to prepare the input buffer to be sent
     * into a call to wrap or unwrap in the JSSE considering the limitations
     * about how much data can be present in each buffer. This amount is determined
     * by the amount of data between position and limit.
     *
     * @param buffer        being examined which will be sent into the JSSE
     * @param maxBufferSize maximum size allowed
     * @return int - limit to restore later, -1 if no changes required
     */
    public static int adjustBufferForJSSE(WsByteBuffer buffer, int maxBufferSize) {
        int limit = -1;
        if (null != buffer) {
            int size = buffer.remaining();
            if (maxBufferSize < size) {
                limit = buffer.limit();
                // Adjust the limit to the max allowed.
                int overFlow = size - maxBufferSize;
                buffer.limit(limit - overFlow);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "adjustBufferForJSSE: from " + limit + " to " + buffer.limit());
                }
            }
        }
        return limit;
    }

    /**
     * This method is called after a call is made to adjustBuffersForJSSE followed by a
     * call to wrap or unwrap in the JSSE. It restores the saved limit in the buffer
     * that was modified. A few extra checks are done here to prevent any problems during
     * odd code paths in the future.
     *
     * @param buffers   array of buffers containing the buffer whose limit should be reset.
     * @param limitInfo array of 2. The first entry is the index of the buffer whose limit
     *                      will be restored in the array. The second entry is the actual limit to be restored.
     */
    public static void resetBuffersAfterJSSE(WsByteBuffer[] buffers, int[] limitInfo) {
        // Handle case where not changes were made in recent call to adjustBuffersForJSSE
        if (limitInfo == null) {
            return;
        }

        int bufferIndex = limitInfo[0];
        int bufferLimit = limitInfo[1];
        // Ensure buffer index is within array bounds.
        if (buffers.length > bufferIndex) {
            WsByteBuffer buffer = buffers[bufferIndex];
            // Ensure the buffer is not null and the limit won't be set beyond the capacity
            if ((buffer != null) && (buffer.capacity() >= bufferLimit)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "resetBuffersAfterJSSE: buffer [" + bufferIndex
                                 + "] from " + buffer.limit() + " to " + bufferLimit);
                }
                // Make the adjustment.
                buffer.limit(bufferLimit);
            }
        }
    }

    /**
     * Assign an array of limits associated with the passed in buffer array.
     *
     * @param buffers
     * @param limits
     */
    public static void getBufferLimits(WsByteBuffer[] buffers, int[] limits) {
        // Double check that the parameters are non null.
        if ((buffers != null) && (limits != null)) {
            // Loop through the buffers.
            // In case of errant parameters, protect from array out of bounds.
            for (int i = 0; i < buffers.length && i < limits.length; i++) {
                // Double check for null buffers.
                if (buffers[i] != null) {
                    // Save the limit.
                    limits[i] = buffers[i].limit();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "getBufferLimits: buffer[" + i + "] limit of " + limits[i]);
                    }
                } else {
                    // When buffer is null, save a limit of 0.
                    limits[i] = 0;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "getBufferLimits: null buffer[" + i + "] limit of " + limits[i]);
                    }
                }
            }
        }
    }

    /**
     * Assign the limits of the passed in buffers to the specified values in the int array.
     * The lengths of both arrays are expected to be similar. The limits are expected to
     * be settable. IE, the respective buffers should have capacity >= to the limit.
     *
     * @param buffers
     * @param limits
     */
    public static void setBufferLimits(WsByteBuffer[] buffers, int[] limits) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "setBufferLimits");
        }

        // Double check that parameters are not null.
        if ((buffers != null) && (limits != null)) {
            int bufferCapacity = 0;
            int limit = 0;
            // Loop through the buffers.
            // In case of errant parameters, protect from array out of bounds.
            for (int i = 0; i < buffers.length && i < limits.length; i++) {
                // Double check for allowed null buffers.
                if (buffers[i] != null) {
                    bufferCapacity = buffers[i].capacity();
                    limit = limits[i];
                    // Only update limit if it changed.
                    if (buffers[i].limit() != limit) {
                        // Double check that the new limit is valid.
                        if (bufferCapacity >= limit) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Buffer [" + i + "] being updated from " + buffers[i].limit() + " to " + limit);
                            }
                            buffers[i].limit(limit);
                        } else {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Buffer [" + i + "] has capacity " + bufferCapacity + " less than passed in limit " + limit);
                            }
                        }
                    }
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setBufferLimits");
        }
    }

    /**
     * Find the amount of data in the source buffers, starting at the input index.
     *
     * @param src
     * @param startIndex
     * @return int
     */
    public static int lengthOf(WsByteBuffer[] src, int startIndex) {
        int length = 0;
        if (null != src) {
            for (int i = startIndex; i < src.length && null != src[i]; i++) {
                length += src[i].remaining();
            }
        }
        return length;
    }

    /**
     * Take the source buffers and extract all non-null, non-empty buffers into
     * a new list. If the releaseEmpty flag is true, then it will release the
     * empty buffers.
     *
     * @param src
     * @param releaseEmpty
     * @return WsByteBuffer[], null if no buffers existed with data
     */
    public static WsByteBuffer[] compressBuffers(WsByteBuffer[] src, boolean releaseEmpty) {
        List<WsByteBuffer> output = new LinkedList<WsByteBuffer>();
        boolean first = true;
        int size = 0;
        for (int i = 0; i < src.length; i++) {
            if (null == src[i]) {
                continue;
            }
            // if we have data, then save it. If it's the first buffer found
            // with data, then slice just the remaining data
            if (0 < src[i].remaining()) {
                if (first) {
                    output.add(src[i].slice());
                    src[i].release();
                    first = false;
                } else {
                    output.add(src[i]);
                }
                size++;
            } else if (releaseEmpty) {
                src[i].release();
            }
        }
        if (0 == size) {
            return null;
        }
        WsByteBuffer[] data = new WsByteBuffer[size];
        output.toArray(data);
        return data;
    }
}
