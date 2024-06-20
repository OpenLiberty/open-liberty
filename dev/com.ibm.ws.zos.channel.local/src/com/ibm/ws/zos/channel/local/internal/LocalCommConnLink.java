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
package com.ibm.ws.zos.channel.local.internal;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.zos.channel.local.LocalCommClientConnHandle;
import com.ibm.ws.zos.channel.local.queuing.BlackQueueReadyCallback;
import com.ibm.ws.zos.channel.local.queuing.NativeWorkRequest;
import com.ibm.ws.zos.channel.local.queuing.NativeWorkRequestType;
import com.ibm.ws.zos.channel.local.queuing.internal.NativeRequestHandler;
import com.ibm.wsspi.channelfw.ConnectionReadyCallback;
import com.ibm.wsspi.channelfw.DiscriminationProcess;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.base.InboundProtocolLink;

/**
 * The CFW ConnectionLink class for the LocalCommChannel. Currently the
 * only upstream channel for LocalComm is WOLA, so the upstream (aka "app-side")
 * ConnectionLink for this guy is WolaConnLink.
 * 
 * LocalCommConnLink.getApplicationCallback() <----------> WolaConnLink.getDeviceLink()
 * LocalCommServiceContext <------------------------------------------+ getDeviceLink().getChannelAccessor
 * 
 */
public class LocalCommConnLink extends InboundProtocolLink {

    /**
     * Trace component for this class.
     */
    private final static TraceComponent tc = Tr.register(LocalCommConnLink.class);

    /**
     * LocalComm service context for this connection. This object is returned by
     * the call to getChannelAccessor. It implements LocalCommServiceContext,
     * which is the ApplicationInterface for LocalCommChannel.
     */
    private LocalCommServiceContextImpl localCommServiceContext = null;

    /**
     * Channel owning this conn link.
     */
    private LocalCommChannel localCommChannel = null;

    /**
     * A reference to the NativeWorkRequest element that created this connection.
     * It contains a ref to the native connection handle (LocalCommClientConnHandle).
     */
    private NativeWorkRequest connectWorkRequest;

    /**
     * Indication if the native close routine has been driven for this connection.
     */
    private boolean nativeCloseDriven = false;

    /**
     * The disconnectSemaphore is used to communicate between READREADY threads and
     * DISCONNECT threads, to ensure the READREADY is finished reading before the DISCONNECT
     * blows away the connection.  See waitForDisconnectLock() for more details.
     */
    private Semaphore disconnectSemaphore = new Semaphore(1);

    /**
     * Constructor.
     * 
     * @param vc
     */
    public LocalCommConnLink(VirtualConnection vc, LocalCommChannel channel) {
        init(vc);
        this.localCommChannel = channel;
        this.localCommServiceContext = new LocalCommServiceContextImpl(this);
    }

    /**
     * Query the channel that owns this object.
     * 
     * @return LocalCommChannel
     */
    protected LocalCommChannel getChannel() {
        return this.localCommChannel;
    }

