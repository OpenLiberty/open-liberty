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

import java.util.concurrent.ExecutorService;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.ws.zos.channel.local.LocalCommDiscriminationData;
import com.ibm.ws.zos.channel.local.LocalCommServiceContext;
import com.ibm.ws.zos.channel.local.queuing.BlackQueueReadyCallback;
import com.ibm.ws.zos.channel.local.queuing.LocalChannelProvider;
import com.ibm.ws.zos.channel.local.queuing.NativeWorkRequest;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.DiscriminationProcess;
import com.ibm.wsspi.channelfw.Discriminator;
import com.ibm.wsspi.channelfw.InboundChannel;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.VirtualConnectionFactory;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;


/**
 * The Localcomm channel.  This class is purely the Java side of the channel.
 * The native half of the localcomm channel is managed via LocalChannelProvider.
 * This class interacts with LocalChannelProvider and manages its lifecycle.
 * 
 * Ascii-art schematic of the local comm channel:
 * 

                                     LocalCommChannel       
                                             |     \-------- LocalCommConnLink  <-----  LocalCommServiceContext  
                                             |      
                                             |
                                             |                     /----------->  LocalCommChannelFactoryProvider (DS) ------------------>  WOLAEndpoint (DS)
                       (java/CFW side)       |                    /                                                      (injected)
                   ..........................|.................../....
                       ("native" side)       |                  /
                                             |                 / (injected)
     (injected DS's)                         |                /
     ExecutorService --------\               V               /      
     NativeMethodManager ------->  LocalChannelProvider (DS)  
     DirectBufferHelper -----/               |                    
                                             |                                                                                                  
                                             |                                                                                                  
                                             |                                                                                                  
                  /--------------------------+-----------------------\                                                                          
                 /                           |                        \                                                                         
                /                            |                         \                                                                        
    NativeRequestHandler  <---  BlackQueueListenerThread  --->  BlackQueueDemultiplexor                                                                 
    (singleton)                 (singleton)                     (singleton)
                              (fka WorkRequestListenerThread)


    Notes:
    1. "Native side" means the Java code (and native code) that manages the local comm native queues
    
    2. WOLAEndpoint has a mandatory dependency on LocalCommChannelFactoryProvider to ensure
       LocalCommChannel component is available before the WOLA channel starts up and creates the CFW chain.
    
    3. LocalCommChannelFactoryProvider, in turn, has mandatory dependency on LocalCommNativeServices,
       to ensure native reqs are satisfied.

 *
 *
 */
public class LocalCommChannel implements InboundChannel, BlackQueueReadyCallback {

	/** 
	 * Config of this channel 
	 */
	private ChannelData channelConfig = null;
	
	/** 
	 * The discrimination process object for this channel.  It is set by CFW via
	 * the setDiscriminationProcess, and later retrieved by CFW via getDiscriminationProcess.
	 * All we need to do is maintain the ref to it.
	 */
	private DiscriminationProcess discriminationProcess = null;
	
	/**
	 * Constructor
	 */
	LocalCommChannel(ChannelData inputConfig) {
		this.channelConfig = inputConfig;
	}
	
	/**
	 * Returns a reference to LocalChannelProvider - the native side of the local comm channel.
	 * 
	 * Note: this method obtains the reference via a static call to LocalCommChannelFactoryProvider.
	 * This is a hack-ish solution to get around the fact that LocalCommChannel is managed by CFW
	 * but needs a reference to an OSGi DS (LocalChannelProvider), whose lifecycle is managed separately
	 * by OSGi.  I tried using ServiceTracker to obtain the service reference, but it turns out that
	 * ServiceTracker will return null if the bundle is stopping, even if the service it tracks hasn't
	 * actually been deactivated yet.  This causes problems during the stopping phase of LocalCommChannel,
	 * since it has lost its reference to the native side of the channel and therefore can't signal it
	 * to stop.
	 * 
	 * I'm sure there's a more kosher way to wire this together, but for now this will do. 
	 * 
	 * @return LocalChannelProvider - The service component that manages the native half of the channel.
	 */
	protected LocalChannelProvider getLocalChannelProvider() {
	    return LocalCommChannelFactoryProvider.getInstance().getLocalChannelProvider();
	}
	
