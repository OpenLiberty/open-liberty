/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.channelfw.osgi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.io.async.IAsyncProvider.AsyncIOHelper;
import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChannelUtils;
import com.ibm.websphere.channelfw.EndPointMgr;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.event.EventEngine;
import com.ibm.websphere.event.ScheduledEventService;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.bytebuffer.internal.ByteBufferConfiguration;
import com.ibm.ws.channelfw.internal.ChannelFrameworkConstants;
import com.ibm.ws.channelfw.internal.ChannelFrameworkImpl;
import com.ibm.ws.channelfw.internal.chains.EndPointMgrImpl;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.feature.ServerStarted;
import com.ibm.ws.tcpchannel.internal.TCPChannelFactory;
import com.ibm.ws.udpchannel.internal.UDPChannelFactory;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.channelfw.HttpProtocolBehavior;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;
import com.ibm.wsspi.timer.ApproximateTime;
import com.ibm.wsspi.timer.QuickApproxTime;

/**
 * OSGi public bundle API for the channel framework. This allows cross bundle
 * dependency to be defined against the framework and provides proper access to
 * the framework itself.
 */
@Component(service = { CHFWBundle.class, ServerQuiesceListener.class },
           name = "com.ibm.ws.channelfw",
           configurationPid = "com.ibm.ws.channelfw",
           configurationPolicy = ConfigurationPolicy.OPTIONAL,
           immediate = true,
           property = { "service.vendor=IBM" })
public class CHFWBundle implements ServerQuiesceListener {

    /** Trace service */
    private static final TraceComponent tc = Tr.register(CHFWBundle.class,
                                                         ChannelFrameworkConstants.BASE_TRACE_NAME,
                                                         ChannelFrameworkConstants.BASE_BUNDLE);

    /**
     * Active HttpDispatcher instance. May be null between deactivate and activate
     * calls.
     */
    private static final AtomicReference<CHFWBundle> instance = new AtomicReference<CHFWBundle>();

    /** Reference to the channel framework */
    private ChannelFrameworkImpl chfw = null;
    /** Reference to ByteBufferConfiguration */
    private ByteBufferConfiguration wsbbmgr = null;
    /** Reference to the event service */
    private EventEngine eventService = null;
    /** Reference to the scheduler service -- required */
    private ScheduledEventService scheduler = null;

    private ScheduledExecutorService scheduledExecutor = null;
    /** Reference to the executor service -- required */
    private ExecutorService executorService = null;

    private static AtomicBoolean serverCompletelyStarted = new AtomicBoolean(false);
    private static Queue<Callable<?>> serverStartedTasks = new LinkedBlockingQueue<>();
    private static Object syncStarted = new Object() {
    }; // use brackets/inner class to make lock appear in dumps using class name

    private volatile ServiceReference<HttpProtocolBehavior> protocolBehaviorRef;
    private static volatile String httpVersionSetting = null;
    private static volatile boolean versionSet = false;
    private static volatile boolean default20Off = false;
    private static volatile boolean default20On = false;

    /** The channel will disable HTTP/2.0 by default. */
    private static final String OPTIONAL_DEFAULT_OFF_20 = "2.0_Optional_Off";
    /** The channel will be enabled for HTTP/2.0 by default". */
    private static final String OPTIONAL_DEFAULT_ON_20 = "2.0_Optional_On";

    /**
     * Constructor.
     */
    public CHFWBundle() {
        this.chfw = ChannelFrameworkImpl.getRef();
    }

    /**
     * DS method for activating this component.
     *
     * @param context
     */
    @Activate
    protected void activate(ComponentContext context, Map<String, Object> config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Activating ", config);
        }

        // handle config (such as TCP factory info) before registering
        // factories, as the register will trigger an automatic load of any
        // delayed configuration, and we need the config before that happens
        modified(config);

        this.chfw.registerFactory("TCPChannel", TCPChannelFactory.class);
        this.chfw.registerFactory("UDPChannel", UDPChannelFactory.class);

