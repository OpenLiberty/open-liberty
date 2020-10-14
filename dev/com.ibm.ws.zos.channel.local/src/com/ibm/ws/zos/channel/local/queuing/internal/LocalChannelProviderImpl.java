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
package com.ibm.ws.zos.channel.local.queuing.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.zos.channel.local.LocalCommClientConnHandle;
import com.ibm.ws.zos.channel.local.internal.LocalCommConnLink;
import com.ibm.ws.zos.channel.local.queuing.BlackQueueDemultiplexor;
import com.ibm.ws.zos.channel.local.queuing.LocalChannelProvider;
import com.ibm.ws.zos.channel.local.queuing.NativeWorkRequest;
import com.ibm.ws.zos.core.Angel;
import com.ibm.ws.zos.core.NativeService;

import com.ibm.ws.zos.core.utils.DirectBufferHelper;
import com.ibm.ws.zos.core.utils.DoubleGutter;
import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.wsspi.logging.Introspector;

/**
 * Main DS component for the native side of the local comm channel.
 * 
 * This component is injected with required services for the native side of
 * the local comm channel (e.g. ExecutorService, NativeMethodManager, etc).
 * 
 * LocalCommChannelFactoryProvider, which provides the LocalCommChannelFactory
 * to CFW, has a mandatory dependency on this component.  Therefore, this component
 * must be activated before any LocalCommChannels can be created.
 * 
 * This class also maintains a mapping of LocalCommClientConnHandle -> LocalCommConnLink.
 * There are several paths up from the native code that pass only a LocalCommClientConnHandle.
 * The map allows us to associate the handle with the LocalCommConnLink for that connection.
 * 
 */
@Component(configurationPolicy=ConfigurationPolicy.IGNORE, immediate=true, property="service.vendor=IBM",
  reference=@Reference(name="NativeService", service=NativeService.class, target="(&(native.service.name=LCOMINIT)(is.authorized=true))"))
public class LocalChannelProviderImpl implements LocalChannelProvider, Introspector {

    private static final TraceComponent tc = Tr.register(LocalChannelProviderImpl.class);

	/**
	 * Reference to the native method manager that allows for JNI interaction with native code.
	 */
    @Reference
    protected NativeMethodManager nativeMethodManager;
    
    /**
     * NativeRequestHandler contains a bunch of JNI methods for interacting with the native
     * local comm code.  It's a stateless singleton object.
     */
    protected NativeRequestHandler nativeRequestHandler;

    /**
     * ExecutorService reference for queueing inbound work -- required
     */
    @Reference
    private ExecutorService executorService;
    
    /**
     * The instance of the buffer management code used to read main memory.
     */
    @Reference
    private DirectBufferHelper directBufferHelper;
    
    /**
     * The instance of the double gutter code used to display a byte array in double gutter fashion.
     */
    @Reference
    private DoubleGutter doubleGutter;
    
    /**
     * Recovery requirement. The LCOM native code has a logical dependency on the
     * HardFailureNativeCleanup service.  When LCOM is init'd it will register a native
     * LCOM cleanup routine (registerLCOMHardFailureRtn()) into the server_process_data structure. 
     * Our native task-level resmgr will check for a "marked" Thread/TCB going through termination. 
     * If it sees such a "marked" Thread it will drive the registered native cleanup routines. 
     * Seeing the "marked" Thread indicates that this server has taken a Hard failure (such as a 
     * Kill -9 or a Runtime.halt(), or an unrecoverable native abend). So, we need this Service 
     * dependency on HardFailureNativeCleanup which is the Service that manages (starts and stops) 
     * the native "marked" Thread to trigger the cleanup to prevent hung servers.
     */
    
    /**
     * The instance of the thread that processes NativeWorkRequests.
     */
    private BlackQueueListenerThread blackQueueListenerThread = null;
 
    /**
     * The BlackQueueDemultiplexor, for routing black queue requests to the appropriate handler.
     */
    private BlackQueueDemultiplexorImpl blackQueueDemultiplexor = new BlackQueueDemultiplexorImpl();
    
