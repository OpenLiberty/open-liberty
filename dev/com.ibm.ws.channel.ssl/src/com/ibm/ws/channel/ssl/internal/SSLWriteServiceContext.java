/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channel.ssl.internal;

import java.io.IOException;
import java.nio.ReadOnlyBufferException;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;

import com.ibm.websphere.event.Event;
import com.ibm.websphere.event.EventEngine;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferUtils;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

/**
 * SSL Channel's TCPWriteRequestContext implementation.
 */
public class SSLWriteServiceContext extends SSLBaseServiceContext implements TCPWriteRequestContext, TCPWriteCompletedCallback {

    /** Trace component for WAS. Protected for use by inner classes. */
    protected static final TraceComponent tc = Tr.register(SSLWriteServiceContext.class,
                                                           SSLChannelConstants.SSL_TRACE_NAME,
                                                           SSLChannelConstants.SSL_BUNDLE);

    // Note: cannot save ref to sslEngine since it could change
    /**
     * Callback from app channel used to notify when a write request is complete.
     * Protected for use by inner classes.
     */
    protected TCPWriteCompletedCallback callback = null;
    /** Output buffer used to store results of encryption and to write to device channel. */
    private WsByteBuffer encryptedAppBuffer = null;
    /** Work that is queued. */
    private QueuedWork queuedWork = null;
    /** Reusable handshake callback. */
    private MyHandshakeCompletedCallback handshakeCallback = null;
    private long asyncBytesToWrite = 0L;
    private int asyncTimeout = 0;

    private final Object closeSync = new Object();
    private boolean closeCalled = false;

