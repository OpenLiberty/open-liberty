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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.event.Topic;

/**
 * This class encapsulates the logic necessary to locate event handlers that are
 * interested in particular topics. The code supports dynamically adding and
 * removing handlers and for caching the results of searches.
 */
final class TopicBasedCache {

    private static final long serialVersionUID = -1668099068630434622L;

    static final String DEFAULT_STAGE_NAME = "Events";

    /**
     * Map of ServiceReference to holders. This map is used to locate wrappers
     * when <code>EventHandler</code>s are bound and unbound.
     */
    final Map<ServiceReference<?>, HandlerHolder> serviceReferenceMap = new HashMap<ServiceReference<?>, HandlerHolder>();

    /**
     * Map of fully specified topic names to their associated handlers.
     */
    final Map<String, List<HandlerHolder>> discreteEventHandlers = new HashMap<String, List<HandlerHolder>>();

    /**
     * Map of topic prefixes to holders. This map is sorted by topic as we'll be
     * searching through the keys regularly.
     */
    final Map<String, List<HandlerHolder>> wildcardEventHandlers = new HashMap<String, List<HandlerHolder>>();

    /**
     * Map of fully specified topic names to work stages.
     */
    final Map<String, String> discreteStageTopics = new HashMap<String, String>();

    /**
     * Map of wildcarded topic names to work stages.
     */
    final Map<String, String> wildcardStageTopics = new HashMap<String, String>();

    /**
     * Simple cache to prevent us from recalculating the target event handlers
     * for popular topics on the mainline path.
     */
    final Map<String, TopicData> topicDataCache = new ConcurrentHashMap<String, TopicData>();

    /**
     * Owning <code>EventEngine</code> implementation.
     */
    EventEngineImpl eventEngine;

    /**
     * Create an instance of <code>TopicBasedCache</code> that references its
     * owning <code>EventAdminImpl</code>.
     *
     * @param eventAdmin
     *            owning <code>EventAdminImpl<code>
     */
    TopicBasedCache(EventEngineImpl eventEngine) {
        this.eventEngine = eventEngine;
    }

    /**
     * Set the list of topics to be associated with the specified work stage.
     *
     * @param stageName
     *            the work stage name
     * @param topics
     *            the topics associated with the work stage
     */
    synchronized void setStageTopics(String stageName, String[] topics) {
        for (String t : topics) {
            if (t.equals("*")) {
                wildcardStageTopics.put("", stageName);
            } else if (t.endsWith("/*")) {
                wildcardStageTopics.put(t.substring(0, t.length() - 1), stageName);
            } else {
                discreteStageTopics.put(t, stageName);
            }
        }

        // Clear the cache since it's no longer up to date
        clearTopicDataCache();
    }

    synchronized ExecutorService getExecutor(String topic) {
        // Look for fully specified topic
        String stageName = discreteStageTopics.get(topic);

        // Then look for most fully qualified wildcard topic prefix
        if (stageName == null) {
            String candidateTopic = null;
            for (Map.Entry<String, String> entry : wildcardStageTopics.entrySet()) {
                String key = entry.getKey();
                if (!topic.startsWith(key)) {
                    continue;
                } else if (candidateTopic == null) {
                    candidateTopic = key;
                    stageName = entry.getValue();
                } else if (key.length() > candidateTopic.length()) {
                    candidateTopic = key;
                    stageName = entry.getValue();
                }
            }
        }

        if (stageName == null) {
            stageName = DEFAULT_STAGE_NAME;
        }

        return eventEngine.getExecutorService(stageName);
    }

    /**
     * Add an <code>EventHandler</code> <code>ServiceReference</code> to our
     * collection. Adding a handler reference will populate the maps that
     * associate topics with handlers.
     *
     * @param serviceReference
     *            the <code>EventHandler</code> reference
     * @param osgiHandler
     *            the serviceReference refers to an OSGi Event Handler
     */
    synchronized void addHandler(ServiceReference<?> serviceReference, boolean osgiHandler) {
        HandlerHolder holder = new HandlerHolder(eventEngine, serviceReference, osgiHandler);

        serviceReferenceMap.put(serviceReference, holder);

        for (String topic : holder.getDiscreteTopics()) {
            addTopicHandlerToMap(topic, holder, discreteEventHandlers);
        }
        for (String topic : holder.getWildcardTopics()) {
            addTopicHandlerToMap(topic, holder, wildcardEventHandlers);
        }

        // Clear the cache since it's no longer up to date
        clearTopicDataCache();
    }

