/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.event.internal;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.event.Event;
import com.ibm.websphere.event.EventEngine;
import com.ibm.websphere.event.ScheduledEventService;
import com.ibm.websphere.event.Topic;

/**
 * Implementation of the scheduled event service. This puts runnables on the
 * Java executor service, which fire OSGi events from the run() method.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM" })
public class ScheduledEventServiceImpl implements ScheduledEventService {

    private ScheduledExecutorService execSvc = null;
    private EventEngine eventSvc = null;

    /**
     * Constructor.
     */
    public ScheduledEventServiceImpl() {
        // do nothing
    }

    /*
     * @see
     * com.ibm.websphere.event.ScheduledEventService#schedule(com.ibm.websphere
     * .event.Topic, long, java.util.concurrent.TimeUnit)
     */
    @Override
    public ScheduledFuture<?> schedule(Topic topic, long delay, TimeUnit unit) {
        return schedule(topic, null, delay, unit);
    }

    /*
     * @see
     * com.ibm.websphere.event.ScheduledEventService#schedule(com.ibm.websphere
     * .event.Topic, java.util.Map, long, java.util.concurrent.TimeUnit)
     */
    @Override
    public ScheduledFuture<?> schedule(Topic topic, Map<?, ?> context, long delay, TimeUnit unit) {
        final EventEngine engine = this.eventSvc;
        final ScheduledExecutorService scheduler = this.execSvc;
        if (null == scheduler || null == engine) {
            throw new IllegalStateException("Service is not running");
        }
        if (null == topic) {
            throw new IllegalArgumentException("Missing topic");
        }
        Task task = new Task(topic, context, engine, CurrentEvent.get());
        return scheduler.schedule(task, delay, unit);
    }

    /*
     * @see
     * com.ibm.websphere.event.ScheduledEventService#schedule(com.ibm.websphere
     * .event.Topic, long, long, java.util.concurrent.TimeUnit)
     */
    @Override
    public ScheduledFuture<?> schedule(Topic topic, long initialDelay, long period, TimeUnit unit) {
        return schedule(topic, null, initialDelay, period, unit);
    }

    /*
     * @see
     * com.ibm.websphere.event.ScheduledEventService#schedule(com.ibm.websphere
     * .event.Topic, java.util.Map, long, long, java.util.concurrent.TimeUnit)
     */
    @Override
    public ScheduledFuture<?> schedule(Topic topic, Map<?, ?> context, long initialDelay, long period, TimeUnit unit) {
        final EventEngine engine = this.eventSvc;
        final ScheduledExecutorService scheduler = this.execSvc;
        if (null == scheduler || null == engine) {
            throw new IllegalStateException("Service is not running");
        }
        if (null == topic) {
            throw new IllegalArgumentException("Missing topic");
        }
        Task task = new Task(topic, context, engine, CurrentEvent.get());
        return scheduler.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    /**
     * Set the executor service reference.
     * 
     * @param ref
     */
    @Reference
    protected void setScheduledExecutor(ScheduledExecutorService ref) {
        this.execSvc = ref;
    }

    /**
     * Remove the reference to the executor service.
     * 
     * @param ref
     */
    protected void unsetScheduledExecutor(ScheduledExecutorService ref) {
        if (ref == this.execSvc) {
            this.execSvc = null;
        }
    }

    /**
     * Set the reference to the event engine service.
     * 
     * @param ref
     */
    @Reference
    protected void setEventEngine(EventEngine ref) {
        this.eventSvc = ref;
    }

    /**
     * Remove the event engine service reference.
     * 
     * @param ref
     */
    protected void unsetEventEngine(EventEngine ref) {
        if (ref == this.eventSvc) {
            this.eventSvc = null;
        }
    }

    /**
     * A simple runnable task to give to the executor service that will fire
     * an event on each run() call.
     */
    private static class Task implements Runnable {
        private Topic myTopic = null;
        private Map<?, ?> myContext = null;
        private EventEngine myEngine = null;
        private final Event myParent;

        /**
         * Constructor.
         * 
         * @param topic
         * @param context
         * @param engine
         */
        protected Task(Topic topic, Map<?, ?> context, EventEngine engine, Event parent) {
            this.myTopic = topic;
            this.myContext = context;
            this.myEngine = engine;
            this.myParent = parent;
        }

        /*
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            try {
                this.myEngine.postEvent(this.myTopic, this.myContext, this.myParent);
            } catch (Throwable t) {
                // Note: ffdc instrumented
            }
        }
    }
}