    /**
     * @return - The ExecutorService DS component
     */
    public ExecutorService getExecutorService() {
        return LocalCommChannelFactoryProvider.getInstance().getExecutorService();
    }
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConnectionLink getConnectionLink(VirtualConnection vc) {
	    // TODO: conn links should be pooled for perf.
		LocalCommConnLink connLink = new LocalCommConnLink(vc, this);
		return connLink;
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
	public void init() throws ChannelException {
	}

	/**
     * {@inheritDoc}
     */
	@Override
	public void start() throws ChannelException {
	       
        getLocalChannelProvider().getBlackQueueDemultiplexor().registerNewConnectionCallback(this);

		getLocalChannelProvider().start();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Note: STOP shall move the channel back to the "init" state.  CFW may
	 * 	     subsequently try to START the channel again.
	 */
	@Override
	public void stop(long millisec) throws ChannelException {

	    // Close all existing open connections 
	    getLocalChannelProvider().getBlackQueueDemultiplexor().disconnectAll();
	    
		getLocalChannelProvider().stop(millisec);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Note: DESTROY shall move the channel back to the "un-inited" state. 
	 *       CFW may subsequently try to INIT the channel again.
	 */
	@Override
	public void destroy() throws ChannelException {
	}

	/**
	 * Returns the name of the channel as reported by the config data.
	 */
	@Override
	public String getName() {
		return channelConfig.getName();
	}

	/**
	 * Returns the interface that the application channel will use to
	 * interface with this channel and its data.
	 */
	@Override
	public Class<?> getApplicationInterface() {
		return LocalCommServiceContext.class;
	}

	/**
	 * This channel does not have a device interface.
	 */
	@Override
	public Class<?> getDeviceInterface() {
		throw new IllegalStateException("Not implemented and should not be used");
	}

	@Override
	public void update(ChannelData cc) {}

	/**
	 * This channel does not have a discriminator.
	 */
	@Override
	public Discriminator getDiscriminator() {
		throw new IllegalStateException("Not implemented and should not be used");
	}

	@Override
	public DiscriminationProcess getDiscriminationProcess() {
		return discriminationProcess;
	}

	@Override
	public void setDiscriminationProcess(DiscriminationProcess dp) {
		discriminationProcess = dp;
	}

	@Override
	public Class<?> getDiscriminatoryType() {
		return LocalCommDiscriminationData.class;
	}
	
	/**
	 * BlackQueueReadyCallback.ready - called for all new connections.
	 */
	@Override
	public void blackQueueReady(NativeWorkRequest nativeWorkRequest) {
	    try {
	        newConnection(nativeWorkRequest);
	    } catch (ChannelException channelException) {
	        // TODO? Or just let FFDC record it?
	    } catch (ChainException chainException) {
	        // TODO? Or just let FFDC record it?
	    }
	}
	
	/**
	 * BlackQueueReadyCallback.cancel.
	 * 
	 * This method will never be called, since we've registered as the newConnectionCallback
	 * and the newConnectionCallback is never cancelled.
	 */
	@Override
	public void cancel(Exception e) {}

	/**
	 * Accepts a new connection from the native layer.  This is driven thru the JNI.
	 * @throws ChainException 
	 * @throws ChannelException 
	 */
	public void newConnection(NativeWorkRequest nativeworkRequest) throws ChannelException, ChainException {
		
		// Create a VirtualConnection for this connection.
		VirtualConnectionFactory vcFactory = ChannelFrameworkFactory.getChannelFramework().getInboundVCFactory();
		VirtualConnection vc = vcFactory.createConnection();

		// Create a LocalCommConnLink for this connection.
		LocalCommConnLink connLink = (LocalCommConnLink) getConnectionLink(vc);
		connLink.setConnectWorkRequest(nativeworkRequest);
		
		// Call ready to perform discrimination of the upstream channels.
		connLink.ready(vc);
	}
}
