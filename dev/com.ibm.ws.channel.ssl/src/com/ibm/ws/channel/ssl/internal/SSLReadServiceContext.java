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

import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;

import com.ibm.websphere.event.Event;
import com.ibm.websphere.event.EventEngine;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channel.ssl.internal.exception.ReadNeededInternalException;
import com.ibm.ws.channel.ssl.internal.exception.SessionClosedException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferUtils;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;
import com.ibm.wsspi.tcpchannel.TCPRequestContext;

/**
 * SSL Channel's TCPReadRequestContext implementation.
 */
public class SSLReadServiceContext extends SSLBaseServiceContext implements TCPReadRequestContext {

    /** Trace component for WAS */
    protected static final TraceComponent tc = Tr.register(SSLReadServiceContext.class,
                                                           SSLChannelConstants.SSL_TRACE_NAME,
                                                           SSLChannelConstants.SSL_BUNDLE);

    /** Callback from app channel used to notify when a the read request is complete. */
    protected TCPReadCompletedCallback callback = null;
    /** Flag if the calling channel requires the output buffer to be allocated by us. Caller will have to release. */
    private boolean callerRequiredAllocation = false;
    /** Size of the buffer that should be allocated for (1) buffer to return to caller, (2) buffer size for TCP. */
    private int jITAllocateSize = 0;
    /** Buffer given to device side channel to read in data. */
    protected WsByteBuffer netBuffer = null;
    /** Buffer given used to write output of ssl engine. Either allocated here or provided by app channel. */
    private WsByteBuffer[] decryptedNetBuffers = null;
    /** Temporary location to save limits of the decryptedNetBuffer before calling unwrap which may modify them. */
    private int[] decryptedNetLimitInfo = null;
    /** Starting positions for decrypt buffers */
    private int[] decryptedNetPosInfo = new int[1];
    /** Buffer array containing decrypted data from previous read. Wasn't room to copy the results into caller supplied buffers. */
    private WsByteBuffer[] unconsumedDecData = null;
    /** Flag to indicate we allocated a decryptedNetworkBuffer that we must release. */
    private boolean decryptedNetBufferReleaseRequired = false;
    /** Read interface of the device side channel for doing reads. */
    protected TCPReadRequestContext deviceReadContext;
    /** Track bytes produced by a decryption. */
    protected long bytesProduced = 0L;
    /** Track bytes requested on a read. */
    protected long bytesRequested = 0L;
    /** Track begining of where data should be read into netBuffer. */
    protected int netBufferMark = 0;
    /** Work that is queued. */
    private QueuedWork queuedWork = null;
    /** Reusable read callback. */
    private SSLReadCompletedCallback readCallback = null;
    /** Reusable exception used for intra function communication. */
    private ReadNeededInternalException readNeededInternalException = null;
    /** Reusable exception used for intra function communication. */
    private SessionClosedException sessionClosedException = null;

    private final Object closeSync = new Object();
    private boolean closeCalled = false;

    /**
     * Constructor.
     *
     * @param connLink
     */
    public SSLReadServiceContext(SSLConnectionLink connLink) {
        super(connLink);
        this.queuedWork = new QueuedWork();
        this.readCallback = new SSLReadCompletedCallback(this);
        this.readNeededInternalException = new ReadNeededInternalException("All available data read, but more needed, read again");
        this.sessionClosedException = new SessionClosedException("SSL engine is closed");
    }

    /**
     * Save the starting positions of the output buffers so that we can properly
     * calculate the amount of data being returned by the read.
     *
     */
    private void saveDecryptedPositions() {
        for (int i = 0; i < decryptedNetPosInfo.length; i++) {
            decryptedNetPosInfo[i] = 0;
        }
        if (null != getBuffers()) {
            WsByteBuffer[] buffers = getBuffers();
            if (buffers.length > decryptedNetPosInfo.length) {
                decryptedNetPosInfo = new int[buffers.length];
            }
            for (int i = 0; i < buffers.length && null != buffers[i]; i++) {
                decryptedNetPosInfo[i] = buffers[i].position();
            }
        }
    }

