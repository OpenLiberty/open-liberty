/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.event.internal;

import static org.osgi.service.event.TopicPermission.PUBLISH;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.TopicPermission;
import org.osgi.service.log.LogService;

import com.ibm.websphere.event.Event;
import com.ibm.websphere.event.EventEngine;
import com.ibm.websphere.event.EventHandle;
import com.ibm.websphere.event.ExecutorServiceFactory;
import com.ibm.websphere.event.Topic;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.event.internal.adapter.BundleEventAdapter;
import com.ibm.ws.event.internal.adapter.FrameworkEventAdapter;
import com.ibm.ws.event.internal.adapter.ServiceEventAdapter;

/**
 * Implementation of the OSGi <code>EventAdmin</code> service. The service
 * is the heart of the event-based runtime model so this implementation is
 * intended to be optimized, extensible, and spec-compliant.
 */
@Component(configurationPid = "com.ibm.ws.event",
           property = { "service.vendor=IBM" })
public class EventEngineImpl implements EventEngine, org.osgi.service.event.EventAdmin {

    private static final TraceComponent tc = Tr.register(EventEngineImpl.class);

    final static String OSGI_EVENT_HANDLER_REFERENCE_NAME = "OsgiEventHandler";
    final static String WS_EVENT_HANDLER_REFERENCE_NAME = "WsEventHandler";

    private final static String CFG_KEY_REENTRANT_HANDLER_DEFAULT = "reentrant.handler.default";
    private final static String CFG_KEY_STAGE_PREFIX = "stage.topics.";

    /**
     * Reference to helper that encapsulates the logic of adding, removing,
     * and locating registered event handlers.
     */
    private final TopicBasedCache topicCache;

    /**
     * Declarative Service component context.
     */
    private ComponentContext componentContext;

    /**
     * The <code>BundleContext</code> of our host bundle.
     */
    private BundleContext bundleContext;

    /**
     * The OSGi <code>LogService</code> to use for error reporting.
     */
    private LogService logService;

    /**
     * An adapter used to broadcast <code>FrameworkEvent</code>s as
     * Event Admin <code>Event</code>s.
     */
    private FrameworkEventAdapter frameworkEventAdapter;

    /**
     * An adapter used to broadcast <code>BundleEvent</code>s as
     * Event Admin <code>Event</code>s.
     */
    private BundleEventAdapter bundleEventAdapter;

    /**
     * An adapter used to broadcast <code>ServiceEvent</code>s as
     * Event Admin <code>Event</code>s.
     */
    private ServiceEventAdapter serviceEventAdapter;

    /**
     * The {@link ExecutorServiceFactory} used to acquire the {@link ExecutorService} instances that deliver the events.
     */
    protected ExecutorServiceFactory executorServiceFactory;

    /**
     * Reentrant handlers by default.
     */
    private boolean defaultReentrancy = false;

