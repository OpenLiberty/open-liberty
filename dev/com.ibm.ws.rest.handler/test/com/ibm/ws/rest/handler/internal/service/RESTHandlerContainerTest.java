/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.internal.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

import com.ibm.wsspi.rest.handler.RESTHandler;

/**
 *
 */
public class RESTHandlerContainerTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery();
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<RESTHandler> handler1Ref = mock.mock(ServiceReference.class, "handler1Ref");
    @SuppressWarnings("unchecked")
    private final ServiceReference<RESTHandler> handler1ChildRef = mock.mock(ServiceReference.class, "handler1ChildRef");
    @SuppressWarnings("unchecked")
    private final ServiceReference<RESTHandler> handler2Ref = mock.mock(ServiceReference.class, "handler2Ref");
    @SuppressWarnings("unchecked")
    private final ServiceReference<RESTHandler> handler3aRef = mock.mock(ServiceReference.class, "handler3aRef");
    @SuppressWarnings("unchecked")
    private final ServiceReference<RESTHandler> handler3bRef = mock.mock(ServiceReference.class, "handler3bRef");
    private final RESTHandler handler1 = mock.mock(RESTHandler.class, "handler1");
    private final RESTHandler handler1Child = mock.mock(RESTHandler.class, "handler1Child");
    private final RESTHandler handler2 = mock.mock(RESTHandler.class, "handler2");
    private final RESTHandler handler3a = mock.mock(RESTHandler.class, "handler3a");
    private final RESTHandler handler3b = mock.mock(RESTHandler.class, "handler3b");
    private RESTHandlerContainerImpl container;

    @Before
    public void setUp() {
        mock.checking(new Expectations() {
            {
                allowing(cc).locateService(RESTHandlerContainerImpl.REST_HANDLER_REF, handler1Ref);
                will(returnValue(handler1));

                allowing(cc).locateService(RESTHandlerContainerImpl.REST_HANDLER_REF, handler1ChildRef);
                will(returnValue(handler1Child));

                allowing(cc).locateService(RESTHandlerContainerImpl.REST_HANDLER_REF, handler2Ref);
                will(returnValue(handler2));

                allowing(cc).locateService(RESTHandlerContainerImpl.REST_HANDLER_REF, handler3aRef);
                will(returnValue(handler3a));

                allowing(cc).locateService(RESTHandlerContainerImpl.REST_HANDLER_REF, handler3bRef);
                will(returnValue(handler3b));

            }
        });
        container = new RESTHandlerContainerImpl();
        container.activate(cc, null);
    }

    @After
    public void tearDown() {
        container.deactivate(cc, 0);
        container = null;

        mock.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#setRestHandler(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void setRestHandler_noProperty() {
        mock.checking(new Expectations() {
            {
                one(handler1Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_ROOT);
                will(returnValue(null));

                allowing(handler1Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT);
                will(returnValue(null));
            }
        });

        container.setRestHandler(handler1Ref);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#setRestHandler(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void setRestHandler_wrongPropertyType() {
        mock.checking(new Expectations() {
            {
                one(handler1Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_ROOT);
                will(returnValue(5));

                allowing(handler1Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT);
                will(returnValue(null));
            }
        });

        container.setRestHandler(handler1Ref);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#setRestHandler(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void setRestHandler_nonSlashURL() {
        mock.checking(new Expectations() {
            {
                allowing(handler1Ref).getProperty("service.id");
                will(returnValue(1L));

                allowing(handler1Ref).getProperty("service.ranking");
                will(returnValue(1L));

                one(handler1Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_ROOT);
                will(returnValue("handler"));

                allowing(handler1Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT);
                will(returnValue(null));
            }
        });

        container.setRestHandler(handler1Ref);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#setRestHandler(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void setRestHandler_emptyURL() {
        mock.checking(new Expectations() {
            {
                allowing(handler1Ref).getProperty("service.id");
                will(returnValue(1L));

                allowing(handler1Ref).getProperty("service.ranking");
                will(returnValue(1L));

                one(handler1Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_ROOT);
                will(returnValue(""));

                allowing(handler1Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT);
                will(returnValue(null));
            }
        });

        container.setRestHandler(handler1Ref);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#setRestHandler(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void setRestHandler_justSlashURL() {
        mock.checking(new Expectations() {
            {
                allowing(handler1Ref).getProperty("service.id");
                will(returnValue(1L));

                allowing(handler1Ref).getProperty("service.ranking");
                will(returnValue(1L));

                one(handler1Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_ROOT);
                will(returnValue("/"));

                allowing(handler1Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT);
                will(returnValue(null));

                allowing(handler1Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_HIDDEN_API);
                will(returnValue(false));
            }
        });

        container.setRestHandler(handler1Ref);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#unsetRestHandler(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void unsetRestHandler_noProperty() {
        mock.checking(new Expectations() {
            {
                one(handler1Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_ROOT);
                will(returnValue(null));

                allowing(handler1Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT);
                will(returnValue(null));
            }
        });
        container.unsetRestHandler(handler1Ref);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#unsetRestHandler(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void unsetRestHandler_wrongPropertyType() {
        mock.checking(new Expectations() {
            {
                one(handler1Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_ROOT);
                will(returnValue(5));

                allowing(handler1Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT);
                will(returnValue(null));
            }
        });

        container.unsetRestHandler(handler1Ref);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#unsetRestHandler(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void unsetRestHandler_nonSlashURL() {
        mock.checking(new Expectations() {
            {
                one(handler1Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_ROOT);
                will(returnValue("handler"));

                allowing(handler1Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT);
                will(returnValue(null));
            }
        });

        container.unsetRestHandler(handler1Ref);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#unsetRestHandler(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void unsetRestHandler_slashURL() {
        mock.checking(new Expectations() {
            {
                one(handler1Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_ROOT);
                will(returnValue("/handler"));

                allowing(handler1Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT);
                will(returnValue(null));
            }
        });

        container.unsetRestHandler(handler1Ref);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#getHandler(java.lang.String)}.
     * <p>
     * Defensive programming test!
     */
    @Test
    public void getHandler_nullURL() {
        assertNull("When the request URL is null, we should return null back (no NPE)",
                   container.getHandler(null));
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#getHandler(java.lang.String)}.
     * <p>This situation should never occur but code defensively if it does.</p>
     */
    @Test
    public void getHandler_justSlash() {
        assertNull("When the request URL is a slash, we should return null back since that is the base URL and not a handler URL",
                   container.getHandler("/ibm/api/"));
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#getHandler(java.lang.String)}.
     * <p>This situation should never occur but code defensively if it does.</p>
     */
    @Test
    public void getHandler_noHandlers() {
        assertNull("When there are no handlers, null should be returned",
                   container.getHandler("/ibm/api/handler1"));
    }

    /**
     * Set up the mock behaviour for handler1.
     */
    private void setHandler1Mock() {
        mock.checking(new Expectations() {
            {
                allowing(handler1Ref).getProperty("service.id");
                will(returnValue(1L));

                allowing(handler1Ref).getProperty("service.ranking");
                will(returnValue(0L));

                allowing(handler1Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_ROOT);
                will(returnValue("/handler1"));

                allowing(handler1Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT);
                will(returnValue(null));

                allowing(handler1Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_HIDDEN_API);
                will(returnValue(false));

                allowing(handler1Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_CUSTOM_SECURITY);
                will(returnValue(null));

            }

        });
    }

    /**
     * Set up the mock behaviour for handler1's child.
     */
    private void setHandler1ChildMock() {
        mock.checking(new Expectations() {
            {
                allowing(handler1ChildRef).getProperty("service.id");
                will(returnValue(11L));

                allowing(handler1ChildRef).getProperty("service.ranking");
                will(returnValue(0L));

                allowing(handler1ChildRef).getProperty(RESTHandler.PROPERTY_REST_HANDLER_ROOT);
                will(returnValue("/handler1/child"));

                allowing(handler1ChildRef).getProperty(RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT);
                will(returnValue(null));

                allowing(handler1ChildRef).getProperty(RESTHandler.PROPERTY_REST_HANDLER_HIDDEN_API);
                will(returnValue(false));

                allowing(handler1ChildRef).getProperty(RESTHandler.PROPERTY_REST_HANDLER_CUSTOM_SECURITY);
                will(returnValue(null));
            }
        });
    }

    /**
     * Set up the mock behaviour for handler2.
     */
    private void setHandler2Mock() {
        mock.checking(new Expectations() {
            {
                allowing(handler2Ref).getProperty("service.id");
                will(returnValue(2L));

                allowing(handler2Ref).getProperty("service.ranking");
                will(returnValue(0L));

                allowing(handler2Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_ROOT);
                will(returnValue("/handler2"));

                allowing(handler2Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT);
                will(returnValue(null));

                allowing(handler2Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_HIDDEN_API);
                will(returnValue(false));

                allowing(handler2Ref).getProperty(RESTHandler.PROPERTY_REST_HANDLER_CUSTOM_SECURITY);
                will(returnValue(null));

            }
        });
    }

    /**
     * Set up the mock behaviour for handler3 variant A, which has a lower
     * service ranking than variant B.
     */
    private void setHandler3aMock() {
        mock.checking(new Expectations() {
            {
                allowing(handler3aRef).getProperty("service.id");
                will(returnValue(31L));

                allowing(handler3aRef).getProperty("service.ranking");
                will(returnValue(1L));

                allowing(handler3aRef).getProperty(RESTHandler.PROPERTY_REST_HANDLER_ROOT);
                will(returnValue("/handler3"));

                allowing(handler3aRef).getProperty(RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT);
                will(returnValue(null));

                allowing(handler3aRef).getProperty(RESTHandler.PROPERTY_REST_HANDLER_HIDDEN_API);
                will(returnValue(false));

                allowing(handler3aRef).getProperty(RESTHandler.PROPERTY_REST_HANDLER_CUSTOM_SECURITY);
                will(returnValue(null));
            }
        });
    }

    /**
     * Set up the mock behaviour for handler3 variant B, which has a higher
     * service ranking than variant A.
     */
    private void setHandler3bMock() {
        mock.checking(new Expectations() {
            {
                allowing(handler3bRef).getProperty("service.id");
                will(returnValue(32L));

                allowing(handler3bRef).getProperty("service.ranking");
                will(returnValue(2L));

                allowing(handler3bRef).getProperty(RESTHandler.PROPERTY_REST_HANDLER_ROOT);
                will(returnValue("/handler2"));

                allowing(handler3bRef).getProperty(RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT);
                will(returnValue(null));

                allowing(handler3bRef).getProperty(RESTHandler.PROPERTY_REST_HANDLER_HIDDEN_API);
                will(returnValue(false));

                allowing(handler3bRef).getProperty(RESTHandler.PROPERTY_REST_HANDLER_CUSTOM_SECURITY);
                will(returnValue(null));
            }
        });
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#getHandler(java.lang.String)}.
     */
    @Test
    public void getHandler_oneHandlerNoMatch() {
        setHandler1Mock();

        container.setRestHandler(handler1Ref);

        assertNull("When the request URL has no handler, we should return null",
                   container.getHandler("/ibm/api/handler3"));
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#getHandler(java.lang.String)}.
     */
    @Test
    public void getHandler_twoHandlersNoMatch() {
        setHandler1Mock();
        setHandler2Mock();

        container.setRestHandler(handler1Ref);
        container.setRestHandler(handler2Ref);

        assertNull("When the request URL has no handler, we should return null",
                   container.getHandler("/ibm/api/handler3"));
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#getHandler(java.lang.String)}.
     */
    @Test
    public void getHandler_oneHandlerMatches() {
        setHandler1Mock();

        container.setRestHandler(handler1Ref);

        assertSame("When the request URL has a handler, that handler should be returned",
                   handler1, container.getHandler("/ibm/api/handler1").handler);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#getHandler(java.lang.String)}.
     */
    @Test
    public void getHandler_oneHandlerMatchesTrailingSlash() {
        setHandler1Mock();

        container.setRestHandler(handler1Ref);

        assertSame("When the request URL has a handler, that handler should be returned",
                   handler1, container.getHandler("/ibm/api/handler1/").handler);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#getHandler(java.lang.String)}.
     * <p>This should never actually happen as getHandler() is called with the
     * path, but its best to code defensively.</p>
     */
    @Test
    public void getHandler_oneHandlerMatchesQueryParams() {
        setHandler1Mock();

        container.setRestHandler(handler1Ref);

        assertSame("When the request URL has a handler, that handler should be returned",
                   handler1, container.getHandler("/ibm/api/handler1?field=value").handler);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#getHandler(java.lang.String)}.
     */
    @Test
    public void getHandler_twoHandlerMatchOne() {
        setHandler1Mock();
        setHandler2Mock();

        container.setRestHandler(handler1Ref);
        container.setRestHandler(handler2Ref);

        assertSame("When the request URL has a handler, that handler should be returned",
                   handler1, container.getHandler("/ibm/api/handler1").handler);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#getHandler(java.lang.String)}.
     */
    @Test
    public void getHandler_twoHandlerMatchTwo() {
        setHandler1Mock();
        setHandler2Mock();

        container.setRestHandler(handler1Ref);
        container.setRestHandler(handler2Ref);

        assertSame("When the request URL has a handler, that handler should be returned",
                   handler2, container.getHandler("/ibm/api/handler2").handler);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#getHandler(java.lang.String)}.
     * <p>The expected behaviour is to match the service ranking order,
     * which is handler3a first.</p>
     */
    @Test
    public void getHandler_twoHandlersMatchOnServiceRank() {
        setHandler3aMock();
        setHandler3bMock();

        container.setRestHandler(handler3aRef);
        container.setRestHandler(handler3bRef);

        assertSame("When the request URL has 2 handlers, the handler with lowest service rank hould be returned",
                   handler3a, container.getHandler("/ibm/api/handler3").handler);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#getHandler(java.lang.String)}.
     * <p>The expected behaviour is to match the service ranking order,
     * which is handler3a first.</p>
     */
    @Test
    public void getHandler_threeHandlersMatchOnServiceRankDifferentOrder() {
        setHandler3aMock();
        setHandler3bMock();

        container.setRestHandler(handler3bRef);
        container.setRestHandler(handler3aRef);

        assertSame("When the request URL has 2 handlers, the handler with lowest service rank hould be returned",
                   handler3a, container.getHandler("/ibm/api/handler3").handler);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#getHandler(java.lang.String)}.
     */
    @Test
    public void getHandler_parentHandlerMatchesChild() {
        setHandler1Mock();

        container.setRestHandler(handler1Ref);

        assertSame("When the request URL is a child of the handler, we should return that handler",
                   handler1, container.getHandler("/ibm/api/handler1/child").handler);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#getHandler(java.lang.String)}.
     */
    @Test
    public void getHandler_childHandlersDoesNotMatchParent() {
        setHandler1ChildMock();

        container.setRestHandler(handler1ChildRef);

        assertNull("A child handler should not handle the parent URL",
                   container.getHandler("/ibm/api/ibm/api/handler1/"));
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#getHandler(java.lang.String)}.
     */
    @Test
    public void getHandler_parentAndChildHandlersBestMatch() {
        setHandler1Mock();
        setHandler1ChildMock();

        container.setRestHandler(handler1Ref);
        container.setRestHandler(handler1ChildRef);

        assertSame("The child handler should always take precidence over the parent handler",
                   handler1Child, container.getHandler("/ibm/api/handler1/child").handler);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#getHandler(java.lang.String)}.
     */
    @Test
    public void getHandler_parentAndChildHandlersBestMatchTrailingSlash() {
        setHandler1Mock();
        setHandler1ChildMock();

        container.setRestHandler(handler1Ref);
        container.setRestHandler(handler1ChildRef);

        assertSame("The child handler should always take precidence over the parent handler",
                   handler1Child, container.getHandler("/ibm/api/handler1/child/").handler);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#getHandler(java.lang.String)}.
     * <p>DO NOT DELETE THIS TEST. THIS CATCHES A BUILD BREAK PROBLEM.</p>
     * <p>There was a point in time where the algorithm to find the best match
     * did not properly handle this scenario.</p>
     */
    @Test
    public void getHandler_parentAndChildHandlersBestMatchDifferentOrder() {
        setHandler1Mock();
        setHandler1ChildMock();

        container.setRestHandler(handler1ChildRef);
        container.setRestHandler(handler1Ref);

        assertSame("The child handler should always take precidence over the parent handler",
                   handler1Child, container.getHandler("/ibm/api/handler1/child").handler);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#getHandler(java.lang.String)}.
     */
    @Test
    public void getHandler_parentAndChildHandlersBestMatchDeepChild() {
        setHandler1Mock();
        setHandler1ChildMock();

        container.setRestHandler(handler1Ref);
        container.setRestHandler(handler1ChildRef);

        assertSame("The child handler should always take precidence over the parent handler",
                   handler1Child, container.getHandler("/ibm/api/handler1/child/deep").handler);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#getHandler(java.lang.String)}.
     */
    @Test
    public void getHandler_parentAndChildHandlersBestMatchDeepChildDifferentOrder() {
        setHandler1Mock();
        setHandler1ChildMock();

        container.setRestHandler(handler1ChildRef);
        container.setRestHandler(handler1Ref);

        assertSame("The child handler should always take precidence over the parent handler",
                   handler1Child, container.getHandler("/ibm/api/handler1/child/deep").handler);
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#getHandler(java.lang.String)}.
     */
    @Test
    public void getHandler_doNotMatchSubstrings() {
        setHandler1Mock();

        container.setRestHandler(handler1Ref);

        assertNull("When the request URL is substring of the handler root, we should return null back",
                   container.getHandler("/ibm/api/handler"));
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#getHandler(java.lang.String)}.
     */
    @Test
    public void getHandler_doNotMatchSuperstrings() {
        setHandler1Mock();

        container.setRestHandler(handler1Ref);

        assertNull("When the request URL is a superstring of the handler root (but not a child resource), we should return null back",
                   container.getHandler("/ibm/api/handler12"));
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.internal.service.RESTHandlerContainerImpl#registeredKeys()}.
     */
    @Test
    public void registeredKeys() {
        assertNotNull("The container should return the backing Iterator",
                      container.registeredKeys());
    }
}
