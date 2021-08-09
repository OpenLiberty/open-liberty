/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.event.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventConstants;

import com.ibm.websphere.event.EventEngine;
import com.ibm.websphere.event.ExecutorServiceFactory;
import com.ibm.websphere.event.Topic;

public class TopicBasedCacheTest {

    @Rule
    public final JUnitRuleMockery context = new JUnitRuleMockery();

    final ExecutorService executorService = Executors.newSingleThreadExecutor();
    final ExecutorServiceFactory mockExecutorServiceFactory = context.mock(ExecutorServiceFactory.class);

    EventEngineImpl eventEngine;
    TopicBasedCache cache;

    @Before
    public void initialize() {
        eventEngine = new EventEngineImpl();

        try {
            Field f = eventEngine.getClass().getDeclaredField("topicCache");
            f.setAccessible(true);
            cache = (TopicBasedCache) f.get(eventEngine);
        } catch (Exception e) {
        }

        eventEngine.setExecutorServiceFactory(mockExecutorServiceFactory);
    }

    @After
    public void destroy() {
        eventEngine = null;
        cache = null;
    }

    @Test
    public void testTopicBasedCache() {
        assertNotNull(cache.serviceReferenceMap);
        assertTrue(cache.serviceReferenceMap.isEmpty());

        assertNotNull(cache.discreteEventHandlers);
        assertTrue(cache.discreteEventHandlers.isEmpty());

        assertNotNull(cache.wildcardEventHandlers);
        assertTrue(cache.wildcardEventHandlers.isEmpty());

        assertNotNull(cache.discreteStageTopics);
        assertTrue(cache.discreteStageTopics.isEmpty());

        assertNotNull(cache.wildcardStageTopics);
        assertTrue(cache.wildcardStageTopics.isEmpty());

        assertNotNull(cache.topicDataCache);
        assertTrue(cache.topicDataCache.isEmpty());

        assertSame(eventEngine, cache.eventEngine);
    }

    @Test
    public void testGetExecutorWildcard() {
        assertTrue(cache.wildcardStageTopics.isEmpty());

        cache.setStageTopics("stage1", new String[] { "*" });
        context.checking(new Expectations() {
            {
                oneOf(mockExecutorServiceFactory).getExecutorService("stage1");
                will(returnValue(executorService));
                oneOf(mockExecutorServiceFactory).getExecutorService("stage1");
                will(returnValue(executorService));
                oneOf(mockExecutorServiceFactory).getExecutorService("stage1");
                will(returnValue(executorService));
            }
        });
        assertSame(executorService, cache.getExecutor("org/osgi/foo"));
        assertSame(executorService, cache.getExecutor("com/ibm/bar"));
        assertSame(executorService, cache.getExecutor("java/something"));
    }

    @Test
    public void testGetExecutorMultipleWildcards() {
        assertTrue(cache.wildcardStageTopics.isEmpty());

        cache.setStageTopics("default", new String[] { "*" });
        cache.setStageTopics("org", new String[] { "org/*" });
        cache.setStageTopics("Apache", new String[] { "org/apache/*" });
        cache.setStageTopics("IBM", new String[] { "com/ibm/*", "com/informix/*", "com/lotus/*" });

        context.checking(new Expectations() {
            {
                exactly(3).of(mockExecutorServiceFactory).getExecutorService("default");
                will(returnValue(executorService));
                exactly(3).of(mockExecutorServiceFactory).getExecutorService("org");
                will(returnValue(executorService));
                exactly(3).of(mockExecutorServiceFactory).getExecutorService("Apache");
                will(returnValue(executorService));
                exactly(4).of(mockExecutorServiceFactory).getExecutorService("IBM");
                will(returnValue(executorService));
            }
        });
        // Map to '*' --> default
        assertSame(executorService, cache.getExecutor("com/example/Topic1"));
        assertSame(executorService, cache.getExecutor("com/facebook/Topic2"));
        assertSame(executorService, cache.getExecutor("com/google/Topic3"));

        // Map 'org/*' (excluding org/apache and org/osgi) --> org
        assertSame(executorService, cache.getExecutor("org/junit/test"));
        assertSame(executorService, cache.getExecutor("org/mortbay/product"));
        assertSame(executorService, cache.getExecutor("org/sourceforge/project"));

        // Map 'org/apache/*' --> Apache
        assertSame(executorService, cache.getExecutor("org/apache/ant"));
        assertSame(executorService, cache.getExecutor("org/apache/tuscanty"));
        assertSame(executorService, cache.getExecutor("org/apache/ivy"));

        // Map 'com/ibm/*', 'com/informix/*', and 'com/lotus/*' --> IBM
        assertSame(executorService, cache.getExecutor("com/ibm/websphere"));
        assertSame(executorService, cache.getExecutor("com/ibm/dby"));
        assertSame(executorService, cache.getExecutor("com/informix/db"));
        assertSame(executorService, cache.getExecutor("com/lotus/notes"));
    }