        instance.set(this); // required components have been activated
    }

    /**
     * DS method for deactivating this component.
     *
     * @param context
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Deactivating");
        }
        instance.compareAndSet(this, null);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Destroying all endpoints");
        }
        EndPointMgrImpl.destroyEndpoints();

        try {
            this.chfw.destroy();
        } catch (Throwable t) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Error destroying framework: " + t);
            }
        }

        this.chfw.deregisterFactory("TCPChannel");
        this.chfw.deregisterFactory("UDPChannel");
    }

    /**
     * Modified method. This method is called when the
     * service properties associated with the service are updated through a
     * configuration change.
     *
     * @param cfwConfiguration
     *                             the configuration data
     */
    @Modified
    protected synchronized void modified(Map<String, Object> cfwConfiguration) {

        if (null == cfwConfiguration) {
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Processing config", cfwConfiguration);
        }

        this.chfw.updateConfig(cfwConfiguration);
    }

    @Reference(service = AsyncIOHelper.class, cardinality = ReferenceCardinality.OPTIONAL)
    protected void setAsyncIOHelper(AsyncIOHelper asyncIOHelper) {
        chfw.setAsyncIOHelper(asyncIOHelper);
    }

    protected void unsetAsyncIOHelper(AsyncIOHelper asyncIOHelper) {
        chfw.setAsyncIOHelper(null);
    }

    /**
     * When notified that the server is going to stop, pre-quiesce all chains in the runtime.
     * This will be called before services start getting torn down..
     *
     * @see com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener#serverStopping()
     */
    @Override
    public void serverStopping() {
        // If the system is configured to quiesce connections..
        long timeout = chfw.getDefaultChainQuiesceTimeout();
        if (timeout > 0) {
            ChainData[] runningChains = chfw.getRunningChains();

            // build list of chain names to pass to stop, use ChannelUtils.stopChains so as to not return until the
            // channels are inactive or the quiesce timeout has hit.
            List<String> names = new ArrayList<String>();
            for (int i = 0; i < runningChains.length; i++) {
                if (FlowType.INBOUND.equals(runningChains[i].getType())) {
                    names.add(runningChains[i].getName());
                }
            }

            ChannelUtils.stopChains(names, -1, null);

        }
    }

    /**
     * Declarative services method that is invoked once the server is started.
     * Only after this method is invoked is the initial polling for
     * persistent tasks performed.
     *
     * @param ref reference to the ServerStarted service
     */
    @Reference(service = ServerStarted.class,
               policy = ReferencePolicy.DYNAMIC,
               cardinality = ReferenceCardinality.OPTIONAL,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setServerStarted(ServiceReference<ServerStarted> ref) {
        // set will be called when the ServerStarted service has been registered (by the FeatureManager as of 9/2015).  This is a signal that
        // the server is fully started, but before the "smarter planet" message has been output. Use this signal to run tasks, mostly likely tasks that will
        // finish the port listening logic, that need to run at the end of server startup

        Callable<?> task;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "CHFW signaled- Server Completely Started signal received");
        }
        while ((task = serverStartedTasks.poll()) != null) {
            try {
                task.call();
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "caught exception performing late cycle server startup task: " + e);
                }
            }
        }

        synchronized (syncStarted) {
            serverCompletelyStarted.set(true);
            syncStarted.notifyAll();
        }
    }

    /**
     * Method is called to run a task if the server has already started, if the server has not started that task is queue to be run when the server start signal
     * has been received.
     *
     * @param callable - task to run
     * @return Callable return null if the task was not ran, but queued, else return the task to denote it has ran.
     * @throws Exception
     */
    public static <T> T runWhenServerStarted(Callable<T> callable) throws Exception {
        synchronized (syncStarted) {
            if (!serverCompletelyStarted.get()) {
                serverStartedTasks.add(callable);
                return null;
            }
        }
        return callable.call();
    }

    /*
     * If the server has not completely started, then wait until it has been.
     * The server will be "completely" stated when the server start signal has been received and any task waiting on that signal before running have now been run.
     */
    @FFDCIgnore({ InterruptedException.class })
    public static void waitServerCompletelyStarted() {
        synchronized (syncStarted) {
            if (serverCompletelyStarted.get() == false) {
                try {
                    syncStarted.wait();
                } catch (InterruptedException x) {
                    // assume we can go one then
                }
            }
        }
        return;
    }

    /**
     * non-blocking method to return the state of server startup with respect to the server being completely started.
     * The server will be "completely" stated when the server start signal has been received and any task waiting on that signal before running have now been run.
     *
     * @return
     */
    @FFDCIgnore({ InterruptedException.class })
    public static boolean isServerCompletelyStarted() {
        return serverCompletelyStarted.get();
    }

    /**
     * Declarative Services method for unsetting the ServerStarted service
     *
     * @param ref reference to the service
     */
    protected synchronized void unsetServerStarted(ServiceReference<ServerStarted> ref) {
        // server is shutting down
    }

    /**
     * Make sure we have an active/ready/configured ByteBuffer service
     */
    @Reference(service = ByteBufferConfiguration.class,
               cardinality = ReferenceCardinality.MANDATORY)
    protected void setByteBufferConfig(ByteBufferConfiguration bbConfig) {
        wsbbmgr = bbConfig;
    }

    /**
     * DS method for removing the ByteBufferConfiguration.
     * This is a required reference, will be called after deactivate.
     *
     * @param service
     */
    protected void unsetByteBufferConfig(ByteBufferConfiguration bbConfig) {
    }

    /**
     * Access the event service.
     *
     * @return EventEngine - null if not found
     */
    public static EventEngine getEventService() {
        CHFWBundle c = instance.get();
        if (null != c) {
            return c.eventService;
        }
        return null;
    }

    /**
     * DS method for setting the event reference.
     *
     * @param service
     */
    @Reference(service = EventEngine.class,
               cardinality = ReferenceCardinality.MANDATORY)
    protected void setEventService(EventEngine service) {
        this.eventService = service;
    }

    /**
     * DS method for removing the event reference.
     * This is a required reference, will be called after deactivate.
     *
     * @param service
     */
    protected void unsetEventService(EventEngine service) {
    }

    /**
     * Access the channel framework's {@link java.util.concurrent.ExecutorService} to
     * use for work dispatch.
     *
     * @return the executor service instance to use within the channel framework
     */
    public static ExecutorService getExecutorService() {
        CHFWBundle c = instance.get();
        if (null != c) {
            return c.executorService;
        }
        return null;
    }

    /**
     * DS method for setting the executor service reference.
     *
     * @param executorService the {@link java.util.concurrent.ExecutorService} to
     *                            queue work to.
     */
    @Reference(service = ExecutorService.class,
               cardinality = ReferenceCardinality.MANDATORY)
    protected void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * DS method for clearing the executor service reference.
     * This is a required reference, will be called after deactivate.
     *
     * @param executorService the service instance to clear
     */
    protected void unsetExecutorService(ExecutorService executorService) {
    }

    /**
     * Access the scheduled event service.
     *
     * @return ScheduledEventService - null if not found
     */
    public static ScheduledEventService getScheduleService() {
        CHFWBundle c = instance.get();
        if (null != c) {
            return c.scheduler;
        }
        return null;
    }

    /**
     * DS method for setting the scheduled executor service reference.
     *
     * @param ref
     */
    @Reference(service = ScheduledExecutorService.class,
               cardinality = ReferenceCardinality.MANDATORY)
    protected void setScheduledExecutorService(ScheduledExecutorService ref) {
        this.scheduledExecutor = ref;
    }

    /**
     * DS method for removing the scheduled executor service reference.
     * This is a required reference, will be called after deactivate.
     *
     * @param ref
     */
    protected void unsetScheduledExecutorService(ScheduledExecutorService ref) {
    }

    /**
     * Access the scheduled executor service.
     *
     * @return ScheduledEventService - null if not found
     */
    public static ScheduledExecutorService getScheduledExecutorService() {
        CHFWBundle c = instance.get();
        if (null != c) {
            return c.scheduledExecutor;
        }
        return null;
    }

    /**
     * DS method for setting the scheduled event service reference.
     *
     * @param ref
     */
    @Reference(service = ScheduledEventService.class,
               cardinality = ReferenceCardinality.MANDATORY)
    protected void setScheduledEventService(ScheduledEventService ref) {
        this.scheduler = ref;
    }

    /**
     * DS method for removing the scheduled event service reference.
     * This is a required reference, will be called after deactivate.
     *
     * @param ref
     */
    protected void unsetScheduledEventService(ScheduledEventService ref) {
    }

    /**
     * DS method to set a factory provider.
     *
     * @param provider
     */
    @Reference(service = ChannelFactoryProvider.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setFactoryProvider(ChannelFactoryProvider provider) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Add factory provider; " + provider);
        }

        this.chfw.registerFactories(provider);
    }

    /**
     * DS method to remove a factory provider.
     *
     * @param provider
     */
    protected void unsetFactoryProvider(ChannelFactoryProvider provider) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Remove factory provider; " + provider);
        }

        this.chfw.deregisterFactories(provider);
    }

    /**
     * Access the channel framework's {@link ApproximateTime} service.
     *
     * @return the approximate time service instance to use within the channel framework
     */
    public static long getApproxTime() {
        return QuickApproxTime.getApproxTime();
    }

    /**
     * Set the approximate time service reference.
     * This is a required reference: will be called before activation.
     *
     * @param ref new ApproximateTime service instance/provider
     */
    @Reference(service = ApproximateTime.class,
               cardinality = ReferenceCardinality.MANDATORY)
    protected void setApproxTimeService(ApproximateTime ref) {
        // do nothing: need the ref for activation of service
    }

    @Reference(service = HttpProtocolBehavior.class, cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected synchronized void setBehavior(ServiceReference<HttpProtocolBehavior> reference) {

        protocolBehaviorRef = reference;
        httpVersionSetting = (String) reference.getProperty(HttpProtocolBehavior.HTTP_VERSION_SETTING);
        if (OPTIONAL_DEFAULT_OFF_20.equalsIgnoreCase(httpVersionSetting)) {
            default20Off = true;
            versionSet = true;
        } else if (OPTIONAL_DEFAULT_ON_20.equalsIgnoreCase(httpVersionSetting)) {
            default20On = true;
            versionSet = true;
        }
    }

    protected synchronized void unsetBehavior(ServiceReference<HttpProtocolBehavior> reference) {
        if (reference == this.protocolBehaviorRef) {
            protocolBehaviorRef = null;
            httpVersionSetting = null;
            versionSet = false;
            default20Off = false;
            default20On = false;
        }
    }

    public static String getServletConfiguredHttpVersionSetting() {
        return httpVersionSetting;
    }

    public static boolean isHttp2DisabledByDefault() {
        return versionSet && default20Off;
    }

    public static boolean isHttp2EnabledByDefault() {
        return versionSet && default20On;
    }

    /**
     * Remove the reference to the approximate time service.
     * This is a required reference, will be called after deactivate.
     *
     * @param ref ApproximateTime service instance/provider to remove
     */
    protected void unsetApproxTimeService(ApproximateTime ref) {
        // do nothing: need the ref for activation of service
    }

    /**
     * Query the reference to the channel framework.
     *
     * @return ChannelFramework
     */
    public ChannelFramework getFramework() {
        return this.chfw;
    }

    /**
     * Access the reference to the bytebuffer pool manager created by this
     * bundle, or the default/fallback pool.
     *
     * This method should never return null
     *
     * @return WsByteBufferPoolManager
     */
    public WsByteBufferPoolManager getBufferManager() {
        ByteBufferConfiguration bbMgr = this.wsbbmgr;

        // Get the byte buffer manager-- bbMgr could return null
        WsByteBufferPoolManager result = (bbMgr == null) ? null : bbMgr.getBufferManager();

        // Fall back to a default if bbMgr was null or null was returned
        return result != null ? result : ChannelFrameworkFactory.getBufferManager();
    }

    /**
     * helper method for getting access to the EndPointMgr
     *
     * @return EndPointMgr
     */
    public EndPointMgr getEndpointManager() {
        return EndPointMgrImpl.getRef();
    }
}
