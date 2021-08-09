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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventConstants;

import com.ibm.websphere.event.EventEngine;
import com.ibm.websphere.event.EventHandler;

public class HandlerHolderTest {

    @Rule
    public final JUnitRuleMockery context = new JUnitRuleMockery();

    EventEngineImpl eventEngine;

    @Before
    public void initialize() {
        eventEngine = new EventEngineImpl();
    }

    @After
    public void destroy() {
        eventEngine = null;
    }

    @Test
    public void testHandlerHolder() throws Exception {
        final ServiceReference handlerReference = context.mock(ServiceReference.class, "handlerReference");
        context.checking(new Expectations() {
            {
                allowing(handlerReference).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue(new String[] { "test/*", "com/ibm/Event1" }));
                allowing(handlerReference).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue("(bundle.symbolicName=com.ibm.*)"));
                allowing(handlerReference).getProperty(Constants.SERVICE_ID);
                will(returnValue(20L));
                allowing(handlerReference).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(200));
                allowing(handlerReference).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
            }
        });

        HandlerHolder holder = new HandlerHolder(eventEngine, handlerReference, false);
        assertSame(eventEngine, holder.eventEngine);
        assertSame(handlerReference, holder.getServiceReference());
        assertEquals("(bundle.symbolicName=com.ibm.*)", holder.filterSpec);
        assertEquals(200, holder.serviceRanking);

        assertNotNull(holder.getDiscreteTopics());
        assertNotNull(holder.getWildcardTopics());
        assertTrue(holder.getDiscreteTopics().contains("com/ibm/Event1"));
        assertTrue(holder.getWildcardTopics().contains("test/"));
    }

    @Test
    public void testGetServiceReference() {
        final ServiceReference handlerReference = context.mock(ServiceReference.class, "handlerReference");
        context.checking(new Expectations() {
            {
                allowing(handlerReference).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue(null));
                allowing(handlerReference).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(handlerReference).getProperty(Constants.SERVICE_ID);
                will(returnValue(20L));
                allowing(handlerReference).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(null));
                allowing(handlerReference).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
            }
        });

        HandlerHolder holder = new HandlerHolder(eventEngine, handlerReference, false);
        assertSame(handlerReference, holder.getServiceReference());
    }

    @Test
    public void testGetDiscreteTopics() {
        final String[] topics = new String[] { "com/ibm/Topic1", "com/ibm/Topic2", "org/apache/*", "org/osgi/*" };
        final ServiceReference handlerReference = context.mock(ServiceReference.class, "handlerReference");
        context.checking(new Expectations() {
            {
                allowing(handlerReference).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue(topics));
                allowing(handlerReference).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(handlerReference).getProperty(Constants.SERVICE_ID);
                will(returnValue(20L));
                allowing(handlerReference).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(null));
                allowing(handlerReference).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
            }
        });

        HandlerHolder holder = new HandlerHolder(eventEngine, handlerReference, false);
        assertEquals(2, holder.getDiscreteTopics().size());
        assertTrue(holder.getDiscreteTopics().contains("com/ibm/Topic1"));
        assertTrue(holder.getDiscreteTopics().contains("com/ibm/Topic2"));
    }

    @Test
    public void testGetWildcardTopics() {
        final String[] topics = new String[] { "com/ibm/Topic1", "com/ibm/Topic2", "org/apache/*", "org/osgi/*" };
        final ServiceReference handlerReference = context.mock(ServiceReference.class, "handlerReference");
        context.checking(new Expectations() {
            {
                allowing(handlerReference).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue(topics));
                allowing(handlerReference).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(handlerReference).getProperty(Constants.SERVICE_ID);
                will(returnValue(20L));
                allowing(handlerReference).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(null));
                allowing(handlerReference).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
            }
        });

        HandlerHolder holder = new HandlerHolder(eventEngine, handlerReference, false);
        assertEquals(2, holder.getWildcardTopics().size());
        assertTrue(holder.getWildcardTopics().contains("org/apache/"));
        assertTrue(holder.getWildcardTopics().contains("org/osgi/"));
    }

    @Test
    public void testIsReentrantDefault() {
        final ServiceReference handlerReference = context.mock(ServiceReference.class, "handlerReference");
        context.checking(new Expectations() {
            {
                allowing(handlerReference).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue(null));
                allowing(handlerReference).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(handlerReference).getProperty(Constants.SERVICE_ID);
                will(returnValue(20L));
                allowing(handlerReference).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(null));
                allowing(handlerReference).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
            }
        });

        HandlerHolder holder = new HandlerHolder(eventEngine, handlerReference, false);
        assertEquals(eventEngine.getDefaultReentrancy(), holder.isReentrant());
    }

    @Test
    public void testIsReentrantReentrant() {
        final ServiceReference handlerReference = context.mock(ServiceReference.class, "handlerReference");
        context.checking(new Expectations() {
            {
                allowing(handlerReference).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue(null));
                allowing(handlerReference).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(handlerReference).getProperty(Constants.SERVICE_ID);
                will(returnValue(20L));
                allowing(handlerReference).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(null));
                allowing(handlerReference).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue("true"));
            }
        });

        HandlerHolder holder = new HandlerHolder(eventEngine, handlerReference, false);
        assertTrue(holder.isReentrant());
    }

    @Test
    public void testIsReentrantNonReentrant() {
        final ServiceReference handlerReference = context.mock(ServiceReference.class, "handlerReference");
        context.checking(new Expectations() {
            {
                allowing(handlerReference).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue(null));
                allowing(handlerReference).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(handlerReference).getProperty(Constants.SERVICE_ID);
                will(returnValue(20L));
                allowing(handlerReference).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(null));
                allowing(handlerReference).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue("false"));
            }
        });

        HandlerHolder holder = new HandlerHolder(eventEngine, handlerReference, false);
        assertFalse(holder.isReentrant());
    }

    @Test
    public void testGetServiceWs() {
        final ServiceReference handlerReference = context.mock(ServiceReference.class, "handlerReference");
        context.checking(new Expectations() {
            {
                allowing(handlerReference).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue(null));
                allowing(handlerReference).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(handlerReference).getProperty(Constants.SERVICE_ID);
                will(returnValue(20L));
                allowing(handlerReference).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(null));
                allowing(handlerReference).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
            }
        });
        final EventHandler mockHandler = context.mock(EventHandler.class, "wsEventHandler");
        final BundleContext bundleContext = context.mock(BundleContext.class, "EventBundleContext");
        context.checking(new Expectations() {
            {
                ignoring(bundleContext);
            }
        });
        final ComponentContext componentContext = context.mock(ComponentContext.class, "EventComponentContext");
        context.checking(new Expectations() {
            {
                ignoring(bundleContext);
                allowing(componentContext).getProperties();
                will(returnValue(new Hashtable<String, Object>()));
                allowing(componentContext).getBundleContext();
                will(returnValue(bundleContext));
                allowing(componentContext).locateService(EventEngineImpl.OSGI_EVENT_HANDLER_REFERENCE_NAME, handlerReference);
                will(returnValue(null));
                oneOf(componentContext).locateService(EventEngineImpl.WS_EVENT_HANDLER_REFERENCE_NAME, handlerReference);
                will(returnValue(mockHandler));
            }
        });

        // Activate the event engine
        eventEngine.activate(componentContext, new HashMap<String, Object>());

        // Create a handler holder for the test service
        HandlerHolder holder = new HandlerHolder(eventEngine, handlerReference, false);
        EventHandler handler = holder.getService();
        assertNotNull(handler);
        assertSame(mockHandler, handler);

        EventHandler handler2 = holder.getService();
        assertSame(handler, handler2);
    }

    @Test
    public void testGetServiceOsgi() {
        final ServiceReference handlerReference = context.mock(ServiceReference.class, "handlerReference");
        context.checking(new Expectations() {
            {
                allowing(handlerReference).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue(null));
                allowing(handlerReference).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(handlerReference).getProperty(Constants.SERVICE_ID);
                will(returnValue(20L));
                allowing(handlerReference).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(null));
                allowing(handlerReference).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
            }
        });
        final org.osgi.service.event.EventHandler mockHandler = context.mock(org.osgi.service.event.EventHandler.class, "osgiEventHandler");
        final BundleContext bundleContext = context.mock(BundleContext.class, "EventBundleContext");
        context.checking(new Expectations() {
            {
                ignoring(bundleContext);
            }
        });
        final ComponentContext componentContext = context.mock(ComponentContext.class, "EventComponentContext");
        context.checking(new Expectations() {
            {
                ignoring(bundleContext);
                allowing(componentContext).getProperties();
                will(returnValue(new Hashtable<String, Object>()));
                allowing(componentContext).getBundleContext();
                will(returnValue(bundleContext));
                allowing(componentContext).locateService(EventEngineImpl.WS_EVENT_HANDLER_REFERENCE_NAME, handlerReference);
                will(returnValue(null));
                oneOf(componentContext).locateService(EventEngineImpl.OSGI_EVENT_HANDLER_REFERENCE_NAME, handlerReference);
                will(returnValue(mockHandler));
            }
        });

        // Activate the event engine
        eventEngine.activate(componentContext, new HashMap<String, Object>());

        // Create a handler holder for the test service
        HandlerHolder holder = new HandlerHolder(eventEngine, handlerReference, true);
        EventHandler handler = holder.getService();
        assertNotNull(handler);

        EventHandler handler2 = holder.getService();
        assertSame(handler, handler2);
    }

    @Test
    public void testGetFilter() throws Exception {
        final String filterString = "(bundle.symbolicName=com.ibm.*)";
        final ServiceReference handlerReference = context.mock(ServiceReference.class, "handlerReference");
        context.checking(new Expectations() {
            {
                allowing(handlerReference).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue(null));
                allowing(handlerReference).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(filterString));
                allowing(handlerReference).getProperty(Constants.SERVICE_ID);
                will(returnValue(20L));
                allowing(handlerReference).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(null));
                allowing(handlerReference).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
            }
        });
        final Filter mockFilter = context.mock(Filter.class, "filter");
        final BundleContext bundleContext = context.mock(BundleContext.class, "EventBundleContext");
        context.checking(new Expectations() {
            {
                allowing(bundleContext).createFilter(filterString);
                will(returnValue(mockFilter));
                ignoring(bundleContext);
            }
        });
        final ComponentContext componentContext = context.mock(ComponentContext.class, "EventComponentContext");
        context.checking(new Expectations() {
            {
                allowing(componentContext).getProperties();
                will(returnValue(new Hashtable<String, Object>()));
                allowing(componentContext).getBundleContext();
                will(returnValue(bundleContext));
                ignoring(componentContext);
            }
        });

        // Activate the event engine
        eventEngine.activate(componentContext, new HashMap<String, Object>());

        // Create a handler holder for the test service
        HandlerHolder holder = new HandlerHolder(eventEngine, handlerReference, false);
        Filter filter = holder.getFilter();
        assertSame(filter, mockFilter);
    }

    @Test
    public void testToString() {
        final String[] topics = new String[] { "com/ibm/Topic1", "com/ibm/Topic2", "org/apache/*", "org/osgi/*" };
        final ServiceReference handlerReference = context.mock(ServiceReference.class, "handlerReference");
        context.checking(new Expectations() {
            {
                allowing(handlerReference).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue(topics));
                allowing(handlerReference).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(handlerReference).getProperty(Constants.SERVICE_ID);
                will(returnValue(20L));
                allowing(handlerReference).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(null));
                allowing(handlerReference).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
            }
        });

        HandlerHolder holder = new HandlerHolder(eventEngine, handlerReference, false);
        String string = holder.toString();
        assertNotNull(string);
        assertTrue(string.contains(HandlerHolder.class.getName()));
        assertTrue(string.contains("target="));
        assertTrue(string.contains("serviceReference="));
    }

    @Test
    public void testCompareTo() {
        final String[] topics = new String[] { "com/ibm/Topic1", "com/ibm/Topic2", "org/apache/*", "org/osgi/*" };
        final ServiceReference handlerReference1 = context.mock(ServiceReference.class, "handlerReference1");

        // Service id 20, Service ranking 1
        context.checking(new Expectations() {
            {
                allowing(handlerReference1).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue(topics));
                allowing(handlerReference1).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(handlerReference1).getProperty(Constants.SERVICE_ID);
                will(returnValue(20L));
                allowing(handlerReference1).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(1));
                allowing(handlerReference1).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
            }
        });

        // Service id 21, Service ranking unset (0)
        final ServiceReference handlerReference2 = context.mock(ServiceReference.class, "handlerReference2");
        context.checking(new Expectations() {
            {
                allowing(handlerReference2).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue("some/topic/name"));
                allowing(handlerReference2).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(handlerReference2).getProperty(Constants.SERVICE_ID);
                will(returnValue(21L));
                allowing(handlerReference2).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(null)); // Default
                allowing(handlerReference2).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
            }
        });

        // Service id 22, Service ranking MAX_VALUE
        final ServiceReference handlerReference3 = context.mock(ServiceReference.class, "handlerReference3");
        context.checking(new Expectations() {
            {
                allowing(handlerReference3).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue("*"));
                allowing(handlerReference3).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(handlerReference3).getProperty(Constants.SERVICE_ID);
                will(returnValue(22L));
                allowing(handlerReference3).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(Integer.MAX_VALUE));
                allowing(handlerReference3).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
            }
        });

        // Service id 23, Service ranking MIN_VALUE
        final ServiceReference handlerReference4 = context.mock(ServiceReference.class, "handlerReference4");
        context.checking(new Expectations() {
            {
                allowing(handlerReference4).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue("*"));
                allowing(handlerReference4).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(handlerReference4).getProperty(Constants.SERVICE_ID);
                will(returnValue(23L));
                allowing(handlerReference4).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(Integer.MIN_VALUE));
                allowing(handlerReference4).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
            }
        });

        // Service id 24, Service ranking default (0)
        final ServiceReference handlerReference5 = context.mock(ServiceReference.class, "handlerReference5");
        context.checking(new Expectations() {
            {
                allowing(handlerReference5).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue("*"));
                allowing(handlerReference5).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(handlerReference5).getProperty(Constants.SERVICE_ID);
                will(returnValue(24L));
                allowing(handlerReference5).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(null));
                allowing(handlerReference5).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
            }
        });

        HandlerHolder holder1 = new HandlerHolder(eventEngine, handlerReference1, false);
        HandlerHolder holder2 = new HandlerHolder(eventEngine, handlerReference2, false);
        HandlerHolder holder3 = new HandlerHolder(eventEngine, handlerReference3, false);
        HandlerHolder holder4 = new HandlerHolder(eventEngine, handlerReference4, false);
        HandlerHolder holder5 = new HandlerHolder(eventEngine, handlerReference5, false);

        // Put items in reverse order in list
        List<HandlerHolder> holders = Arrays.asList(holder5, holder4, holder3, holder2, holder1);
        assertSame(holder5, holders.get(0));
        assertSame(holder4, holders.get(1));
        assertSame(holder3, holders.get(2));
        assertSame(holder2, holders.get(3));
        assertSame(holder1, holders.get(4));

        // Sort the items and verify they are in the correct order
        Collections.sort(holders);

        // First by service ranking: highest to lowest. If rank is a match,
        // use service id lowest/oldest to highest/newest
        assertSame(holder3, holders.get(0)); // MAX_INT
        assertSame(holder1, holders.get(1)); // 1
        assertSame(holder2, holders.get(2)); // 0 -- service id = 21
        assertSame(holder5, holders.get(3)); // 0 -- service id = 24
        assertSame(holder4, holders.get(4)); // MIN_INT
    }

    @Test
    public void testBadTopicStrings() {
        final String[] topics = new String[] { "/com/ibm/Topic1", "com/ibm/Topic2/", "", "//", "foo//bar" };
        final ServiceReference handlerReference = context.mock(ServiceReference.class, "handlerReference");
        context.checking(new Expectations() {
            {
                allowing(handlerReference).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue(topics));
                allowing(handlerReference).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(handlerReference).getProperty(Constants.SERVICE_ID);
                will(returnValue(20L));
                allowing(handlerReference).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(1));
                allowing(handlerReference).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
            }
        });

        HandlerHolder holder = new HandlerHolder(eventEngine, handlerReference, false);
        assertTrue(holder.getWildcardTopics().isEmpty());
        assertTrue(holder.getDiscreteTopics().isEmpty());
    }

    @Test
    public void testNullContext() {
        final ServiceReference handlerReference = context.mock(ServiceReference.class, "handlerReference");
        context.checking(new Expectations() {
            {
                allowing(handlerReference).getProperty(EventConstants.EVENT_TOPIC);
                will(returnValue(null));
                allowing(handlerReference).getProperty(EventConstants.EVENT_FILTER);
                will(returnValue(null));
                allowing(handlerReference).getProperty(Constants.SERVICE_ID);
                will(returnValue(20L));
                allowing(handlerReference).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(1));
                allowing(handlerReference).getProperty(EventEngine.REENTRANT_HANDLER);
                will(returnValue(null));
            }
        });
        HandlerHolder holder = new HandlerHolder(eventEngine, handlerReference, false);
        assertNull("Should return null with null context (no NPE)", holder.getService());
    }
}
