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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.security.Permission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.TopicPermission;
import org.osgi.service.log.LogService;

import com.ibm.websphere.event.Event;
import com.ibm.websphere.event.EventEngine;
import com.ibm.websphere.event.EventHandle;
import com.ibm.websphere.event.EventHandler;
import com.ibm.websphere.event.ExecutorServiceFactory;
import com.ibm.websphere.event.Topic;

import test.common.SharedOutputManager;

public class EventEngineImplTest {

    @Rule
    public final JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    static long sleepTime = 0L;

    static class TestEventHandler implements EventHandler {
        volatile List<Event> receivedEvents = new ArrayList<Event>();

        @Override
        public void handleEvent(Event event) {
            receivedEvents.add(event);
            if (sleepTime != 0) {
                sleep(sleepTime);
            }
        }
    }

    static class TestOSGiEventHandler implements org.osgi.service.event.EventHandler {
        volatile List<org.osgi.service.event.Event> receivedEvents = new ArrayList<org.osgi.service.event.Event>();

        @Override
        public void handleEvent(org.osgi.service.event.Event event) {
            receivedEvents.add(event);
            if (sleepTime != 0) {
                sleep(sleepTime);
            }
        }
    }

    final ExecutorService executorService = Executors.newCachedThreadPool();
    final ExecutorServiceFactory executorServiceFactory = context.mock(ExecutorServiceFactory.class, "executorService");
    final BundleContext bundleContext = context.mock(BundleContext.class, "bundleContext");
    final ComponentContext componentContext = context.mock(ComponentContext.class, "componentContext");
    final TestEventHandler eventHandler = new TestEventHandler();
    final ServiceReference eventHandlerReference = context.mock(ServiceReference.class, "eventHandlerReference");
    final TestOSGiEventHandler osgiEventHandler = new TestOSGiEventHandler();
    final ServiceReference osgiHandlerReference = context.mock(ServiceReference.class, "osgiHandlerReference");
    String[] osgiEventTopics = new String[] { "osgi/*", "all/*" };

    EventEngineImpl eventEngine;

