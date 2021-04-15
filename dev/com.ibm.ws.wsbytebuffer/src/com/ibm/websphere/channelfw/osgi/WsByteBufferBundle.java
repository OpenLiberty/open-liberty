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

import com.ibm.websphere.event.EventEngine;
import com.ibm.websphere.event.ScheduledEventService;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.bytebuffer.internal.ByteBufferConfiguration;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.feature.ServerStarted;
import com.ibm.wsspi.bytebuffer.WsByteBufferFactory;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;
import com.ibm.wsspi.timer.ApproximateTime;
import com.ibm.wsspi.timer.QuickApproxTime;

/**
 * OSGi public bundle API for WsByteBuffer. 
 */
@Component(service = { WsByteBufferBundle.class, ServerQuiesceListener.class },
           name = "com.ibm.ws.wsbytebuffer",
           configurationPid = "com.ibm.ws.wsbytebuffer",
           configurationPolicy = ConfigurationPolicy.OPTIONAL,
           immediate = true,
           property = { "service.vendor=IBM" })
public class WsByteBufferBundle implements ServerQuiesceListener {

    /** Trace service */
    private static final TraceComponent tc = Tr.register(WsByteBufferBundle.class,
                                                         com.ibm.ws.bytebuffer.internal.ChannelFrameworkConstants.BASE_TRACE_NAME,
                                                         com.ibm.ws.bytebuffer.internal.ChannelFrameworkConstants.BASE_BUNDLE);

    /**
     * Active HttpDispatcher instance. May be null between deactivate and activate
     * calls.
     */
    private static final AtomicReference<WsByteBufferBundle> instance = new AtomicReference<WsByteBufferBundle>();

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

    /**
     * Constructor.
     */
    public WsByteBufferBundle() {
        //TODO delete this.chfw = WsByteImpl.getRef();
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

        // TODO
        // handle config (such as TCP factory info) before registering
        // factories, as the register will trigger an automatic load of any
        // delayed configuration, and we need the config before that happens
        // modified(config);


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
        // EndPointMgrImpl.destroyEndpoints();

        try {
        //    this.chfw.destroy();
        } catch (Throwable t) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Error destroying framework: " + t);
            }
        }

    }

    /**
     * Modified method. This method is called when the
     * service properties associated with the service are updated through a
     * configuration change.
     *
     * @param configuration
     *                             the configuration data
     */
    @Modified
    protected synchronized void modified(Map<String, Object> configuration) {

    	// No WsByteBuffer specific config in CF.

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Processing config", configuration);
        }

        return;
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
        // Are there any external mapped buffer/free actions possible?
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
            Tr.debug(this, tc, "WsByteBuffer signaled- Server Completely Started signal received");
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
        WsByteBufferBundle c = instance.get();
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
        WsByteBufferBundle c = instance.get();
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
        WsByteBufferBundle c = instance.get();
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
        WsByteBufferBundle c = instance.get();
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
        return result != null ? result : WsByteBufferFactory.getBufferManager();
    }

}