    public EventEngineImpl() {
        topicCache = new TopicBasedCache(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventImpl createEvent(Topic topic) {
        return new EventImpl(topic);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventImpl createEvent(String topic) {
        return new EventImpl(getTopic(topic));
    }

    // TODO: Fix in some way...
    Topic getTopic(String topicName) {
        return new Topic(topicName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventHandle sendEvent(Topic topic, Map<?, ?> properties) {
        com.ibm.websphere.event.Event event = createEvent(topic);
        if (null != properties) {
            for (Entry<?, ?> entry : properties.entrySet()) {
                event.setProperty((String) entry.getKey(), entry.getValue());
            }
        }
        return sendEvent(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventHandle sendEvent(String topic, Map<?, ?> properties) {
        Event event = createEvent(topic);
        if (null != properties) {
            for (Entry<?, ?> entry : properties.entrySet()) {
                event.setProperty((String) entry.getKey(), entry.getValue());
            }
        }
        return sendEvent(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventHandle sendEvent(Event event) {
        return publishEvent(event, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendEvent(org.osgi.service.event.Event event) {
        publishOsgiEvent(event, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventHandle postEvent(Event event) {
        return publishEvent(event, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventHandle postEvent(String topic, Map<?, ?> properties) {
        Event event = createEvent(topic);
        if (null != properties) {
            for (Entry<?, ?> entry : properties.entrySet()) {
                event.setProperty((String) entry.getKey(), entry.getValue());
            }
        }
        return postEvent(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventHandle postEvent(Topic topic, Map<?, ?> properties) {
        return postEvent(topic, properties, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventHandle postEvent(Topic topic, Map<?, ?> properties, Event parent) {
        EventImpl event = createEvent(topic);
        if (null != properties) {
            for (Entry<?, ?> entry : properties.entrySet()) {
                event.setProperty((String) entry.getKey(), entry.getValue());
            }
        }

        // In the case when this Event is being posted from the ScheduledEventService, the parent Event might
        // already be gone by the time the scheduler fires (Thus losing inheritable context). The runnable
        // passed to the scheduler keeps a reference of the original parent Event and passes it along here
        // where we attach it to this Event.
        if (parent != null) {
            event.setParent((EventImpl) parent);
        }

        return postEvent(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postEvent(org.osgi.service.event.Event event) {
        publishOsgiEvent(event, true);
    }

    /**
     * Publish the specified event to all registered handlers that have an
     * interest in the event topic.
     *
     * @param event
     *            the OSGi <code>Event</code> to deliver
     * @param async
     *            <code>true</code> if the event delivery can be done
     *            asynchronously
     */
    void publishOsgiEvent(org.osgi.service.event.Event event, boolean async) {
        String topic = event.getTopic();
        com.ibm.websphere.event.Event e = createEvent(topic);
        for (String key : event.getPropertyNames()) {
            e.setProperty(key, event.getProperty(key));
        }
        publishEvent(e, async);
    }

    /**
     * Publish the specified event to all registered handlers that have an
     * interest in the event topic.
     *
     * @param event
     *            the Liberty <code>Event</code> to deliver
     * @param async
     *            <code>true</code> if the event delivery can be done asynchronously
     * @return EventImpl
     */
    public EventImpl publishEvent(Event event, boolean async) {
        EventImpl eventImpl = (EventImpl) event;
        eventImpl.setReadOnly(true);
        EventImpl currentEvent = (EventImpl) CurrentEvent.get();
        if (null != currentEvent) {
            eventImpl.setParent(currentEvent);
        }

        // Attempt to get the Topic object and cached TopicData but fallback
        // to the topic name if the Topic object wasn't populated
        Topic topic = eventImpl.getTopicObject();
        String topicName = topic != null ? topic.getName() : eventImpl.getTopic();
        TopicData topicData = topicCache.getTopicData(topic, topicName);
        eventImpl.setTopicData(topicData);

        // Required Java 2 Security check
        checkTopicPublishPermission(topicName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Publishing Event", topicName);
        }

        // Obtain list of matching handlers: returned list of handlers
        // must be thread safe!
        List<HandlerHolder> holders = topicData.getEventHandlers();
        ExecutorService executor = topicData.getExecutorService();

        if (async) {
            int i = 0;
            // Handle asynchronous events.
            Future<?>[] futures = new Future<?>[holders.size()];
            for (HandlerHolder holder : holders) {
                holder.addEvent(eventImpl);
                futures[i++] = queueWorkRequest(holder, executor);
            }
            eventImpl.setFutures(futures);
        } else {
            // fire synchronous events
            for (HandlerHolder holder : holders) {
                holder.fireSynchronousEvent(eventImpl);
            }
        }

        return eventImpl;
    }

    /**
     * Check if the caller has permission to publish events to the specified
     * topic.
     *
     * @param topic
     *            the topic the event is being published to
     */
    void checkTopicPublishPermission(String topic) {
        SecurityManager sm = System.getSecurityManager();
        if (sm == null)
            return;

        sm.checkPermission(new TopicPermission(topic, PUBLISH));
    }

    /**
     * Activate the EventEngine implementation. This method will be called by
     * OSGi Declarative Services implementation when the component is initially
     * activated and when changes to our configuration have occurred.
     *
     * @param componentContext
     *            the OSGi DS context
     */
    @Activate
    protected void activate(ComponentContext componentContext, Map<String, Object> props) {
        this.componentContext = componentContext;

        // // Parse the configuration that we've been provided
        // Dictionary<?, ?> configProperties = componentContext.getProperties();
        // processConfigProperties(configProperties);

        // Hold a reference to the bundle context
        bundleContext = componentContext.getBundleContext();

        // Start listening to the framework events so we can publish them
        // as specified in section 113.6.3
        frameworkEventAdapter = new FrameworkEventAdapter(this);
        bundleContext.addFrameworkListener(frameworkEventAdapter);

        // Start listening to the bundle events so we can publish them
        // as specified in section 113.6.4
        bundleEventAdapter = new BundleEventAdapter(this);
        bundleContext.addBundleListener(bundleEventAdapter);

        // Start listening to the service events so we can publish them
        // as specified in section 113.6.5
        serviceEventAdapter = new ServiceEventAdapter(this);
        bundleContext.addServiceListener(serviceEventAdapter);
        processConfigProperties(props);
    }

    @Modified
    protected void modified(Map<String, Object> props) {
        processConfigProperties(props);
    }

    /**
     * Deactivate the EventAdmin service. This method will be called by the
     * OSGi Declarative Services implementation when the component is
     * deactivated. Deactivation will occur when the service configuration
     * needs to be refreshed, when the bundle is stopped, or when the DS
     * implementation is stopped.
     *
     * @param componentContext
     */
    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        // Remove the framework event adapter
        bundleContext.removeFrameworkListener(frameworkEventAdapter);
        frameworkEventAdapter = null;

        // Remove the bundle event adapter
        bundleContext.removeBundleListener(bundleEventAdapter);
        bundleEventAdapter = null;

        // Remove the service event adapter
        bundleContext.removeServiceListener(serviceEventAdapter);
        serviceEventAdapter = null;

        bundleContext = null;
        this.componentContext = null;
    }

    /**
     * Inject a ServiceReference to a service that implements the OSGi <code>EventHandler</code> interface.
     *
     * @param handlerReference
     *            the service reference
     */
    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC)
    protected void setOsgiEventHandler(ServiceReference<org.osgi.service.event.EventHandler> handlerReference) {
        topicCache.addHandler(handlerReference, true);
    }

    protected void updatedOsgiEventHandler(ServiceReference<org.osgi.service.event.EventHandler> handlerReference) {
        topicCache.updateHandler(handlerReference, true);
    }

    protected void unsetOsgiEventHandler(ServiceReference<org.osgi.service.event.EventHandler> handlerReference) {
        topicCache.removeHandler(handlerReference);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC)
    protected void setWsEventHandler(ServiceReference<com.ibm.websphere.event.EventHandler> handlerReference) {
        topicCache.addHandler(handlerReference, false);
    }

    protected void updatedWsEventHandler(ServiceReference<com.ibm.websphere.event.EventHandler> handlerReference) {
        topicCache.updateHandler(handlerReference, false);
    }

    protected void unsetWsEventHandler(ServiceReference<com.ibm.websphere.event.EventHandler> handlerReference) {
        topicCache.removeHandler(handlerReference);
    }

    private void processConfigProperties(Map<String, Object> properties) {
        Object value = properties.get(CFG_KEY_REENTRANT_HANDLER_DEFAULT);
        if (value instanceof Boolean) {
            defaultReentrancy = ((Boolean) value).booleanValue();
        } else {
            defaultReentrancy = Boolean.parseBoolean((String) value);
        }
        processWorkStageProperties(properties);
    }

    /**
     * Process the service configuration looking for properties that
     * fit the format <code>stage.<i>stage name</i>.topics</code> and
     * that point to a list of topics to associate with the stage.
     *
     * @param config
     *            the service configuration properties
     */
    protected void processWorkStageProperties(Map<String, Object> config) {
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            if (entry.getKey().startsWith(CFG_KEY_STAGE_PREFIX)) {
                String[] topics;
                Object o = entry.getValue();
                if (o instanceof String[]) {
                    topics = (String[]) o;
                } else if (o instanceof String) {
                    topics = new String[] { (String) o };
                } else {
                    topics = new String[0];
                }

                String stageName = entry.getKey().substring(CFG_KEY_STAGE_PREFIX.length());
                topicCache.setStageTopics(stageName, topics);
            }
        }
    }

    /**
     * Get the <code>BundleContext</code> for the host bundle.
     *
     * @return the <code>BundleContext</code>
     */
    BundleContext getBundleContext() {
        return bundleContext;
    }

    /**
     * Get the <code>ComponentContext</code> for this component.
     *
     * @return the <code>ComponentContext</code>
     */
    ComponentContext getComponentContext() {
        return componentContext;
    }

    /**
     * Get the default reentrancy policy for event handlers. When an
     * EventHandler is discovered without an explicit request for a
     * reentrant or non-reentrant behavior, the default will be applied.
     *
     * @return true if we ignore the spec and assume event handlers ar
     *         reentrant
     */
    boolean getDefaultReentrancy() {
        return defaultReentrancy;
    }

    /**
     * Bind the OSGi <code>LogService</code> to use for error reporting.
     *
     * @param logService
     *            the OSGi <code>LogService</code>
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected synchronized void setLogService(LogService logService) {
        this.logService = logService;
    }

    /**
     * Unbind the OSGi <code>LogService</code>.
     *
     * @param logService
     *            the target <code>LogService</code> to unbind
     */
    protected synchronized void unsetLogService(LogService logService) {
        if (logService == this.logService) {
            this.logService = null;
        }
    }

    /**
     * Log a condition to the OSGi <code>LogService</code> if one is available.
     *
     * @param serviceReference
     *            the service that caused the condition
     * @param level
     *            the log level
     * @param message
     *            a human readable message describing the condition
     * @param exception
     *            the exception that generated the condition
     */
    synchronized void log(ServiceReference serviceReference, int level, String message, Throwable exception) {
        if (logService != null) {
            logService.log(serviceReference, level, message, exception);
        }
    }

    /**
     * Get the named {@link java.util.concurrent.ExecutorService} responsible
     * for hosting the stage from the {@link WorkStageManager}.
     *
     * @param stageName
     *            the name of the stage
     *
     * @return the {@code ExecutorService} to use for event delivery or {@code null} if the executor cannot be acquired
     */
    protected synchronized ExecutorService getExecutorService(String stageName) {
        if (executorServiceFactory != null) {
            return executorServiceFactory.getExecutorService(stageName);
        }
        // TODO: Use a default executor? Drive on current thread? Throw exception?
        return null;
    }

    /**
     * Bind the {@link ExecutorServiceFactory} to use when locating the {@code ExecutorService} backing event delivery for handlers.
     *
     * @param executorServiceFactory
     *            the factory to use
     */
    @Reference
    protected synchronized void setExecutorServiceFactory(ExecutorServiceFactory executorServiceFactory) {
        this.executorServiceFactory = executorServiceFactory;
    }

    /**
     * Unbind the {@link ExecutorServiceFactory} used to locate the {@code ExecutorService} backing event delivery for handlers.
     *
     * @param executorServiceFactory
     *            the factory to use
     */
    protected synchronized void unsetExecutorServiceFactory(ExecutorServiceFactory executorServiceFactory) {
        if (this.executorServiceFactory == executorServiceFactory) {
            this.executorServiceFactory = null;
        }
    }

    private final static boolean skipSchedule = Boolean.getBoolean("com.ibm.ws.event.skipSchedule");

    /**
     * Submits a EventImpl object (which is a runnable) to the given
     * ExecutorService. The Future<?> returned by submitting the runnable
     * is returned from this method.
     *
     * @param runnable
     *            The Runnable to queue
     * @param executor
     *            The ExecutorService to run the EventImpl on
     * @return The Future<?> created by submitting the EventImpl to the
     *         ExecutorService
     */
    Future<?> queueWorkRequest(Runnable runnable, ExecutorService executor) {
        Future<?> future = null;
        if (skipSchedule) {
            runnable.run();
        } else {
            future = executor.submit(runnable);
        }
        return future;
    }
}