    @Before
    public void initialize() {
        // Instantiate event engine and mock OSGi environment
        eventEngine = new EventEngineImpl();

        // Required ExecutorServiceFactory
        context.checking(new Expectations() {
            {
                allowing(executorServiceFactory).getExecutorService(with(aNonNull(String.class)));
                will(returnValue(executorService));
                ignoring(executorServiceFactory);
            }
        });

        // BundleContext accessed through ComponentContext
        context.checking(new Expectations() {
            {
                ignoring(bundleContext);
            }
        });

        // EventHandler ServiceReference accessed through ComponentContext
        context.checking(new Expectations() {
            {
                allowing(eventHandlerReference).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue(new String[] { "websphere/*", "all/*" }));
                allowing(eventHandlerReference).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(eventHandlerReference).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));
                allowing(eventHandlerReference).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(1));
                allowing(eventHandlerReference).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
                ignoring(eventHandlerReference);
            }
        });

        // OSGi EventHandler ServiceReference accessed through ComponentContext
        context.checking(new Expectations() {
            {
                allowing(osgiHandlerReference).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue(osgiEventTopics));
                allowing(osgiHandlerReference).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(osgiHandlerReference).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));
                allowing(osgiHandlerReference).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(1));
                allowing(osgiHandlerReference).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
                ignoring(osgiHandlerReference);
            }
        });

        // ComponentContext used on activate/deactivate
        context.checking(new Expectations() {
            {
                allowing(componentContext).getProperties();
                will(returnValue(new Hashtable<String, Object>()));
                allowing(componentContext).getBundleContext();
                will(returnValue(bundleContext));
                allowing(componentContext).locateService(with(equal(EventEngineImpl.WS_EVENT_HANDLER_REFERENCE_NAME)), with(same(eventHandlerReference)));
                will(returnValue(eventHandler));
                allowing(componentContext).locateService(with(equal(EventEngineImpl.OSGI_EVENT_HANDLER_REFERENCE_NAME)), with(same(osgiHandlerReference)));
                will(returnValue(osgiEventHandler));
                allowing(componentContext).locateService(with(aNonNull(String.class)), with(any(ServiceReference.class)));
                will(returnValue(null));
                ignoring(componentContext);
            }
        });

        // Pretend to be DS
        eventEngine.setExecutorServiceFactory(executorServiceFactory);
        eventEngine.activate(componentContext, new HashMap<String, Object>());
        eventEngine.setWsEventHandler(eventHandlerReference);
        eventEngine.setOsgiEventHandler(osgiHandlerReference);
    }

    @After
    public void destroy() {
        sleepTime = 0L;

        eventEngine.deactivate(componentContext);

        eventEngine.unsetWsEventHandler(eventHandlerReference);
        eventHandler.receivedEvents.clear();

        eventEngine.unsetOsgiEventHandler(osgiHandlerReference);
        osgiEventHandler.receivedEvents.clear();

        eventEngine.unsetExecutorServiceFactory(executorServiceFactory);
        eventEngine = null;
    }

    @Test
    public void testCreateEventTopic() {
        Topic topic = new Topic("com/ibm/liberty/test/Topic");
        EventImpl event = eventEngine.createEvent(topic);
        assertNotNull(event);
        assertEquals(topic.getName(), event.getTopic());
        assertSame(topic, event.getTopicObject());
    }

    @Test
    public void testCreateEventString() {
        final String topicName = "com/ibm/liberty/test/Topic";
        EventImpl event = eventEngine.createEvent(topicName);
        assertNotNull(event);
        assertEquals(topicName, event.getTopic());
    }

    @Test
    public void testGetTopic() {
        final String topicName = "com/ibm/liberty/test/Topic";
        Topic topic = eventEngine.getTopic(topicName);
        assertNotNull(topic);
        assertEquals(topicName, topic.getName());
    }

    @Test
    public void testSendEventTopicMapOfQQ() {
        final Topic allTopic = new Topic("all/Topic");
        Map<String, Object> allContextMap = new HashMap<String, Object>();
        allContextMap.put("property1", "value1");
        allContextMap.put("property2", "value2");

        // Verify handlers are clean
        assertTrue(eventHandler.receivedEvents.isEmpty());
        assertTrue(osgiEventHandler.receivedEvents.isEmpty());

        // Synchronous delivery
        sleepTime = 500L;

        EventImpl result = (EventImpl) eventEngine.sendEvent(allTopic, allContextMap);
        assertNotNull(result);
        assertTrue(result.isDone());
        assertEquals(allTopic.getName(), result.getTopic());

        // Verify handlers were invoked
        assertEquals(1, eventHandler.receivedEvents.size());
        assertTrue(eventHandler.receivedEvents.get(0).getPropertyNames().containsAll(allContextMap.keySet()));
        assertEquals(1, osgiEventHandler.receivedEvents.size());
        assertEquals("value1", osgiEventHandler.receivedEvents.get(0).getProperty("property1"));
        assertEquals("value2", osgiEventHandler.receivedEvents.get(0).getProperty("property2"));
    }

    @Test
    public void testSendEventEvent() {
        final Topic allTopic = new Topic("all/Topic");

        Event event = eventEngine.createEvent(allTopic);
        event.setProperty("property1", "value1");
        event.setProperty("property2", "value2");

        // Verify handlers are clean
        assertTrue(eventHandler.receivedEvents.isEmpty());
        assertTrue(osgiEventHandler.receivedEvents.isEmpty());

        // Synchronous delivery
        sleepTime = 500L;

        EventImpl result = (EventImpl) eventEngine.sendEvent(event);
        assertSame(event, result);
        assertTrue(result.isDone());
        assertEquals(allTopic.getName(), result.getTopic());

        // Verify handlers were invoked
        assertEquals(1, eventHandler.receivedEvents.size());
        assertEquals("value1", eventHandler.receivedEvents.get(0).getProperty("property1"));
        assertEquals("value2", eventHandler.receivedEvents.get(0).getProperty("property2"));
        assertEquals(1, osgiEventHandler.receivedEvents.size());
        assertEquals("value1", osgiEventHandler.receivedEvents.get(0).getProperty("property1"));
        assertEquals("value2", osgiEventHandler.receivedEvents.get(0).getProperty("property2"));
    }

    @Test
    public void testSendEventEvent1() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("property1", "value1");
        props.put("property2", "value2");

        org.osgi.service.event.Event event = new org.osgi.service.event.Event("all/Topic", props);
        eventEngine.sendEvent(event);

        // Verify handlers were invoked
        assertEquals(1, eventHandler.receivedEvents.size());
        assertEquals("value1", eventHandler.receivedEvents.get(0).getProperty("property1"));
        assertEquals("value2", eventHandler.receivedEvents.get(0).getProperty("property2"));
        assertEquals(1, osgiEventHandler.receivedEvents.size());
        assertEquals("value1", osgiEventHandler.receivedEvents.get(0).getProperty("property1"));
        assertEquals("value2", osgiEventHandler.receivedEvents.get(0).getProperty("property2"));
    }

    @Test
    public void testPostEventEvent() {

        SharedOutputManager outputMgr = SharedOutputManager.getInstance();
        outputMgr.trace("*=all=enabled");
        outputMgr.captureStreams();

        final Topic allTopic = new Topic("all/Topic");

        EventImpl event = eventEngine.createEvent(allTopic);
        event.setProperty("property1", "value1");
        event.setProperty("property2", "value2");

        // Verify handlers are clean
        assertTrue(eventHandler.receivedEvents.isEmpty());
        assertTrue(osgiEventHandler.receivedEvents.isEmpty());

        eventEngine.postEvent(event);
        assertEquals(allTopic.getName(), event.getTopic());

        // Wait for the event processing to be complete
        while (!event.isDone())
            sleep(100);

        // Verify handlers were invoked
        assertEquals(1, eventHandler.receivedEvents.size());
        assertEquals("value1", eventHandler.receivedEvents.get(0).getProperty("property1"));
        assertEquals("value2", eventHandler.receivedEvents.get(0).getProperty("property2"));
        assertEquals(1, osgiEventHandler.receivedEvents.size());
        assertEquals("value1", osgiEventHandler.receivedEvents.get(0).getProperty("property1"));
        assertEquals("value2", osgiEventHandler.receivedEvents.get(0).getProperty("property2"));

        outputMgr.restoreStreams();

    }

    @Test
    public void testPostEventEvent1() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("property1", "value1");
        props.put("property2", "value2");

        org.osgi.service.event.Event event = new org.osgi.service.event.Event("all/Topic", props);
        eventEngine.postEvent(event);

        // Wait for the event handlers to be driven
        while (eventHandler.receivedEvents.isEmpty() || osgiEventHandler.receivedEvents.isEmpty()) {
            sleep(100);
        }

        // Verify handlers were invoked
        assertEquals(1, eventHandler.receivedEvents.size());
        assertEquals("value1", eventHandler.receivedEvents.get(0).getProperty("property1"));
        assertEquals("value2", eventHandler.receivedEvents.get(0).getProperty("property2"));
        assertEquals(1, osgiEventHandler.receivedEvents.size());
        assertEquals("value1", osgiEventHandler.receivedEvents.get(0).getProperty("property1"));
        assertEquals("value2", osgiEventHandler.receivedEvents.get(0).getProperty("property2"));
    }

    @Test
    public void testPostEvent() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("property1", "value1");
        props.put("property2", "value2");

        eventEngine.postEvent("all/Topic", props);

        // Wait for the event handlers to be driven
        while (eventHandler.receivedEvents.isEmpty() || osgiEventHandler.receivedEvents.isEmpty()) {
            sleep(100);
        }

        // Verify handlers were invoked
        assertEquals(1, eventHandler.receivedEvents.size());
        assertEquals("value1", eventHandler.receivedEvents.get(0).getProperty("property1"));
        assertEquals("value2", eventHandler.receivedEvents.get(0).getProperty("property2"));
        assertEquals(1, osgiEventHandler.receivedEvents.size());
        assertEquals("value1", osgiEventHandler.receivedEvents.get(0).getProperty("property1"));
        assertEquals("value2", osgiEventHandler.receivedEvents.get(0).getProperty("property2"));
    }

    @Test
    public void testSendEvent() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("property1", "value1");
        props.put("property2", "value2");

        eventEngine.sendEvent("all/Topic", props);

        // Verify handlers were invoked
        assertEquals(1, eventHandler.receivedEvents.size());
        assertEquals("value1", eventHandler.receivedEvents.get(0).getProperty("property1"));
        assertEquals("value2", eventHandler.receivedEvents.get(0).getProperty("property2"));
        assertEquals(1, osgiEventHandler.receivedEvents.size());
        assertEquals("value1", osgiEventHandler.receivedEvents.get(0).getProperty("property1"));
        assertEquals("value2", osgiEventHandler.receivedEvents.get(0).getProperty("property2"));
    }

    @Test
    public void testModifyHandlerTopics() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("property1", "value1");
        props.put("property2", "value2");

        eventEngine.sendEvent("all/Topic", props);

        // Verify handlers were invoked
        assertEquals(1, eventHandler.receivedEvents.size());
        assertEquals("value1", eventHandler.receivedEvents.get(0).getProperty("property1"));
        assertEquals("value2", eventHandler.receivedEvents.get(0).getProperty("property2"));
        assertEquals(1, osgiEventHandler.receivedEvents.size());
        assertEquals("value1", osgiEventHandler.receivedEvents.get(0).getProperty("property1"));
        assertEquals("value2", osgiEventHandler.receivedEvents.get(0).getProperty("property2"));

        osgiEventTopics[1] = "none/*";
        eventEngine.updatedOsgiEventHandler(osgiHandlerReference);

        //osgi handler should no longer receive "all" events
        eventEngine.sendEvent("all/Topic2", props);
        assertEquals(1, osgiEventHandler.receivedEvents.size());
        assertEquals("value1", osgiEventHandler.receivedEvents.get(0).getProperty("property1"));
        assertEquals("value2", osgiEventHandler.receivedEvents.get(0).getProperty("property2"));

        //osgi handler should get "none" events
        eventEngine.sendEvent("none/Topic2", props);
        assertEquals(2, osgiEventHandler.receivedEvents.size());
        assertEquals("value1", osgiEventHandler.receivedEvents.get(1).getProperty("property1"));
        assertEquals("value2", osgiEventHandler.receivedEvents.get(1).getProperty("property2"));
    }

    @Test
    public void testMultipleHandlerSendEvent() {
        final Topic allTopic = new Topic("all/Topic");
        final Topic websphereTopic = new Topic("websphere/Topic");
        final Topic osgiTopic = new Topic("osgi/Topic");

        Map<String, Object> allContextMap = new HashMap<String, Object>();
        allContextMap.put("property1", "value1");
        allContextMap.put("property2", "value2");

        // sendEvent is synchronous so make the handlers sleep
        sleepTime = 500L;

        EventHandle allResult = eventEngine.sendEvent(allTopic, allContextMap);
        assertNotNull(allResult);
        assertTrue(allResult.isDone());

        // Verify both handlers received an event
        assertEquals(1, eventHandler.receivedEvents.size());
        assertNotNull(eventHandler.receivedEvents.get(0));
        assertEquals(allTopic.getName(), eventHandler.receivedEvents.get(0).getTopic());
        assertEquals("value1", eventHandler.receivedEvents.get(0).getProperty("property1"));
        assertEquals("value2", eventHandler.receivedEvents.get(0).getProperty("property2"));

        assertEquals(1, osgiEventHandler.receivedEvents.size());
        assertNotNull(osgiEventHandler.receivedEvents.get(0));
        assertEquals(allTopic.getName(), osgiEventHandler.receivedEvents.get(0).getTopic());
        assertEquals("value1", osgiEventHandler.receivedEvents.get(0).getProperty("property1"));
        assertEquals("value2", osgiEventHandler.receivedEvents.get(0).getProperty("property2"));

        Map<String, Object> websphereContextMap = new HashMap<String, Object>();
        websphereContextMap.put("property1", "websphere1");
        websphereContextMap.put("property2", "websphere2");

        EventHandle websphereResult = eventEngine.sendEvent(websphereTopic, websphereContextMap);
        assertNotNull(websphereResult);
        assertTrue(websphereResult.isDone());

        // Verify that only the WebSphere handler received the event
        assertEquals(2, eventHandler.receivedEvents.size());
        assertNotNull(eventHandler.receivedEvents.get(1));
        assertEquals(websphereTopic.getName(), eventHandler.receivedEvents.get(1).getTopic());
        assertEquals("websphere1", eventHandler.receivedEvents.get(1).getProperty("property1"));
        assertEquals("websphere2", eventHandler.receivedEvents.get(1).getProperty("property2"));

        assertEquals(1, osgiEventHandler.receivedEvents.size());

        Map<String, Object> osgiContextMap = new HashMap<String, Object>();
        osgiContextMap.put("property1", "osgi1");
        osgiContextMap.put("property2", "osgi2");

        EventHandle osgiResult = eventEngine.sendEvent(osgiTopic, osgiContextMap);
        assertNotNull(osgiResult);
        assertTrue(osgiResult.isDone());

        assertEquals(2, osgiEventHandler.receivedEvents.size());
        assertNotNull(osgiEventHandler.receivedEvents.get(1));
        assertEquals(osgiTopic.getName(), osgiEventHandler.receivedEvents.get(1).getTopic());
        assertEquals("osgi1", osgiEventHandler.receivedEvents.get(1).getProperty("property1"));
        assertEquals("osgi2", osgiEventHandler.receivedEvents.get(1).getProperty("property2"));

        assertEquals(2, eventHandler.receivedEvents.size());
    }

    @Test
    public void testCheckTopicPublishPermission() {
        final List<Permission> permissions = new ArrayList<Permission>();
        final SecurityManager sm = new SecurityManager() {
            @Override
            public void checkPermission(Permission permission) {
                permissions.add(permission);
            }
        };

        System.setSecurityManager(sm);
        try {
            // Fire the event
            eventEngine.postEvent("no/listener/Topic", new HashMap<String, Object>());
        } finally {
            System.setSecurityManager(null);
        }

        assertTrue(permissions.contains(new TopicPermission("no/listener/Topic", TopicPermission.PUBLISH)));
    }

    @Test
    public void testCheckTopicSubscribePermission() {
        final ServiceReference subscriber = context.mock(ServiceReference.class, "subscriber");
        context.checking(new Expectations() {
            {
                allowing(subscriber).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue(new String[] { "websphere/*", "all/*" }));
                allowing(subscriber).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(subscriber).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));
                allowing(subscriber).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(1));
                allowing(subscriber).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
                ignoring(subscriber);
            }
        });

        final List<Permission> permissions = new ArrayList<Permission>();
        final List<String> permissionInfo = new ArrayList<String>();
        final SecurityManager sm = new SecurityManager() {
            private final ThreadLocal<StringBuilder> tl = new ThreadLocal<StringBuilder>();

            @Override
            public void checkPermission(Permission permission) {
                if (tl.get() == null) {
                    synchronized (permissions) {
                        permissions.add(permission);
                        StringBuilder b = new StringBuilder("\n").append(permission.toString()).append("\n");
                        tl.set(b);
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        new Exception("stack trace").printStackTrace(pw);
                        b.append(sw.toString()).append("\n");
                        permissionInfo.add(b.toString());
                    }
                    tl.remove();
                }
            }
        };

        System.setSecurityManager(sm);
        try {
            eventEngine.setWsEventHandler(subscriber);
        } finally {
            System.setSecurityManager(null);
        }

        synchronized (permissions) {
            assertTrue("permissions: " + permissionInfo.toString(), permissions.contains(new TopicPermission("websphere/*", TopicPermission.SUBSCRIBE)));
            assertTrue("permissions: " + permissionInfo.toString(), permissions.contains(new TopicPermission("all/*", TopicPermission.SUBSCRIBE)));
        }
    }

    @Ignore("Trying configuration component")
    @Test
    public void testModified() {
        //        final ComponentContext componentContext = context.mock(ComponentContext.class, "modifiedComponentContext");
        //        context.checking(new Expectations() {{
        //            oneOf(componentContext).getProperties(); will(returnValue(new Hashtable<String, Object>()));
        //        }});
        //
        //        eventEngine.modified(componentContext);
    }

    @Test
    public void testProcessWorkStageProperties() {
        Hashtable<String, Object> config = new Hashtable<String, Object>();
        config.put("stage.topics.array", new String[] { "stage/array/Topic1", "stage/array/Topic2" });
        config.put("stage.topics.simple", "stage/simple/Topic");
        config.put("stage.topics.bad", new Object());

        TopicBasedCache topicCache = getEngineFieldValue("topicCache", TopicBasedCache.class);

        // Assert pre-conditions
        assertFalse(topicCache.discreteStageTopics.containsKey("stage/array/Topic1"));
        assertFalse(topicCache.discreteStageTopics.containsKey("stage/array/Topic2"));
        assertFalse(topicCache.discreteStageTopics.containsKey("stage/simple/Topic"));
        assertFalse(topicCache.discreteStageTopics.containsKey("stage/bad/Topic"));

        // Update config
        eventEngine.processWorkStageProperties(config);

        // Verify stage topics were pushed to the cache
        assertEquals("array", topicCache.discreteStageTopics.get("stage/array/Topic1"));
        assertEquals("array", topicCache.discreteStageTopics.get("stage/array/Topic2"));
        assertEquals("simple", topicCache.discreteStageTopics.get("stage/simple/Topic"));
        assertFalse(topicCache.discreteStageTopics.containsKey("stage/bad/Topic"));
    }

    @Test
    public void testGetBundleContext() {
        assertSame(bundleContext, getEngineFieldValue("bundleContext", BundleContext.class));
    }

    @Test
    public void testGetComponentContext() {
        assertSame(componentContext, getEngineFieldValue("componentContext", ComponentContext.class));
    }

    @Test
    public void testGetDefaultReentrancy() {
        assertFalse(eventEngine.getDefaultReentrancy());
    }

    @Test
    public void testSetUnsetLogService() {
        LogService logService = context.mock(LogService.class);

        eventEngine.setLogService(logService);
        assertSame(logService, getEngineFieldValue("logService", LogService.class));

        eventEngine.unsetLogService(logService);
        assertNull(getEngineFieldValue("logService", LogService.class));
    }

    @Test
    public void testLog() {
        final LogService logService = context.mock(LogService.class);
        final Throwable throwable = new Exception("Test exception");
        final String message = "Error message";
        context.checking(new Expectations() {
            {
                allowing(logService).log(eventHandlerReference, LogService.LOG_ERROR, message, throwable);
            }
        });

        // Test without a logService
        assertNull(getEngineFieldValue("logService", LogService.class));
        eventEngine.log(eventHandlerReference, LogService.LOG_ERROR, message, throwable);

        // Test with a logService
        eventEngine.setLogService(logService);
        assertSame(logService, getEngineFieldValue("logService", LogService.class));
        eventEngine.log(eventHandlerReference, LogService.LOG_ERROR, message, throwable);
    }

    @Test
    public void testGetExecutorService() {
        // Clear reference set during initialize
        eventEngine.unsetExecutorServiceFactory(executorServiceFactory);
        assertNull(eventEngine.executorServiceFactory);

        final ExecutorServiceFactory factory = context.mock(ExecutorServiceFactory.class, "testExecutorServiceFactory");
        context.checking(new Expectations() {
            {
                oneOf(factory).getExecutorService(TopicBasedCache.DEFAULT_STAGE_NAME);
                will(returnValue(executorService));
            }
        });
        eventEngine.setExecutorServiceFactory(factory);

        // Fire the event
        eventEngine.postEvent("all/Topic", new HashMap<String, Object>());

        // Wait for the event handlers to be driven
        while (eventHandler.receivedEvents.isEmpty() || osgiEventHandler.receivedEvents.isEmpty()) {
            sleep(100);
        }

        // Unbind the executor service
        eventEngine.unsetExecutorServiceFactory(factory);
        assertNull(eventEngine.executorServiceFactory);

        // Drive path without executor service factory
        assertNull(eventEngine.getExecutorService("someName"));

        // Reset reference set at initialize
        eventEngine.setExecutorServiceFactory(executorServiceFactory);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Throwable t) {
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getEngineFieldValue(String name, Class<T> type) {
        try {
            Field f = eventEngine.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(eventEngine);
        } catch (Exception e) {
        }

        return null;
    }
}