    /**
     * Constructor.
     *
     * @param connLink
     */
    public SSLWriteServiceContext(SSLConnectionLink connLink) {
        super(connLink);
        this.queuedWork = new QueuedWork();
        this.handshakeCallback = new MyHandshakeCompletedCallback(this);
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.TCPWriteRequestContext#write(long, int)
     */
    @Override
    public long write(long _numBytes, int timeout) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "write, numBytes=" + _numBytes + ", timeout=" + timeout + ", vc=" + getVCHash());
        }
        final TCPWriteRequestContext tcp = getConnLink().getDeviceWriteInterface();
        long numBytesLeft = _numBytes;

        // Handle timing out of former read request.
        if (timeout == IMMED_TIMEOUT || timeout == ABORT_TIMEOUT) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Requested to timeout former request.  Calling device side.");
            }
            tcp.write(numBytesLeft, timeout);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "write");
            }
            return 0;
        }

        // Look for errors in the request.
        IOException exception = checkForErrors(numBytesLeft, false);
        if (exception != null) {
            // Found an error.
            throw exception;
        }
        SSLEngineResult result = null;
        final long maxBytes = WsByteBufferUtils.lengthOf(getBuffers());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "App provided " + maxBytes + " bytes");
        }

        // Adjust size of numBytes if all data is requested to be written (-1).
        if (numBytesLeft == TCPWriteRequestContext.WRITE_ALL_DATA) {
            numBytesLeft = maxBytes;
        }

        // Check if a handshake is needed.
        if (SSLUtils.isHandshaking(getConnLink().getSSLEngine())) {
            try {
                // A handshake is needed.
                result = doHandshake(null);
            } catch (IOException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception caught during handshake, " + e);
                }
                FFDCFilter.processException(e, getClass().getName(), "84", this);
                throw e;
            }
            // Verify the output of the handshake.
            if (result.getHandshakeStatus() != HandshakeStatus.FINISHED) {
                IOException e = new IOException("Unable to complete SSLhandshake");
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unable to complete SSLhandshake, " + e);
                }
                FFDCFilter.processException(e, getClass().getName(), "117", this);
                throw e;
            }
        }

        // Encrypt the data and write it to the network.

        // make sure we have an output buffer of the target size ready
        final int packetSize = getConnLink().getPacketBufferSize();
        if (numBytesLeft > packetSize) {
            // need >1 packet, do blocks of up to 2 at a time
            getEncryptedAppBuffer(packetSize * 2);
        } else {
            // one packet is fine
            getEncryptedAppBuffer(1);
        }

        try {
            final int cap = this.encryptedAppBuffer.capacity();
            int produced;

            do {
                // write distinct blocks of data to reduce excess overhead (for example, the
                // encrypted buffer must be continually expanded to fit the output data since
                // we can only hand 1 buffer to JSSE, which is expensive expanding and copying data)
                this.encryptedAppBuffer.clear();

                result = encryptMessage();
                numBytesLeft -= result.bytesConsumed();
                produced = result.bytesProduced();
                if (0 < produced) {
                    while (0 < numBytesLeft && (cap - produced) >= packetSize) {
                        // space in the current output buffer to encrypt a little more
                        // Note: JSSE requires a full packet size of open space regardless of the input
                        // amount, so even 100 bytes to encrypt requires 16K of output space
                        result = encryptMessage();
                        numBytesLeft -= result.bytesConsumed();
                        produced += result.bytesProduced();
                    }

                    this.encryptedAppBuffer.flip();
                    tcp.setBuffer(this.encryptedAppBuffer);
                    final long wrote = tcp.write(TCPWriteRequestContext.WRITE_ALL_DATA, timeout);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "wrote " + wrote);
                    }
                }

            } while (0 < numBytesLeft && 0 < produced);
        } catch (Exception original) {
            synchronized (closeSync) {
                // if close has been called then assume this exception was due to a race condition
                // with the close logic and consume the exception without FFDC. Otherwise rethrow
                // as the logic did before this change.
                if (closeCalled) {
                    IOException up = new IOException("Exception occurred while close detected" + original);
                    throw up;
                } else {
                    throw original;
                }
            }
        }

        final long rc = (maxBytes - numBytesLeft);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "write: " + rc);
        }
        return (rc);
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.TCPWriteRequestContext#write(long, com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback, boolean, int)
     */
    @Override
    public VirtualConnection write(long numBytes, TCPWriteCompletedCallback userCallback, boolean forceQueue, int timeout) {
        return write(numBytes, userCallback, forceQueue, timeout, false);
    }

    /**
     * See method above. Extra parameter tells if the request was from a formerly queued request.
     *
     * @param _numBytes
     * @param userCallback
     * @param forceQueue
     * @param timeout
     * @param fromQueue
     * @return VirtualConnection
     */
    protected VirtualConnection write(long _numBytes, TCPWriteCompletedCallback userCallback,
                                      boolean forceQueue, int timeout, boolean fromQueue) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "writeAsync, numBytes=" + _numBytes + ", timeout=" + timeout
                         + ", fromQueue=" + fromQueue + ", vc=" + getVCHash());
        }
        VirtualConnection vc = null;
        try {
            long numBytes = _numBytes;

            // Handle timing out of former read request.
            if (timeout == IMMED_TIMEOUT || timeout == ABORT_TIMEOUT) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Requested to timeout former request.  Calling device side.");
                }
                getConnLink().getDeviceWriteInterface().write(numBytes, this, forceQueue, timeout);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "writeAsync: " + getVC());
                }
                return getVC();
            }

            // Look for errors in the request.
            IOException exceptionInRequest = checkForErrors(numBytes, true);
            if (exceptionInRequest != null) {
                // Found an error.
                boolean fireHere = true;
                if (forceQueue) {
                    // Error must be returned on a separate thread.
                    queuedWork.setErrorParameters(getConnLink().getVirtualConnection(),
                                                  this, userCallback, exceptionInRequest);

                    EventEngine events = SSLChannelProvider.getEventService();
                    if (null == events) {
                        Exception e = new Exception("missing event admin");
                        FFDCFilter.processException(e, getClass().getName(), "172", this);
                        // fall-thru below and use callback here regardless
                    } else {
                        // fire an event to continue this queued work
                        Event event = events.createEvent(SSLEventHandler.TOPIC_QUEUED_WORK);
                        event.setProperty(SSLEventHandler.KEY_RUNNABLE, this.queuedWork);
                        events.postEvent(event);
                        fireHere = false;
                    }
                }
                if (fireHere) {
                    // Call the callback on this thread.
                    userCallback.error(getConnLink().getVirtualConnection(), this, exceptionInRequest);
                }
                // Return null indicating that the callback will handle the response.
                return null;
            }

            // Get the work on another thread if queuing is being forced.
            if (forceQueue) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Forcing write request to another thread, vc=" + getVCHash());
                }
                queuedWork.setWriteParameters(numBytes, userCallback, timeout);

                EventEngine events = SSLChannelProvider.getEventService();
                if (null == events) {
                    IOException e = new IOException("missing event admin");
                    FFDCFilter.processException(e, getClass().getName(), "471", this);
                    userCallback.error(getConnLink().getVirtualConnection(), this, e);
                } else {
                    // fire an event to continue this queued work
                    Event event = events.createEvent(SSLEventHandler.TOPIC_QUEUED_WORK);
                    event.setProperty(SSLEventHandler.KEY_RUNNABLE, this.queuedWork);
                    events.postEvent(event);
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "writeAsync: null");
                }
                return null;
            }

            callback = userCallback;

            // Adjust size of numBytes if all data is requested to be written (-1).
            if (numBytes == TCPWriteRequestContext.WRITE_ALL_DATA) {
                numBytes = WsByteBufferUtils.lengthOf(getBuffers());
            }

            SSLEngineResult sslResult = null;

            // Check if a handshake is needed.
            if (SSLUtils.isHandshaking(getConnLink().getSSLEngine())) {
                // A handshake is needed. Set the write parameters.
                handshakeCallback.setWriteParameters(numBytes, timeout);
                try {
                    sslResult = doHandshake(handshakeCallback);
                } catch (IOException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Caught exception during SSL handshake, " + e);
                    }
                    callback.error(getConnLink().getVirtualConnection(), this, e);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "writeAsynch: null");
                    }
                    return null;
                }
                // Check to see if handshake was done synchronously.
                if (sslResult != null) {
                    // Handshake was done synchronously. Verify results.
                    if (sslResult.getHandshakeStatus() != HandshakeStatus.FINISHED) {
                        IOException e = new IOException("Unable to complete SSLhandshake");
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Unable to complete SSLhandshake, " + e);
                        }
                        callback.error(getConnLink().getVirtualConnection(), this, e);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                            Tr.exit(tc, "writeAsynch: null");
                        }
                        return null;
                    }
                } else {
                    // Handshake is being handled asynchronously.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "writeAsynch: null");
                    }
                    return null;
                }
            }

            // Code can only get here if handshake wasn't needed or was done sync with valid return code.
            // Encrypt the data and write it to the network.
            vc = encryptAndWriteAsync(numBytes, false, timeout);

            // If data is ready, but this was from a formerly queued request, call the callback.
            if (vc != null && fromQueue) {
                callback.complete(vc, this);
                vc = null;
            }
        } catch (Exception original) {
            synchronized (closeSync) {
                // if close has been called then assume this exception was due to a race condition
                // with the close logic. so no FFDC here.
                if (closeCalled) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Cannot write, the link was already closed; vc=" + this.getVCHash());
                    }
                    return null;
                } else {
                    IOException ioe;
                    if (original instanceof IOException) {
                        ioe = (IOException) original;
                    }
                    ioe = new IOException("writeAsynch failed with exception: " + original.getMessage());
                    boolean fireHere = true;
                    if (forceQueue) {
                        // Error must be returned on a separate thread.
                        queuedWork.setErrorParameters(getConnLink().getVirtualConnection(),
                                                      this, userCallback, ioe);
                        EventEngine events = SSLChannelProvider.getEventService();
                        if (null != events) {
                            // fire an event to continue this queued work
                            Event event = events.createEvent(SSLEventHandler.TOPIC_QUEUED_WORK);
                            event.setProperty(SSLEventHandler.KEY_RUNNABLE, this.queuedWork);
                            events.postEvent(event);
                            fireHere = false;
                        }
                    }
                    if (fireHere) {
                        // Call the callback on this thread.
                        userCallback.error(getConnLink().getVirtualConnection(), this, ioe);
                    }
                    return null;
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "writeAsynch: " + vc);
        }
        return vc;
    }

    /**
     * Check the status of the buffers set by the caller taking into account
     * the JITAllocation size if the buffers are null or verifying there is
     * space available in the the buffers based on the size of data requested.
     *
     * @param numBytes
     * @param async
     * @return IOException if an inconsistency/error is found in the request,
     *         null otherwise.
     */
    private IOException checkForErrors(long numBytes, boolean async) {
        IOException exception = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "checkForErrors: numBytes=" + numBytes
                         + " buffers=" + SSLUtils.getBufferTraceInfo(getBuffers()));
        }

        // Extract the buffers provided by the calling channel.
        WsByteBuffer callerBuffers[] = getBuffers();
        if (callerBuffers == null || callerBuffers.length == 0) {
            exception = new IOException("No buffer(s) provided for writing data.");
        } else if ((numBytes < -1) || (numBytes == 0) && (async)) {
            // NumBytes requested must be -1 (write all) or positive
            exception = new IOException("Number of bytes requested, " + numBytes + " is not valid.");
        } else {
            // Ensure buffer provided by caller is big enough to contain the number of bytes requested.
            int bytesAvail = WsByteBufferUtils.lengthOf(callerBuffers);
            if (bytesAvail < numBytes) {
                exception = new IOException("Number of bytes requested, "
                                            + numBytes + " exceeds space remaining in the buffers provided: "
                                            + bytesAvail);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && exception != null) {
            Tr.debug(tc, "Found error, exception generated: " + exception);
        }
        return exception;
    }

    /**
     * This method is called as a part of an asynchronous write, but after a
     * potential SSL handshake has taken place.
     *
     * @param numBytes
     * @param forceQueue
     * @param timeout
     * @return virtual connection
     */
    public VirtualConnection encryptAndWriteAsync(long numBytes, boolean forceQueue, int timeout) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "encryptAndWriteAsync: " + numBytes);
        }

        this.asyncBytesToWrite = 0L;
        this.asyncTimeout = timeout;
        VirtualConnection vc = null;

        try {
            long numBytesLeft = numBytes;
            // make sure we have an output buffer of the target size ready
            final int packetSize = getConnLink().getPacketBufferSize();
            if (numBytesLeft > packetSize) {
                getEncryptedAppBuffer(packetSize * 2);
            } else {
                getEncryptedAppBuffer(1);
            }
            final int cap = this.encryptedAppBuffer.capacity();
            final TCPWriteRequestContext tcp = getConnLink().getDeviceWriteInterface();
            SSLEngineResult result;
            int produced;

            do {
                this.encryptedAppBuffer.clear();

                result = encryptMessage();
                numBytesLeft -= result.bytesConsumed();
                produced = result.bytesProduced();
                if (0 < produced) {
                    while (0 < numBytesLeft && (cap - produced) >= packetSize) {
                        // space in the current output buffer to encrypt a little more
                        // Note: JSSE requires a full packet size of open space regardless of the input
                        // amount, so even 100 bytes to encrypt requires 16K of output space
                        result = encryptMessage();
                        numBytesLeft -= result.bytesConsumed();
                        produced += result.bytesProduced();
                    }

                    // in case this goes async, save bytes-left to pick up later
                    this.asyncBytesToWrite = numBytesLeft;
                    this.encryptedAppBuffer.flip();
                    tcp.setBuffer(this.encryptedAppBuffer);
                    vc = tcp.write(TCPWriteRequestContext.WRITE_ALL_DATA, this, forceQueue, timeout);
                }
            } while (null != vc && 0 < numBytesLeft && 0 < produced);

        } catch (IOException exception) {
            // No FFDC needed. Callback will handle.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception during encryption, " + exception);
            }
            this.callback.error(getConnLink().getVirtualConnection(), this, exception);
        } catch (Exception original) {
            synchronized (closeSync) {
                // if close has been called then assume this exception was due to a race condition
                // with the close logic. so no FFDC here.
                if (closeCalled) {
                    return null;
                } else {
                    throw original;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "encryptAndWriteAsync: " + vc);
        }
        return vc;
    }

    /**
     * This callback will be used as a feedback mechanism for calls to handleHandshake
     * from encryptMessage.
     */
    public class MyHandshakeCompletedCallback implements SSLHandshakeCompletedCallback {
        /** Underlying write context */
        private final TCPWriteRequestContext writeContext;
        /** Number of bytes to write */
        private long numBytes;
        /** Timeout to use during a write */
        private int timeout;
        /** Network level buffer to use for wraps in handshake */
        private WsByteBuffer netBuffer;
        /** Decrypted data buffer... used for unwrap calls in handshake */
        private WsByteBuffer decryptedNetBuffer;
        /** allow other code to tell this class if they changed netBuffer */
        private WsByteBuffer updatedNetBuffer = null;

        /**
         * Constructor.
         *
         * @param _writeContext
         */
        public MyHandshakeCompletedCallback(TCPWriteRequestContext _writeContext) {
            this.writeContext = _writeContext;
        }

        @Override
        public void updateNetBuffer(WsByteBuffer newBuffer) {
            netBuffer = newBuffer;
            updatedNetBuffer = newBuffer;
        }

        @Override
        public WsByteBuffer getUpdatedNetBuffer() {
            return updatedNetBuffer;
        }

        /**
         * Set the write parameters to use later.
         *
         * @param _numBytes
         * @param _timeout
         */
        public void setWriteParameters(long _numBytes, int _timeout) {
            this.numBytes = _numBytes;
            this.timeout = _timeout;
        }

        /**
         * Set the network buffer to use later.
         *
         * @param buffer
         */
        public void setNetBuffer(WsByteBuffer buffer) {
            this.netBuffer = buffer;
        }

        /**
         * Set the buffer to decrypt the message.
         *
         * @param buffer
         */
        public void setDecryptedNetBuffer(WsByteBuffer buffer) {
            this.decryptedNetBuffer = buffer;
        }

        /*
         * @see com.ibm.ws.channel.ssl.internal.SSLHandshakeCompletedCallback#complete(javax.net.ssl.SSLEngineResult)
         */
        @Override
        public void complete(SSLEngineResult sslResult) {
            // Release buffers used in the handshake.
            netBuffer.release();
            netBuffer = null;
            decryptedNetBuffer.release();
            decryptedNetBuffer = null;
            if (sslResult.getHandshakeStatus() != HandshakeStatus.FINISHED) {
                // Invalid return code from SSL handshake
                IOException e = new IOException("Unable to complete SSLhandshake");
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unable to complete SSLhandshake, " + e);
                }
                FFDCFilter.processException(e, getClass().getName(), "245", this);
                callback.error(getConnLink().getVirtualConnection(), writeContext, e);
            } else {
                VirtualConnection vc = encryptAndWriteAsync(numBytes, false, timeout);
                if (vc != null) {
                    callback.complete(vc, writeContext);
                }
            }
        }

        /*
         * @see com.ibm.ws.channel.ssl.internal.SSLHandshakeCompletedCallback#error(java.io.IOException)
         */
        @Override
        public void error(IOException ioe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception during encryption, " + ioe);
            }
            // Release buffers used in the handshake.
            netBuffer.release();
            netBuffer = null;
            decryptedNetBuffer.release();
            decryptedNetBuffer = null;
            callback.error(getConnLink().getVirtualConnection(), writeContext, ioe);
        }
    }

    /**
     * When a write is attempted, a first check is done to see if the SSL engine
     * needs to do a handshake. If so, this method will be called. Note, it is
     * used by both the sync and async writes.
     *
     * @param hsCallback callback for use by async write, null for sync write
     * @return result of the handshake
     * @throws IOException
     */
    private SSLEngineResult doHandshake(MyHandshakeCompletedCallback hsCallback) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "doHandshake");
        }
        SSLEngineResult sslResult;
        SSLEngine sslEngine = getConnLink().getSSLEngine();
        // Line up all the buffers needed for the SSL handshake. Temporary so use
        // indirect allocation for speed.
        WsByteBuffer netBuffer = SSLUtils.allocateByteBuffer(
                                                             sslEngine.getSession().getPacketBufferSize(), true);
        WsByteBuffer decryptedNetBuffer = SSLUtils.allocateByteBuffer(
                                                                      sslEngine.getSession().getApplicationBufferSize(), false);
        try {
            getEncryptedAppBuffer(1);
        } catch (IOException up) {
            // Release buffers used in the handshake.
            netBuffer.release();
            decryptedNetBuffer.release();
            throw up;
        }

        if (hsCallback != null) {
            // If the callback is non null, update the callback with the buffers it
            // will have to release.
            hsCallback.setNetBuffer(netBuffer);
            hsCallback.setDecryptedNetBuffer(decryptedNetBuffer);
        }
        try {
            // Do the SSL handshake. Note, if synchronous the handshakeCallback is null.
            sslResult = SSLUtils.handleHandshake(
                                                 getConnLink(),
                                                 netBuffer,
                                                 decryptedNetBuffer,
                                                 this.encryptedAppBuffer,
                                                 null,
                                                 hsCallback,
                                                 false);

            if (hsCallback.getUpdatedNetBuffer() != null) {
                netBuffer = hsCallback.getUpdatedNetBuffer();
            }

        } catch (IOException e) {
            // Release buffers used in the handshake.
            if (hsCallback.getUpdatedNetBuffer() != null) {
                netBuffer = hsCallback.getUpdatedNetBuffer();
            }
            netBuffer.release();
            decryptedNetBuffer.release();
            throw e;
        } catch (ReadOnlyBufferException robe) {
            // Release buffers used in the handshake.
            if (hsCallback.getUpdatedNetBuffer() != null) {
                netBuffer = hsCallback.getUpdatedNetBuffer();
            }
            netBuffer.release();
            decryptedNetBuffer.release();
            throw new IOException("Caught exception during handshake: " + robe.getMessage(), robe);
        }
        if (sslResult != null) {
            // Handshake was done synchronously.
            // Release buffers used in the handshake.
            netBuffer.release();
            decryptedNetBuffer.release();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "doHandshake: " + sslResult);
        }
        return sslResult;
    }

    /**
     * Handle common activity of write and writeAsynch involving the encryption of the
     * current buffers. The caller will have the responsibility of writing them to the
     * device side channel.
     *
     * @return SSLEngineResult
     * @throws IOException
     */
    private SSLEngineResult encryptMessage() throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "encryptMessage");
        }

        // Get the application buffer used as input to the encryption algorithm.
        // Extract the app buffers containing data to be written.
        final WsByteBuffer[] appBuffers = getBuffers();
        SSLEngineResult result;

        while (true) {
            // Protect JSSE from potential SSL packet sizes that are too big.
            int[] appLimitInfo = SSLUtils.adjustBuffersForJSSE(
                                                               appBuffers, getConnLink().getAppBufferSize());

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "before wrap: appBuffers: "
                             + SSLUtils.getBufferTraceInfo(appBuffers)
                             + "\r\n\tencAppBuf: " + SSLUtils.getBufferTraceInfo(encryptedAppBuffer));
            }

            // Call the SSL engine to encrypt the request.
            result = getConnLink().getSSLEngine().wrap(
                                                       SSLUtils.getWrappedByteBuffers(appBuffers),
                                                       encryptedAppBuffer.getWrappedByteBuffer());

            // Check the result of the call to wrap.
            Status status = result.getStatus();
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "after wrap: appBuffers: "
                             + SSLUtils.getBufferTraceInfo(appBuffers)
                             + "\r\n\tencAppBuf: " + SSLUtils.getBufferTraceInfo(encryptedAppBuffer)
                             + "\r\n\tstatus=" + status
                             + " consumed=" + result.bytesConsumed()
                             + " produced=" + result.bytesProduced());
            }

            // If a limit modification was saved, restore it.
            if (appLimitInfo != null) {
                SSLUtils.resetBuffersAfterJSSE(appBuffers, appLimitInfo);
            }

            if (status == Status.OK) {
                break;
            }

            else if (status == Status.BUFFER_OVERFLOW) {
                // The output buffers provided to the SSL engine were not big enough. A bigger buffer
                // must be supplied. If we can build a bigger buffer and call again, build it.
                increaseEncryptedBuffer();
                continue;
            }

            else {
                throw new IOException("Unable to encrypt data, status=" + status);
            }
        } // end of while

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "encryptMessage: " + result.getStatus());
        }
        return result;
    }

    /**
     * Reuse the buffers used to send data out to the network. If existing buffer
     * is available, grow the array by one.
     *
     * @throws IOException - if allocation failed
     */
    protected void increaseEncryptedBuffer() throws IOException {
        final int packetSize = getConnLink().getPacketBufferSize();
        if (null == this.encryptedAppBuffer) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Allocating encryptedAppBuffer, size=" + packetSize);
            }
            this.encryptedAppBuffer = SSLUtils.allocateByteBuffer(
                                                                  packetSize, getConfig().getEncryptBuffersDirect());
        } else {
            // The existing buffer isn't big enough, add another packet size to it
            final int cap = this.encryptedAppBuffer.capacity();
            final int newsize = cap + packetSize;
            if (0 > newsize) {
                // wrapped over max-int
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Unable to increase encrypted buffer beyond " + cap);
                }
                throw new IOException("Unable to increase buffer beyond " + cap);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Increasing encryptedAppBuffer to " + newsize);
            }
            WsByteBuffer temp = SSLUtils.allocateByteBuffer(
                                                            newsize, this.encryptedAppBuffer.isDirect());
            this.encryptedAppBuffer.flip();
            SSLUtils.copyBuffer(this.encryptedAppBuffer, temp, this.encryptedAppBuffer.remaining());
            this.encryptedAppBuffer.release();
            this.encryptedAppBuffer = temp;
        }
    }

    /**
     * Make sure that an output buffer is ready for encryption use. This will always
     * allocate a minimum of the current SSLSession packet size.
     *
     * @param requested_size
     */
    private void getEncryptedAppBuffer(int requested_size) throws IOException {

        final int size = Math.max(getConnLink().getPacketBufferSize(), requested_size);

        synchronized (closeSync) {
            if (closeCalled) {
                IOException up = new IOException("Operation failed due to connection close detected");
                throw up;
            }

            if (null != this.encryptedAppBuffer) {
                if (size <= this.encryptedAppBuffer.capacity()) {
                    // current buffer exists and is big enough
                    this.encryptedAppBuffer.clear();
                    return;
                }
                // exists but is too small
                this.encryptedAppBuffer.release();
                this.encryptedAppBuffer = null;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Allocating encryptedAppBuffer, size=" + size);
            }
            // Allocate the encrypted data buffer
            this.encryptedAppBuffer = SSLUtils.allocateByteBuffer(
                                                                  size, getConfig().getEncryptBuffersDirect());
        }
    }

    /**
     * Release the potential input buffer that was created during encryption.
     */
    public void close() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "close");
        }

        synchronized (closeSync) {
            if (closeCalled) {
                return;
            }

            closeCalled = true;

            // Release the buffer used to store results of encryption, and given to
            // device channel for writing.
            if (null != this.encryptedAppBuffer) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Releasing ssl output buffer during close. "
                                 + SSLUtils.getBufferTraceInfo(this.encryptedAppBuffer));
                }
                this.encryptedAppBuffer.release();
                this.encryptedAppBuffer = null;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "close");
        }
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback#complete(com.ibm.wsspi.channelfw.VirtualConnection, com.ibm.wsspi.tcpchannel.TCPWriteRequestContext)
     */
    @Override
    public void complete(VirtualConnection vc, TCPWriteRequestContext wsc) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "complete, vc=" + getVCHash());
        }

        synchronized (closeSync) {
            if (closeCalled) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "complete");
                }
                return;
            }
        }

        VirtualConnection rc = vc;
        if (0 < this.asyncBytesToWrite) {
            // previously went async partly through the app data, continue now
            rc = encryptAndWriteAsync(this.asyncBytesToWrite, false, this.asyncTimeout);
        }

        if (null != rc) {
            // Nothing else needs to be done here. Report success up the chain.
            this.callback.complete(rc, this);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "complete");
        }
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback#error(com.ibm.wsspi.channelfw.VirtualConnection, com.ibm.wsspi.tcpchannel.TCPWriteRequestContext,
     * java.io.IOException)
     */
    @Override
    public void error(VirtualConnection vc, TCPWriteRequestContext wsc, IOException ioe) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "error, vc=" + getVCHash());
        }
        // Nothing else needs to be done here. Report error up the chain.
        this.asyncBytesToWrite = 0L;
        this.callback.error(vc, this, ioe);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "error");
        }
    }

    /**
     * This class is used to handle an asynchronous write that was forced to be queued.
     */
    private class QueuedWork implements Runnable {
        /** Number of bytes to write */
        private long numBytes = 0L;
        /** App level callback to use when done */
        private TCPWriteCompletedCallback userCallback = null;
        /** Timeout to use during write */
        private int timeout = 0;
        /** Connection object, used on error paths */
        private VirtualConnection vc = null;
        /** Underlying TCP write context reference */
        private TCPWriteRequestContext tcpWriteRequestContext = null;
        /** Exception seen during error path */
        private IOException exception = null;
        /** Whether queued work is a write or an error */
        private boolean isWrite = true;

        /**
         * Empty constructor. Important methods are below for setting parameters. This
         * object is reused to save on object creation which is why separate constructors
         * were not created to solve this problem.
         */
        protected QueuedWork() {
            // nothing to do
        }

        /**
         * Set the parameters to be used for the next piece of write work.
         *
         * @param _numBytes
         * @param _userCallback
         * @param _timeout
         */
        public void setWriteParameters(long _numBytes, TCPWriteCompletedCallback _userCallback, int _timeout) {
            this.numBytes = _numBytes;
            this.userCallback = _userCallback;
            this.timeout = _timeout;
            this.isWrite = true;
        }

        /**
         * Set the parameters to be used for the next piece of error work.
         *
         * @param _vc
         * @param _tcpWriteRequestContext
         * @param _userCallback
         * @param _exception
         */
        public void setErrorParameters(VirtualConnection _vc,
                                       TCPWriteRequestContext _tcpWriteRequestContext,
                                       TCPWriteCompletedCallback _userCallback,
                                       IOException _exception) {
            this.vc = _vc;
            this.tcpWriteRequestContext = _tcpWriteRequestContext;
            this.userCallback = _userCallback;
            this.exception = _exception;
            this.isWrite = false;
        }

        /**
         * This method will be called by a separate thread to actually do the work.
         */
        @Override
        public void run() {
            // Do the work represented by the action.
            if (this.isWrite) {
                // Do the write on this separate thread. Note last parameter shows this is called
                // from a queued thread.
                write(numBytes, userCallback, false, timeout, true);
            } else {
                // Call the error callback.
                userCallback.error(vc, tcpWriteRequestContext, exception);
            }
        }
    }
}