    /**
     * Constructor used for test purposes only.
     */
    public SSLReadServiceContext() {
        // nothing to do
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.TCPReadRequestContext#read(long, int)
     */
    @Override
    public long read(long numBytes, int timeout) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "read, numbytes=" + numBytes + ", vc=" + getVCHash());
        }

        // Access the read interface of the device side channel.
        if (deviceReadContext == null) {
            deviceReadContext = getConnLink().getDeviceReadInterface();
        }

        // Handle timing out of former read request.
        if (timeout == IMMED_TIMEOUT || timeout == ABORT_TIMEOUT) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Requested to timeout former request.  Calling device side.");
            }
            deviceReadContext.read(numBytes, timeout);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "read");
            }
            return 0L;
        }

        // Reset some object variables.
        this.decryptedNetBufferReleaseRequired = false;
        this.callerRequiredAllocation = false;

        // Look for errors in the request.
        IOException exceptionInRequest = checkRequest(numBytes, false);
        if (exceptionInRequest != null) {
            // Found an error.
            throw exceptionInRequest;
        }

        // Track the number of bytes requested.
        this.bytesRequested = numBytes;

        // save the starting positions of the buffers to make a proper return value later
        saveDecryptedPositions();

        // Handle any left over data from a the previous read request.
        this.bytesProduced = readUnconsumedDecData();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Decrypted data left over from previous request: " + bytesProduced);
        }
        // Was data read to satisfy the request or was the request to get all data available?
        if ((bytesRequested > bytesProduced) || (bytesRequested == 0L)) {
            // Left over data didn't exist or was not enough to satisfy the request.
            long devBytesRead = 0;
            int loopCount = 0;
            long encryptedBytesAvailable = 0;
            long bufferSize = 0;

            // If bytesProduced > 0, then some date was left over in unconsumedDecData but it
            // wasn't enough. The data is currently between pos and lim, but now we need to
            // treat it like data that was already decrypted by the JSSE, pushing the data
            // behind the position.
            if (bytesProduced > 0L) {
                SSLUtils.positionToLimit(decryptedNetBuffers);
                // While we're at it, maximize the room for future reads.
                SSLUtils.limitToCapacity(decryptedNetBuffers);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Adjusted left over decNetBuffers: "
                                 + SSLUtils.getBufferTraceInfo(decryptedNetBuffers));
                }
            }

            // Calculate how much encrypted data is currently available if netbuffers already exists.
            if (null != this.netBuffer) {
                encryptedBytesAvailable = this.netBuffer.remaining();
            }
            // Adjust the buffer size if the number of bytes request was zero (read all).
            if (bytesRequested != 0L) {
                bufferSize = bytesRequested - bytesProduced;
            }
            // Allocate/reuse the buffer to be read into. Data remaining from a previous,
            // unconsumed read, will be put in these buffers before return. Note the size
            // provided is a guess. bytesToRead is decNet data (decrypted). This buffer
            // is for net data (encrypted).
            getNetworkBuffer(bufferSize);

            // Keep reading until enough data has been read to satisfy the request.
            while ((bytesProduced < bytesRequested) || (bytesRequested == 0L)) {
                // Adjust counter to know when first pass of loop took place.
                loopCount++;

                // Bypass a new read if data is available and this is the first loop iteration.
                // Future loop will require a read since as the JSSE's UNDERFLOW will indicate.
                if ((encryptedBytesAvailable > 0) && (loopCount == 1)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "No read needed.  Encrypted bytes already available: "
                                     + encryptedBytesAvailable);
                    }
                } else {
                    // Shift limit to capacity for all buffers to maximize room to read,
                    // reducing allocations.
                    this.netBuffer.limit(this.netBuffer.capacity());
                    long deviceReadRequestSize = 0;
                    if (bytesRequested != 0L && bytesRequested > bytesProduced) {
                        // Attempt to do as much reading as possible up front.
                        deviceReadRequestSize = bytesRequested - bytesProduced - encryptedBytesAvailable;
                        // PK32916 - 0 bytes is not valid in this case so protect against that
                        // boundary condition
                        if (0L >= deviceReadRequestSize) {
                            deviceReadRequestSize = 1;
                        }
                    } else if (bytesRequested == 0L) {
                        deviceReadRequestSize = 0L;
                    } else {
                        // Remember bytesRequested are decrypted bytes.
                        // devBytesRead are encrypted bytes. This case can and
                        // will happen so ensure some reading gets done. Set to 1.
                        deviceReadRequestSize = 1;
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "do sync read of : " + deviceReadRequestSize
                                     + " bytes into netBuffer..." + SSLUtils.getBufferTraceInfo(this.netBuffer));
                    }
                    try {
                        devBytesRead += deviceReadContext.read(deviceReadRequestSize, timeout);
                        if (deviceReadRequestSize == 0L && devBytesRead == 0L) {
                            // A request was made to read all available, and nothing was.
                            this.bytesProduced = 0L;

                            // Reset the buffers to contain previously read data between pos and lim.
                            this.netBuffer.limit(this.netBuffer.position());
                            this.netBuffer.position(netBufferMark);
                            break;
                        }
                    } catch (IOException e) {
                        // no FFDC required
                        // Protect future reads from thinking data was read.
                        // Reset the buffers to contain previously read data between pos and lim.
                        this.netBuffer.limit(this.netBuffer.position());
                        this.netBuffer.position(netBufferMark);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Reset buffers after read error: vc=" + getVCHash()
                                         + ", netBuffer: " + SSLUtils.getBufferTraceInfo(this.netBuffer));
                        }
                        throw e;
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "read bytes (total): " + devBytesRead);
                    }
                }

                // Prepare the input buffer for calling unwrap.
                // Set the position to the mark, noting the beginning of the data just read.
                this.netBuffer.limit(this.netBuffer.position());
                this.netBuffer.position(netBufferMark);

                // decrypt the resulting message.
                Exception exception = decryptMessage(false);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, bytesProduced + " bytes produced of "
                                 + bytesRequested + " bytes requested");
                }

                // Calculate how much encrypted data is still available, for use in future iterations.
                encryptedBytesAvailable = this.netBuffer.remaining();

                if (exception == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Decryption succeeded.");
                    }
                    // Check if more reading is necessary.
                    if (0L == this.bytesRequested) {
                        break;
                    }
                    if (bytesProduced < bytesRequested) {
                        getNetworkBuffer(bytesRequested - bytesProduced);
                    }
                }

                else if (exception instanceof ReadNeededInternalException) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "More data needs to be read, loop to another read");
                    }
                    if (0L == this.bytesRequested) {
                        break;
                    }
                    getNetworkBuffer(this.bytesRequested - this.bytesProduced);
                    continue;
                }

                else if (exception instanceof SessionClosedException) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "SSL Session has been closed.");
                    }
                    // 238579 - inform channel above
                    throw new IOException("SSL connection was closed by peer");
                }

                else {
                    FFDCFilter.processException(exception, getClass().getName(), "118", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Decryption unsuccessful, exception resulted: " + exception);
                    }
                    IOException exc = new IOException("Unable to decrypt message");
                    exc.initCause(exception);
                    throw exc;
                }
            } // end while
        }
        // A valid amount of data has been produced.
        if (this.bytesProduced > 0L) {
            prepareDataForNextChannel();
            // now figure out how much we're actually sending to the caller
            WsByteBuffer[] buffers = getBuffers();
            long count = 0L;
            for (int i = 0; i < buffers.length && count < this.bytesProduced; i++) {
                count += buffers[i].limit() - decryptedNetPosInfo[i];
            }
            this.bytesProduced = count;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "read: " + this.bytesProduced);
        }
        return this.bytesProduced;
    }

    /**
     * Note, a separate thread is not spawned to handle the decryption. The asynchronous
     * behavior of this call will take place when the device side channel makes a
     * nonblocking IO call and the request is potentially moved to a separate thread.
     *
     * The buffers potentially set from the calling application will be used to store the
     * output of the decrypted message. No read buffers are set in the device channel. It
     * will have the responsibility of allocating while we will have the responsibility of
     * releasing.
     *
     * @see com.ibm.wsspi.tcpchannel.TCPReadRequestContext#read(long, TCPReadCompletedCallback, boolean, int)
     */
    @Override
    public VirtualConnection read(long numBytes, TCPReadCompletedCallback userCallback, boolean forceQueue, int timeout) {
        // Call the async read with a flag showing this was not done from a queued request.
        return read(numBytes, userCallback, forceQueue, timeout, false);
    }

    /**
     * See method above. Extra parameter tells if the request was from a formerly queued request.
     *
     * @param numBytes
     * @param userCallback
     * @param forceQueue
     * @param timeout
     * @param fromQueue
     * @return VirtualConnection
     */
    protected VirtualConnection read(long numBytes, TCPReadCompletedCallback userCallback, boolean forceQueue, int timeout, boolean fromQueue) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "readAsynch, numBytes=" + numBytes + ", fromQueue=" + fromQueue + ", vc=" + getVCHash());
        }

        // Access the read interface of the device side channel.
        if (deviceReadContext == null) {
            deviceReadContext = getConnLink().getDeviceReadInterface();
        }

        // Handle timing out of former read request.
        if (timeout == IMMED_TIMEOUT || timeout == ABORT_TIMEOUT) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Requested to timeout former request.  Calling device side.");
            }

            if (userCallback != null) {
                //If this is called to timeout a previous request to read the original callback should be used
                //The original callback would have been set on the first call to the read
                readCallback.setCallBack(userCallback);
            }

            deviceReadContext.read(numBytes, readCallback, forceQueue, timeout);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "readAsynch: " + getVC());
            }
            return getVC();
        }

        // Reset some object variables.
        this.decryptedNetBufferReleaseRequired = false;
        this.callerRequiredAllocation = false;

        // Look for errors in the request.
        IOException exceptionInRequest = checkRequest(numBytes, true);
        if (exceptionInRequest != null) {
            handleAsyncError(forceQueue, exceptionInRequest, userCallback);
            // Return null indicating that the callback will handle the response.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "readAsynch: null");
            }
            return null;
        }

        callback = userCallback;
        VirtualConnection vc = null;
        // Track the number of bytes requested.
        bytesRequested = numBytes;
        // First handle any left over data from a the previous read request.
        bytesProduced = readUnconsumedDecData();
        long bytesToRead = bytesRequested - bytesProduced;
        boolean requestSatisfied = false;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Data left over from previous request: " + bytesProduced);
        }

        // Was enough data read to satisfy the request?
        if (bytesRequested > bytesProduced || bytesRequested == 0L) {
            // Left over data didn't exist or was not to satisfy the request.
            // Access the read interface of the device side channel.
            if (deviceReadContext == null) {
                deviceReadContext = getConnLink().getDeviceReadInterface();
            }

            // If bytesProduced > 0, then some date was left over in unconsumedDecData but it
            // wasn't enough. The data is currently between pos and lim, but now we need to
            // treat it like data that was already decrypted by the JSSE, pushing the data
            // behind the position.
            if (bytesProduced > 0L) {
                SSLUtils.positionToLimit(decryptedNetBuffers);
                // While we're at it, maximize the room for future reads.
                SSLUtils.limitToCapacity(decryptedNetBuffers);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Adjusted left over decNetBuffers: "
                                 + SSLUtils.getBufferTraceInfo(decryptedNetBuffers));
                }
            }

            // See if there is data in a device side network buffer already.
            // This can happen when a handshake occurs and data is left in the buffer encrypted.
            WsByteBuffer deviceBuffer = deviceReadContext.getBuffer();
            if (null != deviceBuffer && 0 != deviceBuffer.position() && 0 != deviceBuffer.remaining()) {
                // Use this buffer. Note, we have the responsibility to free it in close().
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found data in existing network buffer, "
                                 + SSLUtils.getBufferTraceInfo(deviceBuffer));
                }
                this.netBuffer = deviceBuffer;
                // Don't mess with this netBuffer until entering decryptMessage. Position is not zero.
                vc = getConnLink().getVirtualConnection();
            } else {
                // Allocate/reuse the buffer to be read into. Data remaining from a previous,
                // unconsumed read, will be put in these buffers before return. Note the size
                // provided is a guess. bytesToRead is decNet data (decrypted). This buffer
                // is for net data (encrypted).
                if (bytesRequested == 0L) {
                    bytesToRead = 0L;
                } else {
                    bytesToRead = bytesRequested - bytesProduced;
                }
                getNetworkBuffer(bytesToRead);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "do async read of : " + bytesToRead + " bytes");
                }
                readCallback.setCallBack(userCallback);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Calling device side read with netBuffers, "
                                 + SSLUtils.getBufferTraceInfo(this.netBuffer));
                }
                vc = deviceReadContext.read(bytesToRead, readCallback, forceQueue, timeout);
                // This buffer needs to be flipped before entering decryptMessage.
                if (vc != null) {
                    this.netBuffer.limit(this.netBuffer.position());
                    this.netBuffer.position(netBufferMark);
                }
            }
        } else {
            // Left over data was enough to satisfy the request.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Left over data was enough to satisfy the request.");
            }
            vc = getConnLink().getVirtualConnection();
            requestSatisfied = true;
            prepareDataForNextChannel();
            // Don't mess with this buffer until entering decryptMessage. Position is not zero.
        }

        // Determine if the callback will handle the result.
        if (!requestSatisfied && vc != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Data is ready, no callback necessary.");
            }
            Exception exception = null;
            boolean bCont;
            do {
                bCont = false;
                exception = decryptMessage(true);
                if (exception == null) {
                    prepareDataForNextChannel();
                } else if (exception instanceof ReadNeededInternalException) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "More data needs to be read. vc=" + getVCHash());
                    }
                    getNetworkBuffer(1);
                    readCallback.setCallBack(userCallback);
                    vc = deviceReadContext.read(1, readCallback, forceQueue, timeout);
                    // This buffer needs to be flipped before entering decryptMessage.
                    if (vc != null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Read done. No callback necessary, buffers "
                                         + SSLUtils.getBufferTraceInfo(this.netBuffer));
                        }
                        this.netBuffer.limit(this.netBuffer.position());
                        this.netBuffer.position(netBufferMark);
                        bCont = true;
                    }
                } else if (exception instanceof SessionClosedException) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "SSL Session has been closed.");
                    }
                    handleAsyncError(forceQueue,
                                     new IOException("SSL connection closed by peer"), userCallback);
                    vc = null;
                } else {
                    FFDCFilter.processException(exception, getClass().getName(), "192", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Caught exception during unwrap, " + exception);
                    }
                    IOException ioe = new IOException("SSL decryption failed");
                    ioe.initCause(exception);
                    handleAsyncError(forceQueue, ioe, userCallback);
                    vc = null;
                }
            } while (bCont);
        }

        // If data is ready and
        // the request came from a callback (so the callback must be used to notify of complete) or
        // the request has requested to have its response on a separate thread (forceQueue == true)
        if ((vc != null) && (fromQueue || forceQueue)) {
            handleAsyncComplete(forceQueue, callback);
            // Mark that the response was given to the channel above already by setting vc to null.
            vc = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "readAsynch: " + vc);
        }
        return vc;
    }

    /**
     * This method handles calling the complete method of the callback as required by an async
     * read. Appropriate action is taken based on the setting of the forceQueue parameter.
     * If it is true, the complete callback is called on a separate thread. Otherwise it is
     * called right here.
     *
     * @param forceQueue
     * @param inCallback
     */
    private void handleAsyncComplete(boolean forceQueue, TCPReadCompletedCallback inCallback) {
        boolean fireHere = true;
        if (forceQueue) {
            // Complete must be returned on a separate thread.
            // Reuse queuedWork object (performance), but reset the error parameters.
            queuedWork.setCompleteParameters(getConnLink().getVirtualConnection(), this, inCallback);

            EventEngine events = SSLChannelProvider.getEventService();
            if (null == events) {
                Exception e = new Exception("missing event service");
                FFDCFilter.processException(e, getClass().getName(), "471", this);
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
            // Call the callback right here.
            inCallback.complete(getConnLink().getVirtualConnection(), this);
        }
    }

    /**
     * This method handles errors when they occur during the code path of an async read. It takes
     * appropriate action based on the setting of the forceQueue parameter. If it is true, the
     * error callback is called on a separate thread. Otherwise it is called right here.
     *
     * @param forceQueue
     * @param exception
     * @param inCallback
     */
    private void handleAsyncError(boolean forceQueue, IOException exception, TCPReadCompletedCallback inCallback) {
        boolean fireHere = true;
        if (forceQueue) {
            // Error must be returned on a separate thread.
            // Reuse queuedWork object (performance), but reset the error parameters.
            queuedWork.setErrorParameters(getConnLink().getVirtualConnection(),
                                          this, inCallback, exception);

            EventEngine events = SSLChannelProvider.getEventService();
            if (null == events) {
                Exception e = new Exception("missing event service");
                FFDCFilter.processException(e, getClass().getName(), "503", this);
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
            // Call the callback right here.
            inCallback.error(getConnLink().getVirtualConnection(), this, exception);
        }
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
    private IOException checkRequest(long numBytes, boolean async) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "checkRequest");
        }
        IOException exception = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "numBytes=" + numBytes + " jitsize=" + getJITAllocateSize()
                         + " buffers=" + SSLUtils.getBufferTraceInfo(getBuffers()));
        }

        // Extract the buffers provided by the calling channel.
        WsByteBuffer callerBuffers[] = getBuffers();
        if (callerBuffers == null || callerBuffers.length == 0) {
            // Found null caller buffers. Check allocation size set by caller.
            if (getJITAllocateSize() <= 0 || getJITAllocateSize() < numBytes) {
                exception = new IOException("No buffer(s) provided for reading data into.");
            }
        } else if (numBytes == 0) {
            // zero byte read is allowed for sync only
            if (async) {
                // Can't do a read of zero in async mode.
                exception = new IOException("Number of bytes requested, "
                                            + numBytes + " is less than minimum allowed (async).");
            }
        } else if (numBytes < 0) {
            // NumBytes requested must be zero or positive
            exception = new IOException("Number of bytes requested, " + numBytes
                                        + " is less than minimum allowed.");
        } else {
            // Ensure buffer provided by caller is big enough to contain the
            // number of bytes requested.
            int bytesAvail = SSLUtils.lengthOf(callerBuffers, 0);
            if (bytesAvail < numBytes) {
                exception = new IOException("Number of bytes requested, " + numBytes
                                            + " exceeds space remaining in the buffers provided: " + bytesAvail);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "checkRequest: " + exception);
        }
        return exception;
    }

    /**
     * This method is called when a read is requested. It checks to see if any
     * data is left over from the previous read, but there wasn't space in the
     * buffers to store the result.
     *
     * @return number of bytes copied from the left over buffer
     */
    public long readUnconsumedDecData() {
        long totalBytesRead = 0L;

        // Determine if data is left over from a former read request.
        if (unconsumedDecData != null) {
            // Left over data exists. Is there enough to satisfy the request?
            if (getBuffer() == null) {
                // Caller needs us to allocate the buffer to return.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caller needs buffer, unconsumed data: "
                                 + SSLUtils.getBufferTraceInfo(unconsumedDecData));
                }
                // Note, data of unconsumedDecData buffer array should be starting
                // at position 0 in the first buffer.
                totalBytesRead = SSLUtils.lengthOf(unconsumedDecData, 0);
                // First release any existing buffers in decryptedNetBuffers array.
                cleanupDecBuffers();
                callerRequiredAllocation = true;
                // Note, it is the responsibility of the calling channel to release this buffer.
                decryptedNetBuffers = unconsumedDecData;
                // Set left over buffers to null to note that they are no longer in use.
                unconsumedDecData = null;
                if ((decryptedNetLimitInfo == null)
                    || (decryptedNetLimitInfo.length != decryptedNetBuffers.length)) {
                    decryptedNetLimitInfo = new int[decryptedNetBuffers.length];
                }
                SSLUtils.getBufferLimits(decryptedNetBuffers, decryptedNetLimitInfo);
            } else {
                // Caller provided buffers for read. We need to copy the left over
                // data to those buffers.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caller provided buffers, unconsumed data: "
                                 + SSLUtils.getBufferTraceInfo(unconsumedDecData));
                }
                // First release any existing buffers in decryptedNetBuffers array.
                cleanupDecBuffers();
                // The unconsumedDecData buffers have the data to copy to the user buffers.
                // The copyDataToCallerBuffers method copies from decryptedNetBuffers, so assign it.
                decryptedNetBuffers = unconsumedDecData;
                // Copy the outputbuffer to the buffers provided by the caller.
                totalBytesRead = copyDataToCallerBuffers();
                // Null out the reference to the overflow buffers.
                decryptedNetBuffers = null;
            }
        }
        return totalBytesRead;
    }

    /**
     * Get the buffers that the device channel should read into. These buffers
     * get reused over the course of multiple reads. The size of the buffers
     * are determined by either the allocation size specified by the application
     * channel or, if that wasn't set, the max packet buffer size specified in
     * the SSL engine.
     *
     * @param requestedSize minimum amount of space that must be available in the buffers.
     */
    protected void getNetworkBuffer(long requestedSize) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getNetworkBuffer: size=" + requestedSize);
        }
        // Reset the netBuffer mark.
        this.netBufferMark = 0;
        int allocationSize = getConnLink().getPacketBufferSize();
        if (allocationSize < requestedSize) {
            allocationSize = (int) requestedSize;
        }

        if (null == this.netBuffer) {
            // Need to allocate a buffer to give to the device channel to read into.
            this.netBuffer = SSLUtils.allocateByteBuffer(allocationSize, true);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Found existing netbuffer, " + SSLUtils.getBufferTraceInfo(netBuffer));
            }

            int cap = netBuffer.capacity();
            int pos = netBuffer.position();
            int lim = netBuffer.limit();
            if (pos == lim) {
                // 431269 - nothing is currently in this buffer, see if we can reuse it
                if (cap >= allocationSize) {
                    this.netBuffer.clear();
                } else {
                    this.netBuffer.release();
                    this.netBuffer = SSLUtils.allocateByteBuffer(allocationSize, true);
                }
            } else {
                // if we have less than the allocation size amount of data + empty,
                // then make a new buffer to start clean inside of...
                if ((cap - pos) < allocationSize) {
                    // allocate a new buffer, copy the existing data over
                    WsByteBuffer buffer = SSLUtils.allocateByteBuffer(allocationSize, true);
                    SSLUtils.copyBuffer(this.netBuffer, buffer, lim - pos);
                    this.netBuffer.release();
                    this.netBuffer = buffer;
                } else {
                    this.netBufferMark = pos;
                    this.netBuffer.position(lim);
                    this.netBuffer.limit(cap);
                }
            }
        }
        // Inform the device side channel to read into the new buffers.
        deviceReadContext.setBuffer(this.netBuffer);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "netBuffer: " + SSLUtils.getBufferTraceInfo(this.netBuffer));
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getNetworkBuffer");
        }
    }

    private int availableDecryptionSpace() {
        int available = 0;
        if (null != this.decryptedNetBuffers) {
            for (WsByteBuffer buffer : this.decryptedNetBuffers) {
                if (null == buffer) {
                    // not sure this is possible these days but just in case...
                    break;
                }
                available += buffer.remaining();
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "available decryption space: " + available);
        }
        return available;
    }

    /**
     * Get the buffers that will be used for output from the SSL engine. If read
     * buffers were supplied by the calling application channel, then they will
     * be used. Not, if a buffer array was supplied, the first buffer of the array
     * will be used (since the SSL engine on takes a single output buffer). If not
     * supplied, one will be allocated here. The size of the buffer will be either
     * the JITAllocationSize set by the user, or the default size from the SSL
     * engine if the caller didn't provide anything.
     * <p>
     * Note, it is the responsibility of the application channel to release this
     * buffer if it gets allocated.
     */
    private void getDecryptedNetworkBuffers() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getDecryptedNetworkBuffers");
        }

        // Check if we already have a known decNetworkBuffer array.
        if (decryptedNetBuffers == null) {
            // Check if the buffer was set by the calling app channel.
            decryptedNetBuffers = getBuffers();
            if (decryptedNetBuffers == null) {
                // Not set by calling app channel. Allocate it here.
                callerRequiredAllocation = true;
                int allocationSize = getJITAllocateSize();
                int minSize = getConnLink().getAppBufferSize();
                // Ensure the value is positive.
                if (allocationSize <= 0) {
                    allocationSize = minSize;
                }
                // Allocate the buffer. Note, app channel has the responsibility to release this.
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "allocating JIT buffer; size=" + allocationSize);
                }
                decryptedNetBuffers = SSLUtils.allocateByteBuffers(allocationSize,
                                                                   bytesRequested, getConfig().getDecryptBuffersDirect(), false);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Using buffers from getBuffers()");
                }
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Using buffers previously set");
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getDecryptedNetworkBuffers");
        }
    }

    /**
     * This method is called when the SSL engine returns from unwrap indicating
     * that the output buffer was too small. If a buffer was provided by the
     * calling app channel to read into, it may have been too small. Note, we're
     * only able to use the first buffer in the provided array because of the limitations
     * of the unwrap method. If the buffer provided by the caller is too small,
     * one is allocated and sized according to the defaults in the SSL engine.
     * If the "too small" result came after using a buffer that
     * was allocated by us, we can't do anything but throw an Exception.
     * <p>
     * Note, if the calling channel provided buffers and we can't use them, we
     * will have to copy our results into the caller's buffers, save any data
     * that can't fit, and ultimately have the responsibility to free
     * the allocated buffer.
     * <p>
     */
    private void expandDecryptedNetBuffer() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "expandDecryptedNetBuffer");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "callerRequiredAlloc=" + callerRequiredAllocation
                         + ", decNetReleaseReq=" + decryptedNetBufferReleaseRequired);
        }

        boolean expand = false;
        if (!getJITAllocateAction()) {
            // Caller provided original buffers.
            if (decryptedNetBufferReleaseRequired) {
                // Formerly they were found to be inadequate so we created new temp buffers.
                // The new temp buffers were sized based on the JSSE API.
                expand = true;
            } else {
                // Caller provided original buffers. However, they are not big
                // enough to contain the JSSE output. Allocate a new buffer with
                // default size from ssl engine. Its contents will be copied to
                // the caller's supplied buffer after the ssl engine runs.
                // Note, we have the responsibility of releasing this temporary buffer.
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Allocating substitute decNetworkBuffer");
                }
                decryptedNetBuffers = new WsByteBuffer[1];
                decryptedNetBuffers[0] = SSLUtils.allocateByteBuffer(getConnLink().getAppBufferSize(),
                                                                     getConfig().getDecryptBuffersDirect());
                decryptedNetBufferReleaseRequired = true;
            }
        } else {
            // Current buffers were allocated by us, but were still too small.
            expand = true;
        }

        if (expand) {
            // Expand the current set of buffers.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Expanding set of buffers by one.");
            }
            WsByteBuffer tempBuffers[] = decryptedNetBuffers;
            decryptedNetBuffers = new WsByteBuffer[tempBuffers.length + 1];
            for (int i = 0; i < tempBuffers.length; i++) {
                decryptedNetBuffers[i] = tempBuffers[i];
            }
            // Allocate a new buffer and append it to the end of the existing array.
            // 316607 since this is not the first buffer, use the max size so we won't potentially
            // loop through creating many new buffers based on the current jit size, presuming
            // the same size will be used for future reads. Worst case is that we'll have
            // to copy the buffers for a future read with a small jit size.
            decryptedNetBuffers[tempBuffers.length] = SSLUtils.allocateByteBuffer(
                                                                                  getConnLink().getAppBufferSize(), getConfig().getDecryptBuffersDirect());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "decryptedNetBuffers changed to ..."
                         + SSLUtils.getBufferTraceInfo(decryptedNetBuffers));
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "expandDecryptedNetBuffer");
        }
    }

    /**
     * This method is called after a successful decryption is done where the output
     * was written to a temporarily allocated buffer. The purpose of this method
     * is to copy the contents of the temporary buffer to the caller provided
     * buffers. It is assumed that the caller provided buffers can be fetched
     * via getBuffers(). It is not assumed that there will be space in the
     * caller provided buffers to store all the results in the temp buffer.
     * Data will be copied from the decryptedNetworkBuffer starting from the
     * position and up to either the limit or the end of the caller buffers.
     * If any data is left over in the decryptedNetworkBuffer it will be stored
     * in the unconsumedDecData for a future read.
     *
     * @return The number of bytes copied
     */
    private int copyDataToCallerBuffers() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "copyDataToCallerBuffers");
        }
        WsByteBuffer src[] = decryptedNetBuffers;
        WsByteBuffer dst[] = getBuffers();
        WsByteBuffer dstBuffer = null;
        WsByteBuffer srcBuffer = null;
        int dstIndex = 0;
        int srcIndex = 0;
        int srcRemaining = 0;
        int dstRemaining = 0;
        int numBytesCopied = 0;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Before copy:\r\n\tsrc: " + SSLUtils.getBufferTraceInfo(src)
                         + "\r\n\tdst: " + SSLUtils.getBufferTraceInfo(dst));
        }

        // Loop through the destination array.
        for (; srcIndex < src.length && dstIndex < dst.length; srcIndex++) {
            // Access the next src buffer
            srcBuffer = src[srcIndex];
            srcRemaining = srcBuffer.remaining();
            while (srcRemaining > 0 && dstIndex < dst.length) {
                // Access the current dst buffer.
                dstBuffer = dst[dstIndex];
                dstRemaining = dstBuffer.remaining();
                // Data available in src buffer to be copied.
                if (srcRemaining <= dstRemaining) {
                    // All data in src will fit in dst, but room may still be left in dst.
                    SSLUtils.copyBuffer(srcBuffer, dstBuffer, srcRemaining);
                    numBytesCopied += srcRemaining;
                    // Check if any room left in dst.
                    dstRemaining = dstBuffer.remaining();
                    if (dstRemaining == 0) {
                        // No more room in dst buffer. Shift dst array index.
                        dstIndex++;
                    }
                    break;
                } else if (dstRemaining > 0) {
                    // Dst is smaller than src, but there is some room.
                    SSLUtils.copyBuffer(srcBuffer, dstBuffer, dstRemaining);
                    numBytesCopied += dstRemaining;
                    // No more room in dst buffer. Shift dst array index.
                    dstIndex++;
                    // Update data left in src buffer.
                    srcRemaining = srcBuffer.remaining();
                    continue;
                } else {
                    // current destination buffer is completely full already
                    dstIndex++;
                }
            }
        }
        // decrement source array index back to the last one used
        srcIndex--;

        // At this point, the dst buffer has been filled as much as possible.
        // Now we have to look for left over data in the src buffer to save
        // for a future read request. Note that the src buffer may actually
        // be unconsumedDecData.
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "After copy:\r\n\tsrc: " + SSLUtils.getBufferTraceInfo(src)
                         + "\r\n\tdst: " + SSLUtils.getBufferTraceInfo(dst));
        }

        // figure out how much remaining decrypted data is available
        int remaining = SSLUtils.lengthOf(src, srcIndex);

        // Check if the src is the unconsumedDecData
        if (src == unconsumedDecData && null != srcBuffer
            && (srcBuffer.hashCode() == unconsumedDecData[srcIndex].hashCode())) {
            // Check if all the data was drained from the unconsumedDecData buffer.
            if (0 < remaining) {
                // Data is left in the unconsumedDecData buffer. Need to shift
                // it to position 0 for a future read.
                WsByteBuffer temp = unconsumedDecData[srcIndex];
                unconsumedDecData[srcIndex] = unconsumedDecData[srcIndex].slice();
                temp.release();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Data left in unconsumedDecData: "
                                 + SSLUtils.getBufferTraceInfo(unconsumedDecData));
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Entire unconsumedDecData buffer drained.  Release and null out.");
                }
                // Data was completely drained.
                WsByteBufferUtils.releaseBufferArray(unconsumedDecData);
                unconsumedDecData = null;
            }

            // otherwise copy anything that's left
        } else {
            if (0 < remaining) {
                unconsumedDecData = SSLUtils.compressBuffers(decryptedNetBuffers, true);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "unconsumedDecData: " + SSLUtils.getBufferTraceInfo(unconsumedDecData));
                }
            } else {
                cleanupDecBuffers();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "copyDataToCallerBuffers: " + numBytesCopied);
        }
        return numBytesCopied;
    }

    /**
     * Utility method to handle releasing the decrypted network buffers that we
     * may or may not own at this point.
     */
    private void cleanupDecBuffers() {
        // if we have decrypted buffers and they are either JIT created or made
        // during decryption/expansion (user buffers too small) then release them
        // here and dereference
        if (null != this.decryptedNetBuffers
            && (callerRequiredAllocation || decryptedNetBufferReleaseRequired)) {
            WsByteBufferUtils.releaseBufferArray(this.decryptedNetBuffers);
            this.decryptedNetBuffers = null;
        }
    }

    /**
     * Release the potential buffer that were created
     */
    public void close() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "close, vc=" + getVCHash());
        }

        synchronized (closeSync) {
            if (closeCalled) {
                return;
            }

            closeCalled = true;
            if (null != this.netBuffer) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Releasing netBuffer during close "
                                 + SSLUtils.getBufferTraceInfo(netBuffer));
                }
                this.netBuffer.release();
                this.netBuffer = null;
            }

            cleanupDecBuffers();
            if (unconsumedDecData != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Releasing unconsumed decrypted buffers, "
                                 + SSLUtils.getBufferTraceInfo(unconsumedDecData));
                }
                WsByteBufferUtils.releaseBufferArray(unconsumedDecData);
                unconsumedDecData = null;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "close");
        }
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.TCPReadRequestContext#setJITAllocateSize(int)
     */
    @Override
    public void setJITAllocateSize(int numBytes) {
        this.jITAllocateSize = numBytes;
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.TCPReadRequestContext#getJITAllocateAction()
     */
    @Override
    public boolean getJITAllocateAction() {
        return this.callerRequiredAllocation;
    }

    /**
     * Return the size of the buffers that should be created for this request.
     *
     * @return size of allocated buffers.
     */
    public int getJITAllocateSize() {
        return this.jITAllocateSize;
    }

    /**
     * Handle common activity of read and readAsynch involving the decryption of the
     * current buffers. The caller will have the responsibility of informing the app
     * side channels once complete.
     *
     * @param async if called from an async read (true) or sync read (false)
     * @return null if the decryption was successful, the resulting exception otherwise
     */
    protected Exception decryptMessage(boolean async) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "decryptMessage");
        }

        Exception exception = null;
        // Storage for the result of calling the SSL engine
        SSLEngineResult result = null;
        // Status of the SSL engine.
        Status status = null;
        // Access output buffer to give to SSL engine. Attempt to use buffers from app channel.
        getDecryptedNetworkBuffers();
        final int packetSize = getConnLink().getPacketBufferSize();
        final int appBufferSize = getConnLink().getAppBufferSize();

        try {
            while (true) {
                // JSSE (as of JDK 6 11/2010) requires at least the packet-size
                // of available space in the output buffers or it blindly returns
                // the BUFFER_OVERFLOW result
                if (appBufferSize > availableDecryptionSpace()) {
                    expandDecryptedNetBuffer();
                }
                // Protect JSSE from potential SSL packet sizes that are too big.
                int savedLimit = SSLUtils.adjustBufferForJSSE(netBuffer, packetSize);
                // These limits will be reset only if the result of the unwrap is
                // "OK". Otherwise, they will not be changed. Try to reuse this array.
                if ((decryptedNetLimitInfo == null)
                    || (decryptedNetLimitInfo.length != decryptedNetBuffers.length)) {
                    decryptedNetLimitInfo = new int[decryptedNetBuffers.length];
                }
                SSLUtils.getBufferLimits(decryptedNetBuffers, decryptedNetLimitInfo);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "before unwrap:\r\n\tnetBuf: "
                                 + SSLUtils.getBufferTraceInfo(netBuffer)
                                 + "\r\n\tdecNetBuffers: " + SSLUtils.getBufferTraceInfo(decryptedNetBuffers));
                }
                // Call the SSL engine to decrypt the request.
                result = getConnLink().getSSLEngine().unwrap(
                                                             this.netBuffer.getWrappedByteBuffer(),
                                                             SSLUtils.getWrappedByteBuffers(decryptedNetBuffers));
                if (0 < result.bytesProduced()) {
                    SSLUtils.flipBuffers(decryptedNetBuffers, result.bytesProduced());
                }
                status = result.getStatus();
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "after unwrap:\r\n\tnetBuf: "
                                 + SSLUtils.getBufferTraceInfo(netBuffer)
                                 + "\r\n\tdecNetBuffers: " + SSLUtils.getBufferTraceInfo(decryptedNetBuffers)
                                 + "\r\n\tstatus=" + status
                                 + " HSstatus=" + result.getHandshakeStatus()
                                 + " consumed=" + result.bytesConsumed()
                                 + " produced=" + result.bytesProduced());
                }

                // If a limit modification was saved, restore it.
                if (-1 != savedLimit) {
                    this.netBuffer.limit(savedLimit);
                }

                // Record the number of bytes produced.
                bytesProduced += result.bytesProduced();

                // check CLOSED status first, recent JDKs are showing the status=CLOSED
                // and hsstatus=NEED_WRAP when receiving the connection closed message
                // from the other end. Our call to flushCloseDown() later will handle
                // the write of our own 23 bytes of connection closure
                if (status == Status.CLOSED) {
                    exception = sessionClosedException;
                    break;
                }

                // Handle the SSL engine result
                // Note: check handshake status first as renegotations could be requested
                // at any point and the Status is secondary to the handshake status
                if ((result.getHandshakeStatus() == HandshakeStatus.NEED_TASK)
                    || (result.getHandshakeStatus() == HandshakeStatus.NEED_WRAP)
                    || (result.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP)) {
                    try {
                        result = doHandshake(async);
                    } catch (IOException e) {
                        // no FFDC required
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Caught exception during SSL handshake, " + e);
                        }
                        exception = e;
                        break;
                    }
                    // Check to see if handshake was done synchronously.
                    if (result != null) {
                        // Handshake was done synchronously. Verify results.
                        status = result.getStatus();
                        if (result.getHandshakeStatus() == HandshakeStatus.FINISHED) {
                            // Handshake finished. Need to read more per original request.
                            exception = readNeededInternalException;
                            // No error here. Just inform caller.
                            break;
                        } else if (status == Status.OK) {
                            // Handshake complete and initial request already read and decrypted.
                            prepareDataForNextChannel();
                            break;
                        } else {
                            // Unknown result from handshake. All other results should have thrown exceptions.
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Unhandled result from SSL engine: " + status);
                            }
                            exception = new SSLException("Unhandled result from SSL engine: " + status);
                            break;
                        }
                    }
                    // Handshake is being handled asynchronously.
                    break;
                }

                else if (status == Status.OK) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "OK result from the SSL engine,"
                                     + " callerReqAlloc=" + getJITAllocateAction()
                                     + " decNetBuffRelReq=" + decryptedNetBufferReleaseRequired);
                    }
                    if (bytesRequested > bytesProduced) {
                        // More data needs to be decrypted.
                        // 431992 - only change pos/lim if something was produced
                        if (0 < result.bytesProduced()) {
                            // Prepare the output buffer, preventing overwrites and
                            // opening up space closed by JSSE.
                            SSLUtils.positionToLimit(decryptedNetBuffers);
                            // Reset the limits saved for the decryptedNetBuffers.
                            SSLUtils.setBufferLimits(decryptedNetBuffers, decryptedNetLimitInfo);
                        }
                        // Check if all the data has been read from the netBuffers.
                        if (this.netBuffer.remaining() == 0) {
                            // No more data available. More must be read. Reused
                            // exception instance (save new object).
                            exception = readNeededInternalException;
                            break;
                        }
                        // More data is available. Loop around and decrypt again.
                        continue;
                    }
                    // Data has been decrypted.
                    break;
                }

                else if (status == Status.BUFFER_OVERFLOW) {
                    // The output buffers provided to the SSL engine were not big
                    // enough. A bigger buffer must be supplied. If we can build
                    // a bigger buffer and call again, build it. Prepare the output
                    // buffer, preventing overwrites and opening up space closed by JSSE.
                    expandDecryptedNetBuffer();
                    // Try again with the bigger buffer array.
                    continue;
                }

                else if (status == Status.BUFFER_UNDERFLOW) {
                    // The engine was not able to unwrap the incoming data because there were not
                    // source bytes available to make a complete packet.
                    // More data needs to be read into the input buffer. Alert
                    // calling method to keep reading.
                    exception = readNeededInternalException;
                    // No error here. Just inform caller.
                    break;
                }

                else {
                    exception = new SSLException("Unknown result from ssl engine not handled yet: " + status);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Unknown result from ssl engine not handled yet: " + status);
                    }
                    break;
                }
            } // end while

        } catch (SSLException ssle) {
            // no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception during decryption, " + ssle);
            }
            exception = ssle;
            cleanupDecBuffers();
        } catch (Exception up) {
            synchronized (closeSync) {
                // if close has been called then assume this exception was due to a race condition
                // with the close logic and consume the exception without FFDC. Otherwise rethrow
                // as the logic did before this change.
                if (!closeCalled) {
                    throw up;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "decryptMessage: " + exception);
        }
        return exception;
    }

    public void setNetBuffer(WsByteBuffer buff) {
        this.netBuffer = buff;
    }

    /**
     * This callback will be used as a feedback mechanism for calls to handleHandshake
     * from encryptMessage.
     */
    public class MyHandshakeCompletedCallback implements SSLHandshakeCompletedCallback {
        /** Underlying TCP read context */
        private final TCPReadRequestContext readContext;
        /** Applvl callback used when this one completes or fails */
        private final TCPReadCompletedCallback hsReadCallback;
        /** Buffer used at network level */
        private WsByteBuffer hsNetBuffer;
        /** Buffer used in handshake unwrap calls */
        private WsByteBuffer decryptedNetBuffer;
        /** Buffer used in handshake wrap calls */
        private WsByteBuffer encryptedAppBuffer;
        /** allow other code to tell this class if they changed netBuffer */
        private WsByteBuffer updatedNetBuffer = null;

        /**
         * Internal callback used for handshake exchanges.
         *
         * @param _readContext
         * @param _readCallback
         * @param _netBuffer
         * @param _decryptedNetBuffer
         * @param _encryptedAppBuffer
         */
        public MyHandshakeCompletedCallback(
                                            TCPReadRequestContext _readContext,
                                            TCPReadCompletedCallback _readCallback,
                                            WsByteBuffer _netBuffer,
                                            WsByteBuffer _decryptedNetBuffer,
                                            WsByteBuffer _encryptedAppBuffer) {
            this.readContext = _readContext;
            this.hsReadCallback = _readCallback;
            this.hsNetBuffer = _netBuffer;
            this.decryptedNetBuffer = _decryptedNetBuffer;
            this.encryptedAppBuffer = _encryptedAppBuffer;
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

        /*
         * @see com.ibm.ws.channel.ssl.internal.SSLHandshakeCompletedCallback#complete(javax.net.ssl.SSLEngineResult)
         */
        @Override
        public void complete(SSLEngineResult sslResult) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.entry(tc, "handshake complete");
            }
            // Release buffers used in the handshake.
            hsNetBuffer.release();
            hsNetBuffer = null;
            decryptedNetBuffer.release();
            decryptedNetBuffer = null;
            encryptedAppBuffer.release();
            encryptedAppBuffer = null;
            // Verify results.
            Status sslStatus = sslResult.getStatus();
            // Handshake was done synchronously. Verify results.
            if (sslResult.getHandshakeStatus() == HandshakeStatus.FINISHED) {
                // Handshake finished. Another read is needed in order to build
                // a valid SSL input packet.
                // Force queue so that original code path will be taken.
                read(1, hsReadCallback, true, TCPRequestContext.USE_CHANNEL_TIMEOUT);
            } else if (sslStatus == Status.OK) {
                // Handshake complete and initial request already read and decrypted.
                prepareDataForNextChannel();
                // Pass on to caller
                callback.complete(getConnLink().getVirtualConnection(), readContext);
            } else {
                // Unknown result from handshake. All other results should have thrown exceptions.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unhandled result from SSL engine: " + sslStatus);
                }
                IOException exception = new IOException("Unhandled result from SSL engine: " + sslStatus);
                FFDCFilter.processException(exception, getClass().getName(), "750", this);
                callback.error(getConnLink().getVirtualConnection(), readContext, exception);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "handshake complete");
            }
        }

        /*
         * @see com.ibm.ws.channel.ssl.internal.SSLHandshakeCompletedCallback#error(java.io.IOException)
         */
        @Override
        public void error(IOException ioe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.entry(tc, "handshake error");
            }
            // Release buffers used in the handshake.
            hsNetBuffer.release();
            hsNetBuffer = null;
            decryptedNetBuffer.release();
            decryptedNetBuffer = null;
            encryptedAppBuffer.release();
            encryptedAppBuffer = null;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception during encryption, " + ioe);
            }
            FFDCFilter.processException(ioe, getClass().getName(), "762", this);
            callback.error(getConnLink().getVirtualConnection(), readContext, ioe);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "handshake error");
            }
        }
    }

    /**
     * When data is read, there is always the change the a renegotiation will take place. If so,
     * this method will be called. Note, it is used by both the sync and async writes.
     *
     * @param async
     * @return result of the handshake
     * @throws IOException
     */
    private SSLEngineResult doHandshake(boolean async) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "doHandshake");
        }

        SSLEngineResult sslResult;
        // Line up all the buffers needed for the SSL handshake. Temporary so
        // use indirect allocation for speed.
        int appSize = getConnLink().getAppBufferSize();
        int packetSize = getConnLink().getPacketBufferSize();
        WsByteBuffer localNetBuffer = SSLUtils.allocateByteBuffer(packetSize, true);
        WsByteBuffer decryptedNetBuffer = SSLUtils.allocateByteBuffer(appSize, false);
        WsByteBuffer encryptedAppBuffer = SSLUtils.allocateByteBuffer(packetSize, true);
        // Callback to be used if the request is async.
        MyHandshakeCompletedCallback handshakeCallback = null;
        if (async) {
            handshakeCallback = new MyHandshakeCompletedCallback(this, callback, localNetBuffer, decryptedNetBuffer, encryptedAppBuffer);
        }

        try {
            // Do the SSL handshake. Note, if synchronous the handshakeCallback is null.
            sslResult = SSLUtils.handleHandshake(
                                                 getConnLink(),
                                                 localNetBuffer,
                                                 decryptedNetBuffer,
                                                 encryptedAppBuffer,
                                                 null,
                                                 handshakeCallback,
                                                 false);

            if (handshakeCallback.getUpdatedNetBuffer() != null) {
                netBuffer = handshakeCallback.getUpdatedNetBuffer();
            }

        } catch (IOException e) {
            // Release buffers used in the handshake.
            localNetBuffer.release();
            localNetBuffer = null;
            decryptedNetBuffer.release();
            decryptedNetBuffer = null;
            encryptedAppBuffer.release();
            encryptedAppBuffer = null;
            throw e;
        }
        if (sslResult != null) {
            // Handshake was done synchronously.
            // Release buffers used in the handshake.
            localNetBuffer.release();
            localNetBuffer = null;
            decryptedNetBuffer.release();
            decryptedNetBuffer = null;
            encryptedAppBuffer.release();
            encryptedAppBuffer = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "doHandshake");
        }
        return sslResult;
    }

    /**
     * This method was written because two areas of the code needed it. It is called after
     * a call to unwrap or handleHandshake results in OK. It means that data is available
     * and can be sent to the calling channel. This method prepares the data before it is
     * passed to the calling channel.
     */
    protected void prepareDataForNextChannel() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "prepareDataForNextChannel");
        }

        // Ensure the output buffers are already set in place for the calling channel.
        if (getJITAllocateAction()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Allocation was done here, adjust and hand off buffers, JIT="
                             + getJITAllocateSize());
            }

            // Multiple cases to handle here. Remember, we allocated these buffers.
            // 1 - decNetBuffers has only 1 buffer and it is <= the JIT size
            // We can just send it up to the caller.
            // 2 - decNetBuffers has only 1 buffer, but it is > the JIT size
            // We need to copy it to a JIT sized buffer and save the rest of a future read.
            // 3 - decNetBuffers has multiple buffers, the first of which has the matching JIT size.
            // We should pass the first buffer back to the caller and save the rest of a future read.
            // 4 - decNetBuffers has multiple buffers, the first of which does not match the JIT size.
            // We have to check ensure the JIT size is honored. May have to do a buffer copy.
            int decryptedDataSize = SSLUtils.lengthOf(decryptedNetBuffers, 0);
            if (decryptedNetBuffers.length == 1) {
                if (decryptedDataSize <= getJITAllocateSize()) {

                    // Case 1:
                    // There is only one decryptedNetBuffer and the size is less than the JIT.
                    // Therefore, we can return this buffer to the caller.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "single decNetBuffer is okay to pass to caller");
                    }
                    // Data is currently between pos and lim. Adjust for calling channel.
                    SSLUtils.positionToLimit(decryptedNetBuffers);
                    // Reset the limits saved for the decryptedNetBuffers.
                    SSLUtils.setBufferLimits(decryptedNetBuffers, decryptedNetLimitInfo);
                    // Store the decrypted buffer to this context for reference by the caller.
                    setBuffer(decryptedNetBuffers[0]);

                } else {
                    // Case 2:
                    // There is only one decryptedNetBuffer, but the size is greater than the JIT.
                    // Therefore, we have to copy the buffer into a JIT sized buffer
                    // and save the left overs.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "only one decNetBuffer, but too big ("
                                     + decryptedDataSize + ") for JIT.  Need to copy.");
                    }
                    // Create a new buffer to hand to the caller.
                    // We want to reuse copyDataToCallerBuffers.
                    // It copies from decryptedNetBuffers to getBuffers() which
                    // are currently the same, so change it.
                    setBuffer(SSLUtils.allocateByteBuffer(getJITAllocateSize(), false));
                    getBuffer().limit(getJITAllocateSize());
                    // Copy from the decNetBuffers into the buffers to be sent to the caller.
                    copyDataToCallerBuffers();
                }

            } else {
                // There are > 1 decryptedNetBuffers.
                // Check if the first one matches the JIT size.
                // TODO this is wrong. should be using capacity() instead of
                // remaining I think since after decrypt, remaining is less than
                // buffer cap (decrypt 4 bytes and remaining is 8188 but buffer is
                // the 8192 that JIT size is by default), meaning it's fine
                // not sure about all cases though...
                if (decryptedNetBuffers[0].remaining() == getJITAllocateSize()) {
                    // Case 3:
                    // Found a match. Return the first buffer to the caller and
                    // save the rest for a future read.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "multiple buffers, first of which matches the JIT size");
                    }
                    // Data is currently between pos and lim. Adjust for calling channel.
                    decryptedNetBuffers[0].position(decryptedNetBuffers[0].limit());
                    // Store the decrypted buffer to this context for reference by the caller.
                    setBuffer(decryptedNetBuffers[0]);
                    // Now save the other buffers in the unconsumedDecData (which is null at this point)
                    if (null != unconsumedDecData) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                            Tr.event(tc, "Expected null unconsumed array, but isn't: "
                                         + SSLUtils.getBufferTraceInfo(unconsumedDecData));
                        }
                        // shouldn't release buffers we might not own (especially since
                        // they shouldn't even be here in the first place)
                        // WsByteBufferUtils.releaseBufferArray(unconsumedDecData);
                    }
                    // figure out if any more buffers exist with actual data to save
                    int size = 0;
                    for (int i = 1; i < decryptedNetBuffers.length; i++) {
                        // if there is nothing in this buffer, just release it
                        if (0 == decryptedNetBuffers[i].remaining()) {
                            decryptedNetBuffers[i].release();
                        } else {
                            size++;
                        }
                    }
                    if (0 < size) {
                        unconsumedDecData = new WsByteBuffer[size];
                        for (int i = 1, x = 0; x < size; i++, x++) {
                            unconsumedDecData[x] = decryptedNetBuffers[i];
                        }
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "unconsumedDecData: "
                                         + SSLUtils.getBufferTraceInfo(unconsumedDecData));
                        }
                    }

                } else {
                    // Case 4:
                    // Multiple buffers, first of which doesn't match the JIT. Need to copy.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "multiple buffers, first does not match the JIT size.");
                    }
                    // Create a new buffer to hand to the caller.
                    // We want to reuse copyDataToCallerBuffers.
                    // It copies from decryptedNetBuffers to getBuffers() which
                    // are currently the same, so change it.
                    setBuffer(SSLUtils.allocateByteBuffer(getJITAllocateSize(), false));
                    getBuffer().limit(getJITAllocateSize());
                    // Adjust the decryptedNetBuffers. Results from the JSSE keep
                    // untouched buffers with their pos = lim. Buffers with data
                    // have nonzero pos and lim. Unused buffers have zero
                    // pos and lim. We need the data to be between pos and lim
                    // for the copy. Zeroing out the pos enables this.
                    for (int i = 0; i < decryptedNetBuffers.length; i++) {
                        decryptedNetBuffers[i].position(0);
                    }
                    // Copy from the decNetBuffers into the buffers to be sent to the caller.
                    copyDataToCallerBuffers();
                }
            }

        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Using channel provided buffers");
            }
            // Calling channel provided buffers to read into.
            if (decryptedNetBufferReleaseRequired) {
                // User provided buffers were not originally adequate. Temporary
                // buffers were allocated to hold the result.
                for (int i = 0; i < this.decryptedNetBuffers.length; i++) {
                    if (null != this.decryptedNetBuffers[i]) {
                        this.decryptedNetBuffers[i].position(0);
                    }
                }
                copyDataToCallerBuffers();
            } else {
                // User provided buffers were adequate
                SSLUtils.positionToLimit(decryptedNetBuffers);
                SSLUtils.setBufferLimits(decryptedNetBuffers, decryptedNetLimitInfo);
            }
        }
        // The decryptedNetBuffers are being given to the next channel.
        // Eliminate tracking of ownership.
        decryptedNetBuffers = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Buffers being sent to next channel: "
                         + SSLUtils.getBufferTraceInfo(getBuffers()));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "prepareDataForNextChannel");
        }
    }

    /**
     * Callback used for reads started by the SSL context.
     */
    private class SSLReadCompletedCallback implements TCPReadCompletedCallback {
        /** User callback to use when complete */
        private TCPReadCompletedCallback myCallback = null;
        /** Context using this callback */
        private SSLReadServiceContext readContext = null;

        /**
         * Constructor.
         *
         * @param _readContext
         */
        public SSLReadCompletedCallback(SSLReadServiceContext _readContext) {
            this.readContext = _readContext;
        }

        /**
         * Set the callback to be used when the read is complete or hits an error.
         *
         * @param _myCallback
         */
        public void setCallBack(TCPReadCompletedCallback _myCallback) {
            this.myCallback = _myCallback;
        }

        /*
         * @see com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback#complete(com.ibm.wsspi.channelfw.VirtualConnection, com.ibm.wsspi.tcpchannel.TCPReadRequestContext)
         */
        @Override
        public void complete(VirtualConnection vc, TCPReadRequestContext tcpRead) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.entry(tc, "complete, vc=" + getVCHash());
            }

            synchronized (closeSync) {
                if (closeCalled) {
                    // close will cleanup the buffers, so we have to return at this point
                    return;
                }

                // We set a single buffer to be read into, so there will only ever be one. Call getBuffer()
                netBuffer = tcpRead.getBuffer();
                // Prepare the input buffer for call to unwrap.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "just after async read, but before flip\r\n\t"
                                 + SSLUtils.getBufferTraceInfo(netBuffer));
                }
                // Remember where the position was before flipping in case more reading necessary.
                netBuffer.limit(netBuffer.position());
                netBuffer.position(netBufferMark);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Read bytes: " + netBuffer.remaining());
                }
            }

            // Decrypt the message. Note that since we are already on a new thread,
            // so don't force queue here.

            // added sync, but I'm not willing to hold the lock through-out decryptMessage, to much risk of a deadlock for
            // a mainline fix/change, decryptMessage will need to catch exceptions and ignore
            // them if close has been invoked, fixes are ugly sometimes.
            Exception exception = decryptMessage(true);
            synchronized (closeSync) {
                if (closeCalled) {
                    // close will cleanup the buffers, so we have to return at this point
                    return;
                }
            }

            if (exception == null) {
                // Check if has been read.
                if (bytesRequested > bytesProduced) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Some data decrypted, more data required, do a read. vc=" + getVCHash());
                    }
                    // Force queue so that original code path will be taken. Use a different callback.
                    tcpRead.read(1, this, true, TCPRequestContext.USE_CHANNEL_TIMEOUT);
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "data decrypted: bytesRequested=" + bytesRequested
                                     + " bytesProduced=" + bytesProduced);
                    }
                    // data has been produced. Prepare data and inform callback.
                    prepareDataForNextChannel();
                    // Already on a new thread so no special logic needed.
                    this.myCallback.complete(vc, readContext);
                }
            } else if (exception instanceof ReadNeededInternalException) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No data was decrypted, more data required, do a read. vc=" + getVCHash());
                }
                // Another read is needed in order to build a valid SSL input packet.
                // Align pos and lim in prep for another read. The flip needs to be undone.
                getNetworkBuffer(1);
                tcpRead.setBuffer(netBuffer);
                // Force queue so that original code path will be taken. Use a different callback.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Calling device side read with netBuffer, "
                                 + SSLUtils.getBufferTraceInfo(netBuffer));
                }
                tcpRead.read(1, this, true, TCPRequestContext.USE_CHANNEL_TIMEOUT);
            } else if (exception instanceof SessionClosedException) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "SSL Session has been closed.");
                }
                // 238579 - inform channel above
                // Already on a new thread so no special logic needed.
                callback.error(vc, readContext, new IOException("SSL connection was closed by peer"));
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caught exception during unwrap, " + exception);
                }
                FFDCFilter.processException(exception, getClass().getName(), "798", this);
                // Already on a new thread so no special logic needed.
                callback.error(vc, readContext,
                               new IOException("SSL decryption failed: " + exception.getMessage()));
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "complete");
            }
        }

        /*
         * @see com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback#error(com.ibm.wsspi.channelfw.VirtualConnection, com.ibm.wsspi.tcpchannel.TCPReadRequestContext,
         * java.io.IOException)
         */
        @Override
        public void error(VirtualConnection vc, TCPReadRequestContext tcpRead, IOException ioe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.entry(tc, "error, vc=" + getVCHash());
            }

            synchronized (closeSync) {
                // Protect future reads from thinking data was read.
                // Current netbuffers has space to read into between pos and lim.
                if ((null == netBuffer) || (closeCalled)) {
                    // this callback shouldn't be happening
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Unexpected callback, unable to proceed");
                    }
                    return;
                }
                netBuffer.limit(netBuffer.position());
                netBuffer.position(netBufferMark);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Reset buffers after read error: netBuffer:"
                                 + SSLUtils.getBufferTraceInfo(netBuffer));
                }
            }

            // Report error up the chain. Already on a new thread so no special logic needed.
            callback.error(vc, readContext, ioe);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "error");
            }
        }
    }

    /**
     * This class is used to handle an asynchronous read that was forced to be queued.
     */
    private class QueuedWork implements Runnable {
        /** Queued action is a read */
        private final static int READ = 0;
        /** Queued action is an error */
        private final static int ERROR = 1;
        /** Queued action is a completion */
        private final static int COMPLETE = 2;

        /** Number of bytes to read */
        private long numBytes = 0L;
        /** App level callback to use */
        private TCPReadCompletedCallback userCallback = null;
        /** Timeout to use for the read */
        private int timeout = 0;
        /** Connection object for this work */
        private VirtualConnection vc = null;
        /** Underlying TCP read context */
        private TCPReadRequestContext tcpReadRequestContext = null;
        /** Exception if an error happens */
        private IOException exception = null;
        /** Action to take on queued run() */
        private int action = READ;

        /**
         * Empty constructor. Important methods are below for setting parameters. This
         * object is reused to save on object creation which is why separate constructors
         * were not created to solve this problem.
         */
        protected QueuedWork() {
            // nothing to do
        }

        /**
         * Set the parameters to be used for the next piece of read work.
         *
         * @param _numBytes
         * @param _userCallback
         * @param _timeout
         */
        public void setReadParameters(long _numBytes, TCPReadCompletedCallback _userCallback, int _timeout) {
            this.numBytes = _numBytes;
            this.userCallback = _userCallback;
            this.timeout = _timeout;
            this.action = READ;
        }

        /**
         * Set the parameters to be used for the next piece of error work.
         *
         * @param _vc
         * @param _tcpReadRequestContext
         * @param _userCallback
         * @param _exception
         */
        public void setErrorParameters(VirtualConnection _vc, TCPReadRequestContext _tcpReadRequestContext, TCPReadCompletedCallback _userCallback, IOException _exception) {
            this.vc = _vc;
            this.tcpReadRequestContext = _tcpReadRequestContext;
            this.userCallback = _userCallback;
            this.exception = _exception;
            this.action = ERROR;
        }

        /**
         * Set the parameters to be used for the next piece of complete work.
         *
         * @param _vc
         * @param _tcpReadRequestContext
         * @param _userCallback
         */
        public void setCompleteParameters(VirtualConnection _vc, TCPReadRequestContext _tcpReadRequestContext, TCPReadCompletedCallback _userCallback) {
            this.vc = _vc;
            this.tcpReadRequestContext = _tcpReadRequestContext;
            this.userCallback = _userCallback;
            this.action = COMPLETE;
        }

        /**
         * This method will be called by a separate thread to actually do the work.
         */
        @Override
        public void run() {
            // Do the work represented by the action.
            if (action == READ) {
                // Do the read on this separate thread. Note last parameter shows this is called
                // from a queued thread.
                read(numBytes, userCallback, false, timeout, true);
            } else if (action == ERROR) {
                // Call the error callback.
                userCallback.error(vc, tcpReadRequestContext, exception);
            } else {
                // Call the complete callback.
                userCallback.complete(vc, tcpReadRequestContext);
            }
        }
    }
}