    /**
     * A mapping of native conn handles to LocalCommConnLinks. 
     * 
     * This mapping is populated as new connections arrive (see LocalCommConnLink.ready).
     * It is de-populated as connections are destroyed (see LocalCommConnLink.destroy).
     */
    private Map<LocalCommClientConnHandle, LocalCommConnLink> connHandleToConnLinkMap = new ConcurrentHashMap<LocalCommClientConnHandle, LocalCommConnLink>();

    @Activate    
    protected void activate(org.osgi.framework.BundleContext context) {
    	// Lets make sure that we're connected to an Angel that supports what we need
    	// to do.  We require that the authorized common function module be passed a
    	// client token that is *NOT* the STOKEN.  The STOKEN is not unique enough,
    	// since a server process might be a BPXAS and therefore re-use a STOKEN.
    	// We're checking manually so that we can issue a message explaining what is
    	// wrong, if something is wrong.
        org.osgi.framework.ServiceReference<?> angelService = context.getServiceReference(Angel.class.getCanonicalName());
        if (angelService != null) {
            int angelVersion = Integer.parseInt((String)angelService.getProperty(Angel.ANGEL_DRM_VERSION));
            if ((angelVersion >= 0) && (angelVersion <= 2)) {
            	Object[] messageFillins = new Object[] {Integer.toString(angelVersion), "3"};
            	Tr.error(tc, "ANGEL_VERSION_INCOMPATIBLE", messageFillins);

            	TraceNLS nls = TraceNLS.getTraceNLS(this.getClass(), "com.ibm.ws.zos.channel.local.internal.resources.ZMessages");
            	throw new RuntimeException(nls.getFormattedMessage("ANGEL_VERSION_INCOMPATIBLE", messageFillins, null));
            }
        } else {
        	Tr.error(tc, "ANGEL_VERSION_INCOMPATIBLE", new Object[]{"UNKNOWN", "3"});
        }
        
        // Attempt to load native code via the method manager.
        Object[] callbackData = new Object[] { LocalCommServiceResults.class, "setResults" };
        nativeMethodManager.registerNatives(LocalChannelProviderImpl.class, callbackData);

        init();
        // Note: this "channel" is started/stopped from LocalCommChannel.

        nativeRequestHandler = new NativeRequestHandler(this);
        
        // Register the FFDC callback for logging FFDC records for failures in the native code.
        blackQueueDemultiplexor.registerFfdcCallback( new FFDCCallback() );
    }

    @Deactivate
    protected void deactivate() {
    	
        // Note: this "channel" is usually started/stopped from LocalCommChannel.
        // However, we absolutely must make sure to shutdown the native listener thread, 
        // otherwise the server will never stop.  Hence, the call to stop here.
        stop(0);
        
        destroy();
    }

    /**
     * Retrieves the executor service reference.
     * 
     * @return CollaborationEngine
     */
    public ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * @return The DirectBufferHelper service.
     */
    protected DirectBufferHelper getDirectBufferHelper() {
        return directBufferHelper;
    }
    
    /**
     * @return The DoubleGutter service.
     */
    protected DoubleGutter getDoubleGutter() {
        return doubleGutter;
    }
    
    /**
     * @return The NativeRequestHandler
     */
    public NativeRequestHandler getNativeRequestHandler() {
        return nativeRequestHandler;
    }
    
    /**
     * Drive native to initialize needed structures
     */
    public void init() {

        int rc = ntv_initializeChannel();

        if (rc == -1) {
            // Initialization failed ... issue message and throw.
            LocalCommServiceResults.getLComServiceResult().issueLComServiceMessage();
            throw new RuntimeException("Native LCOM Channel failed init: " + LocalCommServiceResults.getLComServiceResult().toString());
        }
    }
    
   
    /**
     * Start the work queue thread (aka "black queue").
     */
    public void start() {
        startBlackQueueListenerThread();
    }
    
    /**
     * Stop the work queue thread.
     */
    public void stop(long millisec) {
        stopBlackQueueListenerThread();
    }

    /**
     * Drive native to UN-initialize data structures
     */
    public void destroy() {

        int rc = ntv_uninitializeChannel();

        if (rc == -1) {
            // Native call failed ... issue message.
        	LocalCommServiceResults.getLComServiceResult().issueLComServiceMessage();
        	throw new RuntimeException("Native LCOM Channel failed destroy: " + LocalCommServiceResults.getLComServiceResult().toString());
        }
    }
    