    /**
     * Called by LocalCommChannel when a new connection arrives.
     */
    @Override
    public void ready(VirtualConnection vc) {

        DiscriminationProcess dp = getChannel().getDiscriminationProcess();

        int state = DiscriminationProcess.FAILURE;
        try {

            // The nativeWorkRequest associated with this new connection is used for discrimination
            // of the upstream channels.
            state = dp.discriminate(getVirtualConnection(), connectWorkRequest, this);

            if (DiscriminationProcess.SUCCESS == state) {
                connectAccepted();
                safeGetApplicationCallback().ready(vc);

            } else if (DiscriminationProcess.FAILURE == state) {
                // No upstream channels can handle the connection. 
                // Throw an exception, which will be caught (below) and the connection closed.
                throw new Exception("Discrimination failed");
            }
        } catch (Exception e) {
            close(vc, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getChannelAccessor() {
        return localCommServiceContext;
    }

    /**
     * Things to do for a close:
     * 
     * 1) close the native half of the connection
     * 2) cancel all black queue callbacks for this connection
     * 
     * @{inheritDoc
     */
    @Override
    public void close(VirtualConnection vc, Exception e) {
        
        closeNative(e);
 
        // Inform registered callbacks that the connection is closing.
        getChannel().getLocalChannelProvider().getBlackQueueDemultiplexor().cancelCallbacks(getClientConnectionHandle(), e);

        // Send destroy up the chain.
        destroy(e);
        
        if (vc != null) {
            vc.destroy();
        }
    }
    
    /**
     * Encapsulate logic that makes sure ntv_close is called ONLY ONCE for this conn link.
     */
    private void closeNative(Exception e) {
        // Close the native side of the connection.  
        try {
            // Ensure invoked only once
            boolean driveClose = false;
            synchronized (connectWorkRequest) {
                if (nativeCloseDriven == false) {
                    nativeCloseDriven = true;
                    driveClose = true;
                }
            }

            if (driveClose) {
                if (e != null) {
                    dumpNativeControlBlocks(e);
                }
                
                getNativeRequestHandler().close(connectWorkRequest);
            }
        } catch (IOException e1) {
            // Allow FFDC to be gathered.
        }
    }
    
    /**
     * Called from closeNative when we're closing the connection due to an exception.
     * Dump the contents of the conn's native control blocks (footprint tables, etc) in an FFDC.
     */
    private void dumpNativeControlBlocks(Exception e) {
        try {
            List<String> nativeData = getNativeRequestHandler().dumpNativeControlBlocks(connectWorkRequest);
            
            FFDCFilter.processException(e, 
                                        "LocalCommConnLink", 
                                        "closeNative",
                                        this,
                                        nativeData.toArray());
        } catch (Exception e1) {
            // Oh well, didn't work.  Don't let the exception percolate - 
            // we still need to finish the close.
        }
    }

    
    /**
     * Destroy resources held by this object.
     */
    @Override
    protected void destroy() {
        super.destroy();
        
        // Remove the mapping from native conn handle to this conn link.
        getConnHandleToConnLinkMap().remove( getClientConnectionHandle() );
    }

    /**
     * Cache the NativeWorkRequest element that initially created this connection.
     * 
     * @param nativeWorkRequest
     */
    protected void setConnectWorkRequest(NativeWorkRequest nativeWorkRequest) {
        this.connectWorkRequest = nativeWorkRequest;
    }

    /**
     * @return the connectWorkRequest associated with this connection.
     */
    public NativeWorkRequest getConnectWorkRequest() {
        return connectWorkRequest;
    }

    /**
     * @return The native connection handle for this connection.
     */
    public LocalCommClientConnHandle getClientConnectionHandle() {
        return connectWorkRequest.getClientConnectionHandle();
    }

    /**
     * @return The NativeRequestHandler, for invoking methods against the native side of the channel.
     */
    public NativeRequestHandler getNativeRequestHandler() {
        return getChannel().getLocalChannelProvider().getNativeRequestHandler();
    }
    
    /**
     * @return A mapping of native conn handle to LocalCommConnLinks.
     */
    private Map<LocalCommClientConnHandle, LocalCommConnLink> getConnHandleToConnLinkMap() {
        return getChannel().getLocalChannelProvider().getConnHandleToConnLinkMap();
    } 

    /**
     * Called whenever a new connection is accepted (meaning it successfully passed
     * discrimination).
     * 
     * Registers with the BlackQueueDemultiplexor for DISCONNECT work.
     * 
     * Informs the native half of the channel that the connection has been accepted.
     * The native half then informs the client side.
     * 
     */
    private void connectAccepted() throws IOException {
                
        // Map the native conn handle to this conn link.
        getConnHandleToConnLinkMap().put( getClientConnectionHandle(), this );

        getChannel().getLocalChannelProvider().getBlackQueueDemultiplexor().registerCallback(new DisconnectCallback(),
                                                                                             NativeWorkRequestType.REQUESTTYPE_DISCONNECT,
                                                                                             getClientConnectionHandle());

        getNativeRequestHandler().nativeConnectAccepted(connectWorkRequest);
    }
    
    /**
     * The application callback is set to null in LocalCommConnLink.destroy.
     * Other threads might still be trying to access the application callback
     * and most likely they are not expecting a null, which results in 
     * NullPointerExceptions, which look like bugs.  In this case, it is
     * much more likely that the null is due to the conn closing.  So intead
     * of causing NPEs, we wrap getApplicationCallback with a
     * null check and throw a RuntimeException with a clear message instead.
     * 
     * @see com.ibm.wsspi.channelfw.ConnectionLink#getApplicationCallback()
     * 
     * @throws RuntimeException if the app callback is null.
     */
    protected ConnectionReadyCallback safeGetApplicationCallback() {
        ConnectionReadyCallback appCallback = super.getApplicationCallback();
        
        if (appCallback == null) {
            throw new RuntimeException(new IOException("ApplicationCallback is null. The connection has been destroyed"));
        }

        return appCallback;
    }

    /**
     * This method is called by the BlackQueueListenerThread when it receives a
     * READREADY work request for this connection.  The lock will be released by
     * the READREADY thread after it has finished reading off the data.
     * 
     * The purpose of the lock is to prevent a subsequent DISCONNECT from blowing 
     * away the connection before the READREADY is finished reading (see waitForDisconnectLock).
     */
    public boolean obtainDisconnectLock() {
        return disconnectSemaphore.tryAcquire();     // Do not block.
    }
    
    /**
     * This method is called by the upstream channel during a READREADY request 
     * after it has finished reading the data.  After calling this method it is
     * safe to disconnect the connection (i.e. no READREADY request still processing). 
     */
    protected void releaseDisconnectLock() {
        disconnectSemaphore.release();
        
        // Debug - see if we messed up the semaphore
        if (tc.isDebugEnabled()) {
        	int permitsAvailable = disconnectSemaphore.availablePermits();
        	if (permitsAvailable > 1) {
        		Tr.debug(tc, "Disconnect semaphore invalid state: " + permitsAvailable);
        	}
        }
    }
    
    /**
     * Wait at most timeout_ms milliseconds for the disconnect semaphore
     * to become available.
     * 
     * The purpose of this is to avoid timing situation where, for example, a
     * client queues a READREADY and DISCONNECT work request one right after
     * another.  The two requests are pulled off the native black queue in 
     * guaranteed order (BlackQueueListenerThread); however then they get submitted 
     * to Java's ExecutorService, which may run them in any order. So the DISCONNECT 
     * may run and blow away the connection before the server has a chance to read 
     * and process the data for the READREADY request.
     * 
     * The quick fix for this is to acquire the disconnectSemaphore prior 
     * to submitting a READREADY request to the ExecutorService (see BlackQueueListenerThread). 
     * The thread processing the READREADY will eventually release the semaphore
     * when it's done reading the data (see LocalCommServiceContext.okToDisconnect).
     *  
     * Meanwhile the DISCONNECT callback will call this method and block until the
     * semaphore is available. The thread will block for at most timeout_ms.  After that, 
     * we let the disconnect proceed (don't want to hold it up forever in case the
     * READREADY thread died or something).
     * 
     * @param timeout_ms max time to wait in milliseconds
     */ 
    public void waitForDisconnectLock(long timeout_ms) {
        
        boolean acquired = false;
        try {
            acquired = disconnectSemaphore.tryAcquire(timeout_ms, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            // Just FFDC it.
        }
        
        if (!acquired) {
            // We were not able to acquire the semaphore. This is unusual and indicates
            // that a previous READREADY request didn't properly release the semaphore.
            // Issue an FFDC message
            FFDCFilter.processException(new WaitForDisconnectLockException(timeout_ms), 
                                        "LocalCommConnLink", 
                                        "waitForDisconnectLock", 
                                        this);
        } else {
            // Immediately release. We don't need to hold it anymore.
            disconnectSemaphore.release();
        }
    }

    /**
     * Inner class implements black queue callback for DISCONNECT requests.
     */
    private class DisconnectCallback implements BlackQueueReadyCallback {
        @Override
        public void blackQueueReady(NativeWorkRequest nativeWorkRequest) {
            close(getVirtualConnection(), null);
        }

        @Override
        public void cancel(Exception e) {
            // We must be closing the connection.
            // Nothing else to do here.
        }
    }
    
    /**
     * 
     */
    private class WaitForDisconnectLockException extends Exception {
        public WaitForDisconnectLockException(long timeout_ms) {
            super("Could not acquire the disconnectSemaphore within the given timeout (" + timeout_ms + "ms). "
                  + " The connection will now be disconnected.  This could cause failures in other threads"
                  + " that may be trying to read from the connection.  Native connection handle: " 
                  + getClientConnectionHandle());
        }
    }
}



    

