/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.collector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.collector.internal.Task;
import com.ibm.ws.collector.internal.TaskConfig;
import com.ibm.ws.collector.internal.TaskImpl;
import com.ibm.ws.collector.internal.TraceConstants;
import com.ibm.ws.logging.collector.CollectorConstants;
import com.ibm.ws.logging.collector.Formatter;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.CollectorManager;
import com.ibm.wsspi.collector.manager.Handler;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * An abstract class that defines the common functionality of a collector service
 */
public abstract class Collector implements Handler, Formatter {

    private static final TraceComponent tc = Tr.register(Collector.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    /** Events Buffer reference */
    private volatile EventsBuffer eventsBuffer;
    //No of events the buffer will hold before it gets flushed
    private final int maxSize = 10000;
    //Buffer flush time interval
    private final long period = 5000;

    /** Collector manager reference */
    private volatile CollectorManager collectorMgr;

    /** Map holding instances of tasks that this class manages */
    private final ConcurrentHashMap<String, Task> taskMap = new ConcurrentHashMap<String, Task>();

    /** Latch to handle a corner case where modified() might get called before init() */
    private final CountDownLatch latch = new CountDownLatch(1);

    /** Config keys */
    protected static final String SOURCE_LIST_KEY = "source";
    private static final String TAG_LIST_KEY = "tag";
    private static final String MAX_FIELD_KEY = "maxFieldLength";
    private static final String MAX_EVENTS_KEY = "maxEvents";

    protected static final String EXECUTOR_SERVICE = "executorService";
    protected final AtomicServiceReference<ExecutorService> executorServiceRef = new AtomicServiceReference<ExecutorService>(EXECUTOR_SERVICE);

    protected abstract void setExecutorService(ServiceReference<ExecutorService> executorService);

    protected abstract void unsetExecutorService(ServiceReference<ExecutorService> executorService);

    protected void activate(ComponentContext cc, Map<String, Object> configuration) {
        executorServiceRef.activate(cc);
        //When the component gets activated
        //Process the configuration, initialize the events buffer and start it.
        try {
            //Process and update the configuration
            configure(configuration);
        } catch (IOException e) {
            //FFDC
        }
        Target target = getTarget();
        eventsBuffer = new EventsBuffer(target, maxSize, period);
        eventsBuffer.start();
    }

    protected void deactivate(ComponentContext cc, int reason) {
        eventsBuffer.stop();
        //To ensure that we don't hold any reference to the collector manager after
        //the component gets deactivated.
        collectorMgr = null;
        executorServiceRef.deactivate(cc);
    }

    protected void modified(Map<String, Object> configuration) {
        //This is to handle the situation where init() has not yet been called
        //and the configuration gets updated. We wait for init() to get called before
        // we proceed with configuration update.
        if (collectorMgr == null) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        try {
            //Obtain the list of sources that we are currently subscribed to
            ArrayList<String> oldSources = new ArrayList<String>(taskMap.keySet());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "listOfOldSources " + oldSources);
            }

            //Obtain the list of sources that we will (potentially) subscribe to
            //which was passed in when the configuration changed
            List<TaskConfig> configList = new ArrayList<TaskConfig>();
            configList.addAll(parseConfig(configuration));
            ArrayList<String> newSources = new ArrayList<String>();
            for (TaskConfig taskConfig : configList) {
                newSources.add(taskConfig.sourceId());
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "listOfNewSources " + newSources);
            }