    @Test
    public void testGetExecutorDiscrete() {
        assertTrue(cache.discreteStageTopics.isEmpty());

        cache.setStageTopics("ibm", new String[] { "com/ibm/topic1", "com/ibm/topic2" });
        cache.setStageTopics("osgi", new String[] { "org/osgi/event", "org/osgi/service" });

        context.checking(new Expectations() {
            {
                exactly(2).of(mockExecutorServiceFactory).getExecutorService("ibm");
                will(returnValue(executorService));
                exactly(1).of(mockExecutorServiceFactory).getExecutorService(TopicBasedCache.DEFAULT_STAGE_NAME);
                will(returnValue(executorService));
                exactly(2).of(mockExecutorServiceFactory).getExecutorService("osgi");
                will(returnValue(executorService));
                exactly(1).of(mockExecutorServiceFactory).getExecutorService(TopicBasedCache.DEFAULT_STAGE_NAME);
                will(returnValue(executorService));
            }
        });
        assertSame(executorService, cache.getExecutor("com/ibm/topic1"));
        assertSame(executorService, cache.getExecutor("com/ibm/topic2"));
        assertSame(executorService, cache.getExecutor("com/ibm/topic3"));
        assertSame(executorService, cache.getExecutor("org/osgi/event"));
        assertSame(executorService, cache.getExecutor("org/osgi/service"));
        assertSame(executorService, cache.getExecutor("org/osgi/framework/event"));
    }