    /**
     * Remove an <code>EventHandler</code> <code>ServiceReference</code> from
     * our collection. Removing a handler reference will cause us to clear all
     * references to it in our topic maps.
     *
     * @param serviceReference
     *            the <code>EventHandler</code> reference
     */
    synchronized void removeHandler(ServiceReference<?> serviceReference) {
        HandlerHolder holder = serviceReferenceMap.remove(serviceReference);

        if (holder != null) {
            for (String topic : holder.getDiscreteTopics()) {
                removeTopicHandlerFromMap(topic, holder, discreteEventHandlers);
            }
            for (String topic : holder.getWildcardTopics()) {
                removeTopicHandlerFromMap(topic, holder, wildcardEventHandlers);
            }
        }

        // Clear the cache since it's no longer up to date
        clearTopicDataCache();
    }

    /**
     * @param handlerReference
     * @param b
     */
    synchronized void updateHandler(ServiceReference<?> serviceReference, boolean osgiHandler) {
        removeHandler(serviceReference);
        addHandler(serviceReference, osgiHandler);
    }

    /**
     * Find all registered <code>EventHandler</code> references that have
     * expressed interest in the specified topic.
     *
     * @param topic
     *            the topic associated with the event to publish
     * @return the (possibly empty) list of handlers interested in the topic
     */
    synchronized List<HandlerHolder> findHandlers(String topic) {
        List<HandlerHolder> handlers = new ArrayList<HandlerHolder>();

        List<HandlerHolder> discreteHandlers = discreteEventHandlers.get(topic);
        if (discreteHandlers != null) {
            handlers.addAll(discreteHandlers);
        }

        for (Map.Entry<String, List<HandlerHolder>> entry : wildcardEventHandlers.entrySet()) {
            if (topic.startsWith(entry.getKey())) {
                handlers.addAll(entry.getValue());
            }
        }

        return handlers;
    }

    /**
     * Get the cached information about the specified topic. The cached data
     * will allow us to avoid the expense of finding various and sundry data
     * associated with a specific topic and topic hierarchies.
     *
     * @param topic
     *            the topic associated with an event
     * @param topicName
     *            the topic name associated with an event
     * @return the cached information
     */
    TopicData getTopicData(Topic topic, String topicName) {
        TopicData topicData = null;

        if (topic != null) {
            topicData = topic.getTopicData();
        }

        if (topicData == null) {
            topicData = topicDataCache.get(topicName);
            if (topic != null && topicData != null) {
                topic.setTopicDataReference(topicData.getReference());
            }
        }

        if (topicData == null) {
            synchronized (this) {
                topicData = buildTopicData(topicName);
                if (topic != null) {
                    topic.setTopicDataReference(topicData.getReference());
                }
            }
        }

        return topicData;
    }

    private synchronized TopicData buildTopicData(String topicName) {
        // Didn't find in cache so build a list. We need to get the write
        // lock while we're building the list because dropping the read
        // lock opens a window where a stale handler list could be placed
        // in the cache
        ExecutorService executorService = getExecutor(topicName);
        List<HandlerHolder> handlers = findHandlers(topicName);

        // Put the newly acquired info into the cache
        TopicData topicData = new TopicData(topicName, executorService, handlers);
        topicDataCache.put(topicName, topicData);

        return topicData;
    }

    /**
     * Clear the cache when it's no longer up to date. This is a relatively
     * infrequent operation that occurs when handlers come and go. By clearing
     * the cache, we can avoid visiting the entire cached config and simply
     * rebuild it as events are processed.
     */
    private synchronized void clearTopicDataCache() {
        for (TopicData topicData : topicDataCache.values()) {
            topicData.clearReference();
        }
        topicDataCache.clear();
    }

    private synchronized void addTopicHandlerToMap(String topic, HandlerHolder holder, Map<String, List<HandlerHolder>> map) {
        List<HandlerHolder> handlers = map.get(topic);
        if (handlers == null) {
            handlers = new ArrayList<HandlerHolder>();
            map.put(topic, handlers);
        }
        if (!handlers.contains(holder)) {
            handlers.add(holder);
        }
        // Sort the list of handlers, based on priority.
        Collections.sort(handlers);
    }

    private synchronized void removeTopicHandlerFromMap(String topic, HandlerHolder holder, Map<String, List<HandlerHolder>> map) {
        List<HandlerHolder> handlers = map.get(topic);
        if (handlers != null) {
            handlers.remove(holder);
            if (handlers.isEmpty()) {
                map.remove(topic);
            }
        }
    }

    /**
     * Simple diagnostic aid to provide a human readable representation of the
     * object.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(";serviceReferenceMap=").append(serviceReferenceMap);
        return sb.toString();
    }

}
