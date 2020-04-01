/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.collector.manager.internal;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.collector.manager.test.source.DummyHandler;
import com.ibm.ws.collector.manager.test.source.DummySource;

import test.common.SharedOutputManager;

/**
 *
 */
public class CollectorManagerTest {

    private static SharedOutputManager outputMgr;

    /**
     * Capture stdout/stderr output to the manager.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // There are variations of this constructor:
        // e.g. to specify a log location or an enabled trace spec. Ctrl-Space for suggestions
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.trace("com.ibm.ws.collector.manager.*=all");
        outputMgr.captureStreams();

    }

    /**
     * Final teardown work when class is exiting.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
    }

    @Test
    public void testSetSource() {
        final String m = "testSetSource";
        try {
            //Check the source registration flow
            //When a source registers, a manager for the same should get created.
            CollectorManagerImpl cMgr = new CollectorManagerImpl();
            Map<String, Object> testConfig = new HashMap<String, Object>();
            cMgr.activate(testConfig);

            DummySource source = new DummySource();
            cMgr.setSource(source);
            @SuppressWarnings("unchecked")
            Map<String, SourceManager> sourceMgrs = (Map<String, SourceManager>) getField(cMgr, "sourceMgrs");
            String sourceId = CollectorManagerUtils.getSourceId(source);

            assertTrue("Source manager creation for source " + source + " failed",
                       (sourceMgrs.get(sourceId)) != null);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testSetHandler() {
        final String m = "testSetHandler";
        try {
            //Check the handler registration flow
            //When a handler registers, a manager for the same should get created.
            //A handler is also passed a reference to the collector manager during registration.
            CollectorManagerImpl cMgr = new CollectorManagerImpl();
            Map<String, Object> testConfig = new HashMap<String, Object>();
            cMgr.activate(testConfig);

            DummyHandler handler = new DummyHandler();
            cMgr.setHandler(handler);
            @SuppressWarnings("unchecked")
            Map<String, HandlerManager> handlerMgrs = (Map<String, HandlerManager>) getField(cMgr, "handlerMgrs");
            String handlerId = CollectorManagerUtils.getHandlerId(handler);

            assertTrue("Handler manager creation for handler " + handler + " failed",
                       (handlerMgrs.get(handlerId)) != null);

            Object handlerCollectorMgrRef = getField(handler, "collectorManager");

            assertTrue("Collector manager reference was not passed during registration. Reference: " + handlerCollectorMgrRef,
                       cMgr == handlerCollectorMgrRef);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testUnsetSource() {
        final String m = "testUnsetSource";
        try {
            //Check source de-registration flow
            //On removal of a source, the corresponding manager should be removed.
            CollectorManagerImpl cMgr = new CollectorManagerImpl();
            Map<String, Object> testConfig = new HashMap<String, Object>();
            cMgr.activate(testConfig);

            DummySource source = new DummySource();
            cMgr.setSource(source);

            cMgr.unsetSource(source);
            @SuppressWarnings("unchecked")
            Map<String, SourceManager> sourceMgrs = (Map<String, SourceManager>) getField(cMgr, "sourceMgrs");
            String sourceId = CollectorManagerUtils.getSourceId(source);

            assertTrue("Source manager removal for source " + source + " failed",
                       (sourceMgrs.get(sourceId)) == null);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testUnsetHandler() {
        final String m = "testUnsetHandler";
        try {
            //Check handler de-registration flow
            //On removal of a handler, the corresponding manager should be removed.
            CollectorManagerImpl cMgr = new CollectorManagerImpl();
            Map<String, Object> testConfig = new HashMap<String, Object>();
            cMgr.activate(testConfig);

            DummyHandler handler = new DummyHandler();
            cMgr.setHandler(handler);

            cMgr.unsetHandler(handler);
            @SuppressWarnings("unchecked")
            Map<String, HandlerManager> handlerMgrs = (Map<String, HandlerManager>) getField(cMgr, "handlerMgrs");
            String handlerId = CollectorManagerUtils.getHandlerId(handler);

            assertTrue("Handler manager removal for handler " + handler + " failed",
                       (handlerMgrs.get(handlerId)) == null);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test(expected = Exception.class)
    public void testSubscribeWithoutRegisration() throws Exception {
        //Call a subscribe on a handler without registering it.
        CollectorManagerImpl cMgr = new CollectorManagerImpl();
        Map<String, Object> testConfig = new HashMap<String, Object>();
        cMgr.activate(testConfig);

        DummyHandler handler = new DummyHandler();
        List<String> sourceIds = new ArrayList<String>() {
            {
                add("dummysource|memory");
            }
        };
        cMgr.subscribe(handler, sourceIds);

    }

    @Test(expected = Exception.class)
    public void testUnSubscribeWithoutRegisration() throws Exception {
        //Call a subscribe on a handler without registering it.
        CollectorManagerImpl cMgr = new CollectorManagerImpl();
        Map<String, Object> testConfig = new HashMap<String, Object>();
        cMgr.activate(testConfig);

        DummyHandler handler = new DummyHandler();
        List<String> sourceIds = new ArrayList<String>() {
            {
                add("dummysource|memory");
            }
        };
        cMgr.unsubscribe(handler, sourceIds);
    }

    @Test
    public void testHandlerSubscriptionWithoutSource() {
        final String m = "testHandlerSubscriptionWithoutSource";
        try {
            //Check the following scenario
            //Source is not registered, then handler shows up and subscribes to the source
            //Following which source shows up
            CollectorManagerImpl cMgr = new CollectorManagerImpl();
            Map<String, Object> testConfig = new HashMap<String, Object>();
            cMgr.activate(testConfig);

            DummyHandler handler = new DummyHandler();
            cMgr.setHandler(handler);
            List<String> sourceIds = new ArrayList<String>() {
                {
                    add("dummysource|memory");
                }
            };
            try {
                cMgr.subscribe(handler, sourceIds);
            } catch (NullPointerException npe) {
                //This is expected as configuration admin will not be available.
            }
            @SuppressWarnings("unchecked")
            Map<String, HandlerManager> handlerMgrs = (Map<String, HandlerManager>) getField(cMgr, "handlerMgrs");
            String handlerId = CollectorManagerUtils.getHandlerId(handler);
            HandlerManager handlerMgr = handlerMgrs.get(handlerId);

            assertTrue("Handler manager did not add the source to pending subscription list",
                       handlerMgr.getPendingSubscriptions().contains("dummysource|memory"));

            DummySource source = new DummySource();
            cMgr.setSource(source);

            @SuppressWarnings("unchecked")
            Map<String, SourceManager> sourceMgrs = (Map<String, SourceManager>) getField(cMgr, "sourceMgrs");
            String sourceId = CollectorManagerUtils.getSourceId(source);
            SourceManager sourceMgr = sourceMgrs.get(sourceId);

            assertTrue("Set buffer manager on source was not called",
                       outputMgr.checkForLiteralStandardOut("setBufferManager"));

            assertTrue("Source manager did not add the handler to subscribers list",
                       sourceMgr.getSubscriptions().contains(handlerId));

            assertTrue("Set buffer manager on handler was not called",
                       outputMgr.checkForLiteralStandardOut("setBufferManager: " + sourceId));

            assertTrue("Handler manager did not add the handler to subscribed sources list",
                       handlerMgr.getSubsribedSources().contains(sourceId));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }

    }

    @Test
    public void testHandlerSubscriptionWithSource() {
        final String m = "testHandlerSubscriptionWithSource";
        try {
            //Check the following scenario
            //Source is registered, then handler shows up and subscribes to the source
            //Scenario modified so that Handler shows up first and then source is registered
            //This is the real life scenario. Sources do not start by themselves.
            CollectorManagerImpl cMgr = new CollectorManagerImpl();
            Map<String, Object> testConfig = new HashMap<String, Object>();
            cMgr.activate(testConfig);

            DummyHandler handler = new DummyHandler();
            cMgr.setHandler(handler);

            List<String> sourceIds = new ArrayList<String>() {
                {
                    add("dummysource|memory");
                }
            };
            try {
                cMgr.subscribe(handler, sourceIds);
            } catch (NullPointerException npe) {
                //This is expected as bundleContext is not available
            }
            DummySource source = new DummySource();
            cMgr.setSource(source);
            cMgr.subscribe(handler, sourceIds);

            @SuppressWarnings("unchecked")
            Map<String, HandlerManager> handlerMgrs = (Map<String, HandlerManager>) getField(cMgr, "handlerMgrs");
            String handlerId = CollectorManagerUtils.getHandlerId(handler);
            HandlerManager handlerMgr = handlerMgrs.get(handlerId);

            @SuppressWarnings("unchecked")
            Map<String, SourceManager> sourceMgrs = (Map<String, SourceManager>) getField(cMgr, "sourceMgrs");
            String sourceId = CollectorManagerUtils.getSourceId(source);
            SourceManager sourceMgr = sourceMgrs.get(sourceId);

            assertTrue("Source manager did not add the handler to subscribers list",
                       sourceMgr.getSubscriptions().contains(handlerId));

            assertTrue("Set buffer manager on Source Manager as not called",
                       outputMgr.checkForLiteralStandardOut("setBufferManager"));

            assertTrue("Set buffer manager on handler was not called",
                       outputMgr.checkForLiteralStandardOut("setBufferManager: " + sourceId));

            assertTrue("Handler manager did not add the handler to subscribed sources list",
                       handlerMgr.getSubsribedSources().contains(sourceId));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }

    }

    @Test
    public void testHandlerUnSubscription() {
        final String m = "testHandlerSubscriptionWithSource";
        try {
            //Check the following scenario
            //Handler comes up, register with a source
            //Finally withdraws its subscription for that source.
            CollectorManagerImpl cMgr = new CollectorManagerImpl();
            Map<String, Object> testConfig = new HashMap<String, Object>();
            cMgr.activate(testConfig);

            DummyHandler handler = new DummyHandler();
            cMgr.setHandler(handler);
            List<String> sourceIds = new ArrayList<String>() {
                {
                    add("dummysource|memory");
                }
            };
            try {
                cMgr.subscribe(handler, sourceIds);
            } catch (NullPointerException npe) {
                //This is expected as bundleContext is not available
            }

            DummySource source = new DummySource();
            cMgr.setSource(source);

            cMgr.unsubscribe(handler, sourceIds);

            @SuppressWarnings("unchecked")
            Map<String, SourceManager> sourceMgrs = (Map<String, SourceManager>) getField(cMgr, "sourceMgrs");
            String sourceId = CollectorManagerUtils.getSourceId(source);
            SourceManager sourceMgr = sourceMgrs.get(sourceId);

            @SuppressWarnings("unchecked")
            Map<String, HandlerManager> handlerMgrs = (Map<String, HandlerManager>) getField(cMgr, "handlerMgrs");
            String handlerId = CollectorManagerUtils.getHandlerId(handler);
            HandlerManager handlerMgr = handlerMgrs.get(handlerId);

            assertTrue("Unset buffer manager on handler was not called",
                       outputMgr.checkForLiteralStandardOut("unsetBufferManager: " + sourceId));

            assertTrue("Source manager did remove the handler to subscribers list",
                       !sourceMgr.getSubscriptions().contains(handlerId));

            assertTrue("Unset buffer manager on source was not called",
                       outputMgr.checkForLiteralStandardOut("unsetBufferManager"));

            assertTrue("Handler manager did not remove the handler to subscribed sources list",
                       !handlerMgr.getSubsribedSources().contains(sourceId));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    //Utility methods
    public Object getField(Object instance, String fieldName) throws Throwable {
        Field field = instance.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(instance);
    }

}