    @Test
    public void testAddHandler() {
        final ServiceReference handlerReference = context.mock(ServiceReference.class);
        context.checking(new Expectations() {
            {
                oneOf(handlerReference).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue(new String[] { "test/*", "com/ibm/Event" }));
                oneOf(handlerReference).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(handlerReference).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(null));
                allowing(handlerReference).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));
                allowing(handlerReference).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
            }
        });

        // Make sure no handlers are registered
        assertTrue(cache.serviceReferenceMap.isEmpty());
        assertTrue(cache.discreteEventHandlers.isEmpty());
        assertTrue(cache.wildcardEventHandlers.isEmpty());

        // Register the handler
        cache.addHandler(handlerReference, false);

        // Make sure the event handler and service reference maps look good
        assertEquals(1, cache.serviceReferenceMap.size());
        assertTrue(cache.serviceReferenceMap.containsKey(handlerReference));
        assertEquals(1, cache.discreteEventHandlers.size());
        assertTrue(cache.discreteEventHandlers.containsKey("com/ibm/Event"));
        assertEquals(1, cache.wildcardEventHandlers.size());
        assertTrue(cache.wildcardEventHandlers.containsKey("test/"));

        HandlerHolder holder = cache.serviceReferenceMap.get(handlerReference);
        assertTrue(cache.discreteEventHandlers.get("com/ibm/Event").contains(holder));
        assertTrue(cache.wildcardEventHandlers.get("test/").contains(holder));
    }

    @Test
    public void testRemoveHandler() {
        final ServiceReference handlerReference1 = context.mock(ServiceReference.class, "handler1");
        context.checking(new Expectations() {
            {
                allowing(handlerReference1).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue(new String[] { "test/*", "com/ibm/Event" }));
                allowing(handlerReference1).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(handlerReference1).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(null));
                allowing(handlerReference1).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));
                allowing(handlerReference1).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
            }
        });

        final ServiceReference handlerReference2 = context.mock(ServiceReference.class, "handler2");
        context.checking(new Expectations() {
            {
                allowing(handlerReference2).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue(new String[] { "test/*", "com/ibm/Event" }));
                allowing(handlerReference2).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(handlerReference2).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(null));
                allowing(handlerReference2).getProperty(Constants.SERVICE_ID);
                will(returnValue(2L));
                allowing(handlerReference2).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
            }
        });

        // Register handler1
        cache.addHandler(handlerReference1, false);
        assertEquals(1, cache.serviceReferenceMap.size());
        assertTrue(cache.serviceReferenceMap.containsKey(handlerReference1));

        // Register handler2
        cache.addHandler(handlerReference2, false);
        assertEquals(2, cache.serviceReferenceMap.size());
        assertTrue(cache.serviceReferenceMap.containsKey(handlerReference2));

        // Remove handler1 and make sure handler2 is still setup
        cache.removeHandler(handlerReference1);
        assertFalse(cache.serviceReferenceMap.containsKey(handlerReference1));
        assertTrue(cache.serviceReferenceMap.containsKey(handlerReference2));
        assertFalse(cache.serviceReferenceMap.isEmpty());
        assertFalse(cache.discreteEventHandlers.isEmpty());
        assertFalse(cache.wildcardEventHandlers.isEmpty());

        // Handler2 validation
        assertEquals(1, cache.serviceReferenceMap.size());
        assertTrue(cache.serviceReferenceMap.containsKey(handlerReference2));
        assertEquals(1, cache.discreteEventHandlers.size());
        assertTrue(cache.discreteEventHandlers.containsKey("com/ibm/Event"));
        assertEquals(1, cache.wildcardEventHandlers.size());
        assertTrue(cache.wildcardEventHandlers.containsKey("test/"));

        // Remove handler2 and make sure things are empty
        cache.removeHandler(handlerReference2);
        assertTrue(cache.serviceReferenceMap.isEmpty());
        assertTrue(cache.discreteEventHandlers.isEmpty());
        assertTrue(cache.wildcardEventHandlers.isEmpty());
    }

    @Test
    public void testFindHandlers() {
        final ServiceReference handlerReference1 = context.mock(ServiceReference.class, "handler1");
        context.checking(new Expectations() {
            {
                allowing(handlerReference1).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue(new String[] { "test/*", "com/ibm/Event1" }));
                allowing(handlerReference1).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(handlerReference1).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));
                allowing(handlerReference1).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(2));
                allowing(handlerReference1).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
            }
        });

        final ServiceReference handlerReference2 = context.mock(ServiceReference.class, "handler2");
        context.checking(new Expectations() {
            {
                allowing(handlerReference2).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue(new String[] { "test/*", "com/ibm/Event2" }));
                allowing(handlerReference2).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(handlerReference2).getProperty(Constants.SERVICE_ID);
                will(returnValue(2L));
                allowing(handlerReference2).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(1));
                allowing(handlerReference2).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
            }
        });

        cache.addHandler(handlerReference1, false);
        cache.addHandler(handlerReference2, false);

        // No registered handlers
        assertNotNull(cache.findHandlers("some/weird/event/topic"));
        assertTrue(cache.findHandlers("some/weird/event/topic").isEmpty());

        // Multiple registered handlers
        List<HandlerHolder> testHandlers = cache.findHandlers("test/example/Topic1");
        assertNotNull(testHandlers);
        assertEquals(2, testHandlers.size());
        if (testHandlers.get(0).serviceReference == handlerReference1) {
            assertTrue(testHandlers.get(0).serviceReference == handlerReference1);
            assertTrue(testHandlers.get(1).serviceReference == handlerReference2);
        } else {
            assertTrue(testHandlers.get(0).serviceReference == handlerReference2);
            assertTrue(testHandlers.get(1).serviceReference == handlerReference1);
        }

        // Discrete handlers
        testHandlers = cache.findHandlers("com/ibm/Event1");
        assertNotNull(testHandlers);
        assertEquals(1, testHandlers.size());
        assertSame(handlerReference1, testHandlers.get(0).serviceReference);

        testHandlers = cache.findHandlers("com/ibm/Event2");
        assertNotNull(testHandlers);
        assertEquals(1, testHandlers.size());
        assertSame(handlerReference2, testHandlers.get(0).serviceReference);
    }

    @Test
    public void testGetTopicData() {
        final ServiceReference handlerReference1 = context.mock(ServiceReference.class, "handler1");
        context.checking(new Expectations() {
            {
                allowing(handlerReference1).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue(new String[] { "test/*", "com/ibm/Event1" }));
                allowing(handlerReference1).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(handlerReference1).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));
                allowing(handlerReference1).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(2));
                allowing(handlerReference1).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
            }
        });

        final ServiceReference handlerReference2 = context.mock(ServiceReference.class, "handler2");
        context.checking(new Expectations() {
            {
                allowing(handlerReference2).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue(new String[] { "test/*", "com/ibm/Event2" }));
                allowing(handlerReference2).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(handlerReference2).getProperty(Constants.SERVICE_ID);
                will(returnValue(2L));
                allowing(handlerReference2).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(1));
                allowing(handlerReference2).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
            }
        });

        context.checking(new Expectations() {
            {
                allowing(mockExecutorServiceFactory).getExecutorService(TopicBasedCache.DEFAULT_STAGE_NAME);
                will(returnValue(executorService));
            }
        });

        final Topic testTopic1 = new Topic("test/Topic1");
        final Topic testTopic2 = new Topic("test/Topic2");

        assertTrue(cache.topicDataCache.isEmpty());

        // Start by looking for handlers when nobody has been registered
        TopicData topicData1 = cache.getTopicData(testTopic1, testTopic1.getName());
        TopicData topicData2 = cache.getTopicData(null, testTopic1.getName());
        assertNotNull(topicData1);
        assertSame(topicData1, topicData2);
        assertNotNull(testTopic1.getTopicData());
        assertSame(topicData1, testTopic1.getTopicData());
        assertTrue(topicData1.getEventHandlers().isEmpty());

        // Check that topicData is the same for a new Topic object with the same Topic Name
        final Topic testTopic3 = new Topic("test/Topic1");
        TopicData topicData3 = cache.getTopicData(testTopic3, testTopic3.getName());
        assertSame(topicData1, topicData3);

        // Add two handlers
        cache.addHandler(handlerReference1, false);
        cache.addHandler(handlerReference2, false);

        // Make sure TopicData cached by Topic is clear
        assertNull(testTopic1.getTopicData());

        topicData1 = cache.getTopicData(testTopic1, testTopic1.getName());
        topicData2 = cache.getTopicData(testTopic2, testTopic2.getName());
        assertNotNull(topicData1);
        assertNotNull(topicData2);
        assertSame(topicData1, testTopic1.getTopicData());
        assertSame(topicData2, testTopic2.getTopicData());
        assertEquals(2, topicData1.getEventHandlers().size());
        assertEquals(2, topicData2.getEventHandlers().size());

        TopicData topicData4 = cache.getTopicData(null, "com/ibm/Event2");
        assertNotNull(topicData4);
        assertEquals(1, topicData4.getEventHandlers().size());
        assertSame(handlerReference2, topicData4.getEventHandlers().get(0).serviceReference);
    }

    @Test
    public void testToString() {
        String string = cache.toString();
        assertNotNull(string);
        assertTrue(string.contains(cache.getClass().getName()));
        assertTrue(string.contains("serviceReferenceMap="));
    }
}