            //Find the set of sources that should be unsubscribed from as:
            //{Sources to unsubscribe} = {Old sources} - {New sources}
            ArrayList<String> sourcesToUnsubscribe = new ArrayList<String>(oldSources);
            sourcesToUnsubscribe.removeAll(newSources);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "listOfSourcesToUnsubscribe " + sourcesToUnsubscribe);
            }

            //Find the set of sources that should be subscribed to as:
            //{{New sources} - {Old sources}
            ArrayList<String> sourcesToSubscribe = new ArrayList<String>(newSources);
            sourcesToSubscribe.removeAll(oldSources);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "listOfSourcesToSubscribe " + sourcesToSubscribe);
            }

            //Process the unsubscription
            collectorMgr.unsubscribe(this, sourcesToUnsubscribe);
            //Deallocate tasks that are no longer needed and remove them from the taskMap
            deconfigure(sourcesToUnsubscribe);
            //Create new tasks and add them to the TaskMap
            configure(configuration);
            //Process the subscription
            collectorMgr.subscribe(this, sourcesToSubscribe);
        } catch (Exception e) {

        }
    }

    /*
     * Deallocate the handlers before removing the specified sources from the TaskMap
     *
     * @param sourcesToUnsubscribe The list of sources we have unsubscribed from and await final deallocation
     */
    private void deconfigure(List<String> sourcesToUnsubscribe) {
        for (String sourceName : sourcesToUnsubscribe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Task deConfig " + this, sourceName);
            }
            taskMap.get(sourceName).setHandlerName(null);
            //taskMap.get(sourceName).setConfig(null);
            taskMap.remove(sourceName);
        }
    }

    /*
     * Process the configuration and create the relevant tasks
     * Target is also initialized using the information provided in the configuration
     */
    private void configure(Map<String, Object> configuration) throws IOException {
        List<TaskConfig> configList = new ArrayList<TaskConfig>();
        configList.addAll(parseConfig(configuration));
        for (TaskConfig taskConfig : configList) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Task config " + this, taskConfig);
            }
            if (taskConfig.getEnabled()) {
                //Create the task, set the configuration and add it to the map if it's not already present.
                Task task = new TaskImpl();
                if (task != null) {
                    //Check if task already exist (by check if the source id is already in the taskMap)
                    //if not, we set the task with the new config and get appropriate handler based on the type of source
                    //else, we simply replace the config in the original task with the modified config
                    if (!taskMap.containsKey(taskConfig.sourceId())) {
                        task.setHandlerName(getHandlerName());
                        task.setConfig(taskConfig);
                        taskMap.putIfAbsent(taskConfig.sourceId(), task);
                    } else {
                        taskMap.get(taskConfig.sourceId()).setConfig(taskConfig);
                    }
                }
            }
        }
    }

    /*
     * Parse the server configuration and create relevant task's
     * configuration which includes the following
     * 1) Source name
     * 2) Location
     * 3) Status, whether it is enabled or not
     * 4) Tags associated with the config
     * 5) maxEvents associated with the config, to throttle events
     */
    private List<TaskConfig> parseConfig(Map<String, Object> config) {
        List<TaskConfig> result = new ArrayList<TaskConfig>();
        String[] tagList = null;
        if (config.containsKey(TAG_LIST_KEY)) {

            tagList = (String[]) config.get(TAG_LIST_KEY);
            ArrayList<String> validList = new ArrayList<String>();
            ArrayList<String> invalidList = new ArrayList<String>();
            validateTags(tagList, validList, invalidList);
            if (invalidList.size() > 0) {
                //If contain special characters, check failed
                Tr.warning(tc, "TAGS_FILTERING_WARNING", Arrays.toString(invalidList.toArray(new String[invalidList.size()])));
            }
            tagList = validList.toArray(new String[validList.size()]);
        }

        int maxFieldLength = (Integer) config.get(MAX_FIELD_KEY);
        int maxEvents = 0;
        //Events Throttling - maxEvents
        if (config.containsKey(MAX_EVENTS_KEY)) {
            //Check if maxEvents is in the integer range 0-2147483647
            try {

                maxEvents = Integer.parseInt(config.get(MAX_EVENTS_KEY).toString());
                if (maxEvents < 0) {
                    maxEvents = 0;
                }

            } catch (Exception e) {//Any other casting exception will result in the following out-of-range warning
                Tr.warning(tc, "MAXEVENTS_OUTOFRANGE_WARNING", config.get(MAX_EVENTS_KEY));
                maxEvents = 0;

            }

        }

        if (config.containsKey(SOURCE_LIST_KEY)) {
            String[] sourceList = (String[]) config.get(SOURCE_LIST_KEY);
            if (sourceList != null) {
                for (String source : sourceList) {
                    if (!getSourceName(source.trim()).isEmpty()) {
                        TaskConfig.Builder builder;
                        if (getSourceName(source.trim()).equals(CollectorConstants.AUDIT_LOG_SOURCE)) {
                            builder = new TaskConfig.Builder(getSourceName(source.trim()), CollectorConstants.SERVER);
                        } else {
                            builder = new TaskConfig.Builder(getSourceName(source.trim()), CollectorConstants.MEMORY);
                        }
                        builder.enabled(true);
                        builder.tags(tagList);
                        builder.maxEvents(maxEvents);
                        builder.maxFieldLength(maxFieldLength);
                        result.add(builder.build());
                    }
                }
            }
        }

        return result;
    }

    /**
     * Filter out tags with escaping characters and invalid characters, restrict to only alphabetical and numeric characters
     *
     * @param tagList
     * @return
     */
    private static void validateTags(String[] tagList, ArrayList<String> validList, ArrayList<String> invalidList) {
        for (String tag : tagList) {
            tag = tag.trim();
            if (tag.contains("\\") || tag.contains(" ") || tag.contains("\n") || tag.contains("-") || tag.equals("")) {
                invalidList.add(tag);
            } else {
                validList.add(tag);
            }
        }
    }

    /*
     * We are manually mapping the configuration source values to the actual source
     * in this method. When we move to a more generic model this will go away.
     */
    protected String getSourceName(String source) {
        if (source.equals(CollectorConstants.GC_CONFIG_VAL))
            return CollectorConstants.GC_SOURCE;
        else if (source.equals(CollectorConstants.MESSAGES_CONFIG_VAL))
            return CollectorConstants.MESSAGES_SOURCE;
        else if (source.equals(CollectorConstants.FFDC_CONFIG_VAL))
            return CollectorConstants.FFDC_SOURCE;
        else if (source.equals(CollectorConstants.TRACE_CONFIG_VAL))
            return CollectorConstants.TRACE_SOURCE;
        else if (source.equalsIgnoreCase(CollectorConstants.ACCESS_CONFIG_VAL))
            return CollectorConstants.ACCESS_LOG_SOURCE;
        else if (source.equalsIgnoreCase(CollectorConstants.AUDIT_CONFIG_VAL))
            return CollectorConstants.AUDIT_LOG_SOURCE;
        return "";
    }

    /** Methods from the collector manager handler interface */
    @Override
    public void init(CollectorManager collectorManager) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Subscribing to sources " + this, taskMap.keySet());
        }
        try {
            this.collectorMgr = collectorManager;
            //Get the source Ids from the task map and subscribe to relevant sources
            collectorMgr.subscribe(this, new ArrayList<String>(taskMap.keySet()));
        } catch (Exception e) {

        } finally {
            latch.countDown();
        }
    }

    @Override
    public void setBufferManager(String sourceId, BufferManager bufferMgr) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Setting buffer manager " + this, sourceId, bufferMgr);
        }
        Task task = taskMap.get(sourceId);
        if (task != null) {
            task.setBufferMgr(bufferMgr);
            task.setExecutorService(executorServiceRef.getService());
            task.setEventsBuffer(eventsBuffer);
            task.setFormatter(this);
            task.start();
        }
    }

    @Override
    public void unsetBufferManager(String sourceId, BufferManager bufferMgr) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Un-setting buffer manager " + this, sourceId, bufferMgr);
        }
        Task task = taskMap.get(sourceId);
        if (task != null) {
            task.stop();
        }
    }

    public abstract Target getTarget();
}