    /**
     * Start the thread that listens for native LCom requests on the black queue.
     */
    private void startBlackQueueListenerThread() {

        blackQueueListenerThread = AccessController.doPrivileged(new PrivilegedAction<BlackQueueListenerThread>() {
            @Override
            public BlackQueueListenerThread run() {
                return new BlackQueueListenerThread(LocalChannelProviderImpl.this);
            }
        });

        blackQueueListenerThread.start();
    }

    /**
     * Stop the thread that listens for native LCom requests on the black queue.
     */
    private void stopBlackQueueListenerThread() {
        if (blackQueueListenerThread != null) {
            blackQueueListenerThread.end();
            blackQueueListenerThread = null;
        }
    }
    
    /**
     * @return the BlackQueueDemultiplexor.
     */
    @Override
    public BlackQueueDemultiplexor getBlackQueueDemultiplexor() {
        return blackQueueDemultiplexor;
    }
    
    /**
     * @return the conn handle to conn link map.
     */
    @Override
    public Map<LocalCommClientConnHandle, LocalCommConnLink> getConnHandleToConnLinkMap() {
        return connHandleToConnLinkMap;
    }
    
    /**
     * @return the CONNECT NativeWorkRequest associated with the given clientConnHandle/LocalCommConnLink,
     *         or null if the LocalCommConnLink associated with the given clientConnHandle has been
     *         destroyed.
     */
    protected NativeWorkRequest getConnectWorkRequest(LocalCommClientConnHandle clientConnHandle) {
        LocalCommConnLink connLink = connHandleToConnLinkMap.get(clientConnHandle);
        return (connLink != null) ? connLink.getConnectWorkRequest() : null;
    }
    

	@Override
	public boolean isBlackQueueListenerThread() {
		return Thread.currentThread().equals(blackQueueListenerThread);
	}

	/** 
     * {@inheritDoc} 
     */
    @Override
    public String getIntrospectorDescription() {
        return "Introspect CFW Local Channel z/OS.";
    }

    /** 
     * {@inheritDoc} 
     */
    @Override
    public String getIntrospectorName() {
        return "LocalChannelProviderImpl";
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void introspect(PrintWriter writer) throws IOException {
        
        
        //TODO synchronize with close()

        // Write Native ControlBlock info
    	writer.println();
    	writer.println();
        writer.write("LocalComm Native Control Blocks (LHDL, LSCL, LOCL, LDAT)");
    	writer.println();
    	writer.println();

        // Write registered callback info (in blackQueueDemultiplexor) and gather lhdl pointers.
        Set<LocalCommClientConnHandle> clientConnHandles = blackQueueDemultiplexor.introspect(writer);
        
        // Dump out all connection handles.
        for (String s : dumpNativeControlBlocks(clientConnHandles)) {
            writer.println(s);
        }
        
        // Statistics for the listener thread
        blackQueueListenerThread.printStatistics(writer);
    }

 
    /**
     * @return A list of Stringified native control block data (double guttered).
     */
    protected List<String> dumpNativeControlBlocks(Iterable<LocalCommClientConnHandle> clientConnHandles) throws IOException {
        IntrospectHelper introspectHelper = new IntrospectHelper(this);
        
        List<String> retMe = new ArrayList<String>();
        for (LocalCommClientConnHandle clientConnHandle : clientConnHandles) {
            
            NativeWorkRequest connectWorkRequest = getConnectWorkRequest( clientConnHandle );
            if (connectWorkRequest != null) {
                nativeRequestHandler.dumpNativeControlBlocks(connectWorkRequest, introspectHelper);
            } else {
                // The connection must have been destroyed.  Just dump out the raw conn handle.
                retMe.add("LocalCommClientConnHandle: " + clientConnHandle.toString());
                retMe.add("LocalCommConnLink has been destroyed.  Cannot dump native control blocks.");
            }
            
            retMe.add("");  // Formatting.
        }
        return retMe;
    }
     
    //-------------------------------------------------------------------------
    // Native Services
    //-------------------------------------------------------------------------
    protected native int ntv_initializeChannel();

    protected native int ntv_uninitializeChannel();
}

