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
package com.ibm.ws.tcpchannel.internal;

import java.io.IOException;
import java.net.SocketTimeoutException;

import com.ibm.io.async.AsyncTimeoutException;
import com.ibm.io.async.IAbstractAsyncFuture;
import com.ibm.io.async.IAsyncFuture;
import com.ibm.io.async.ICompletionListener;
import com.ibm.io.async.TimerWorkItem;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.channelfw.VirtualConnection;

class AioReadCompletionListener implements ICompletionListener {
    private static final TraceComponent tc = Tr.register(AioReadCompletionListener.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    private static volatile IOException connClosedException = null; // volatile to ensure other threads see the constructed exception

    AioReadCompletionListener() {
        // nothing needs to be done in constructor
    }

    @Override
    public void futureCompleted(IAbstractAsyncFuture future, Object o) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "AioReadCompletionListener.futureCompleted");
        }
        AioTCPReadRequestContextImpl req = (AioTCPReadRequestContextImpl) o;
        boolean errorOccurred = false;
        IOException ioe = null;
        long byteCount = 0;
        boolean complete = false;
        VirtualConnection vci = null;

        // New timeout code
        // Cancel timeout request
        TimerWorkItem twi = future.getTimeoutWorkItem();
        if (twi != null) {
            twi.state = TimerWorkItem.ENTRY_CANCELLED;
        }

        // check if close has already occurred
        vci = req.getTCPConnLink().getVirtualConnection();

        // looking at the vci is messy here, but we're not doing the try, catch
        // to save synchronization logic. vci could be null if connection
        // is closing before we can asked to request permission to finish
        // the read.
        if (vci != null) {
            if ((vci.isInputStateTrackingOperational())) {
                if (!vci.requestPermissionToFinishRead()) {
                    // can't get permission, so throw away request
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Can't get permission to finish read, throwing read request away.");
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "AioReadCompletionListener.futureCompleted");
                    }
                    return;
                }
            }
        } else {
            // can't get the virtual connection object, so throw away request
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Can't get virtual connection object, throwing read request away.");
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "AioReadCompletionListener.futureCompleted");
            }
            return;
        }

        try {
            IAsyncFuture fut = (IAsyncFuture) future;
            byteCount = fut.getByteCount();
            // if a JIT buffer was used instead of the supplied buffers, replace them
            if (fut.getJITBuffer() != null) {
                req.setBuffer(fut.getJITBuffer());
            }

            if (byteCount == 0) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "getByteCount returned 0, channel must be closed ");
                }
                errorOccurred = true;

                if (connClosedException == null) {
                    connClosedException = new IOException("Read failed: Connection closed by peer.");
                }
                ioe = connClosedException;
            } else {
                // Need to postProcess the buffers here, for indirect buffer support
                req.postProcessReadBuffers(byteCount);
                complete = req.updateIOCounts(byteCount, 0);
            }

        } catch (AsyncTimeoutException ate) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "exception caught: " + ate);
            }
            // convert AsyncTimeoutException to SocketTimeoutException
            ioe = new SocketTimeoutException(ate.getMessage());
            ioe.initCause(ate);
            errorOccurred = true;

        } catch (IOException ex) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "exception caught: " + ex);
            }
            ioe = ex;
            errorOccurred = true;

        } catch (InterruptedException ie) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "AioReadCompletionListener caught exception. " + ie);
            }
            FFDCFilter.processException(ie, getClass().getName() + ".futureCompleted", "134");
        }

        if (!errorOccurred) {
            if (complete) {
                future.setFullyCompleted(true);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "calling callback.complete method");
                }
                req.getReadCompletedCallback().complete(req.getTCPConnLink().getVirtualConnection(), req);
            } else {
                // we didn't get all the data we needed
                // update stats for partial read
                if (req.getTCPConnLink().getConfig().getDumpStatsInterval() > 0) {
                    req.getTCPConnLink().getTCPChannel().totalPartialAsyncReads.incrementAndGet();
                }
                // reset the timeout value if not infinite
                long remainingTimeout = req.getTimeoutInterval();
                if (remainingTimeout != 0) {
                    // timeout specified, reset it to the time left
                    remainingTimeout = req.getTimeoutTime() - System.currentTimeMillis();
                    // if timeout already expired, set IOException
                    if (remainingTimeout <= 0) {
                        ioe = new SocketTimeoutException("Async read timed out after reading partial data");
                        errorOccurred = true;
                    }
                }
                if (!errorOccurred) {
                    // still no errors, do another async read
                    try {
                        ((AioSocketIOChannel) req.getTCPConnLink().getSocketIOChannel()).readAIO(req, true, remainingTimeout);
                    } catch (IOException ex) {
                        ioe = ex;
                        errorOccurred = true;
                    }
                }
            }
        }
        if (errorOccurred) {
            if (req.getJITAllocateAction() == true && (req.getBuffer() != null)) {
                req.getBuffer().release();
                req.setBuffer(null);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "IOException while doing IO requested on local: " + req.getTCPConnLink().getSocketIOChannel().getSocket().getLocalSocketAddress() + " remote: "
                             + req.getTCPConnLink().getSocketIOChannel().getSocket().getRemoteSocketAddress());
                Tr.event(tc, "Calling read error callback with Exception is: " + ioe);
            }
            req.getReadCompletedCallback().error(req.getTCPConnLink().getVirtualConnection(), req, ioe);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "AioReadCompletionListener.futureCompleted");
        }
    }

}
