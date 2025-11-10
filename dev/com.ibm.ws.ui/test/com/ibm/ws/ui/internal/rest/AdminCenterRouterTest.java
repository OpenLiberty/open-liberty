/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.ui.internal.RequestNLS;
import com.ibm.ws.ui.internal.rest.v1.CatalogAPI;
import com.ibm.ws.ui.internal.rest.v1.DeployValidation;
import com.ibm.ws.ui.internal.rest.v1.IconRestHandler;
import com.ibm.ws.ui.internal.rest.v1.ToolDataAPI;
import com.ibm.ws.ui.internal.rest.v1.ToolboxAPI;
import com.ibm.ws.ui.internal.rest.v1.V1Root;
import com.ibm.ws.ui.internal.rest.v1.utils.FeatureUtils;
import com.ibm.ws.ui.internal.rest.v1.utils.URLUtils;
import com.ibm.ws.ui.internal.rest.v1.utils.UtilsRoot;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 *
 */
public class AdminCenterRouterTest {
    private final Mockery mock = new JUnit4Mockery();;
    private final RESTRequest restRequest = mock.mock(RESTRequest.class);
    private final RESTResponse restResponse = mock.mock(RESTResponse.class);
    private final AdminCenterRestHandler baseHandler = mock.mock(AdminCenterRestHandler.class, "baseHandler");
    private final AdminCenterRestHandler handler1 = mock.mock(AdminCenterRestHandler.class, "handler1");
    private final AdminCenterRestHandler handler1subHandler = mock.mock(AdminCenterRestHandler.class, "handler1subHandler");
    private final AdminCenterRestHandler handler2 = mock.mock(AdminCenterRestHandler.class, "handler2");
    private AdminCenterRouter router;

    @Before
    public void setUp() {
        mock.checking(new Expectations() {
            {
                allowing(baseHandler).hasChildren();
                will(returnValue(false));

                allowing(handler1).hasChildren();
                will(returnValue(true));

                allowing(handler1subHandler).hasChildren();
                will(returnValue(true));

                allowing(handler2).hasChildren();
                will(returnValue(false));
            }
        });
        Map<String, AdminCenterRestHandler> testHandlers = new HashMap<String, AdminCenterRestHandler>();
        testHandlers.put("/adminCenter", baseHandler);
        testHandlers.put("/adminCenter/handler1", handler1);
        testHandlers.put("/adminCenter/handler1/subHandler", handler1subHandler);
        testHandlers.put("/adminCenter/handler2", handler2);
        router = new AdminCenterRouter(testHandlers);
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    @AfterClass
    public static void resetRESTHandler() {
        RequestNLS.setRESTRequest(null);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.AdminCenterRouter#AdminCenterRouter()}.
     */
    @Test
    public void defaultAdminCenterRouter() {
        AdminCenterRouter defaultRouter = new AdminCenterRouter();
        defaultRouter.activate();

        Map<String, AdminCenterRestHandler> handlers = defaultRouter.getHandlers();
        assertEquals("The default handlers map had an unexpected number of elements. Was a handler removed or added?",
                     10, handlers.size());

        // Validate the mapping is correct. This guards against accidental changes.
        assertTrue("URL to handler mismatch", handlers.get("/adminCenter") instanceof APIRoot);
        assertTrue("URL to handler mismatch", handlers.get("/adminCenter/v1") instanceof V1Root);
        assertTrue("URL to handler mismatch", handlers.get("/adminCenter/v1/catalog") instanceof CatalogAPI);
        assertTrue("URL to handler mismatch", handlers.get("/adminCenter/v1/toolbox") instanceof ToolboxAPI);
        assertTrue("URL to handler mismatch", handlers.get("/adminCenter/v1/utils") instanceof UtilsRoot);
        assertTrue("URL to handler mismatch", handlers.get("/adminCenter/v1/utils/feature") instanceof FeatureUtils);
        assertTrue("URL to handler mismatch", handlers.get("/adminCenter/v1/utils/url") instanceof URLUtils);
        assertTrue("URL to handler mismatch", handlers.get("/adminCenter/v1/icons") instanceof IconRestHandler);
        assertTrue("URL to handler mismatch", handlers.get("/adminCenter/v1/deployValidation") instanceof DeployValidation);
        assertTrue("URL to handler mismatch", handlers.get("/adminCenter/v1/tooldata") instanceof ToolDataAPI);

        for (String key : handlers.keySet()) {
            assertFalse("Path " + key + "ends with a '/', this will cause problems. Do not register keys with trailing slashes",
                        key.endsWith("/"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.AdminCenterRouter#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_nullPath() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getURI();
                will(returnValue(null));

                one(restRequest).getContextPath();
                will(returnValue(null));

                one(restRequest).getPath();
                will(returnValue(null));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        router.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.AdminCenterRouter#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_emptyPath() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getURI();
                will(returnValue(""));

                one(restRequest).getContextPath();
                will(returnValue(""));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        router.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.AdminCenterRouter#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_noHandlers() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getURI();
                will(returnValue("/ibm/api/adminCenter"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        // Create with an empty map of handlers
        router = new AdminCenterRouter(new HashMap<String, AdminCenterRestHandler>());
        router.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.AdminCenterRouter#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_noMatch() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getURI();
                will(returnValue("/ibm/api/wrong"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        router.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.AdminCenterRouter#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_noMatchSubstrings() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getURI();
                will(returnValue("/ibm/api/adminU"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        router.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.AdminCenterRouter#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_noMatchSubstringsTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getURI();
                will(returnValue("/ibm/api/adminU/"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        router.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.AdminCenterRouter#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_noMatchSuperstrings() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getURI();
                will(returnValue("/ibm/api/adminCenterv1"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        router.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.AdminCenterRouter#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_noMatchSuperstringsTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getURI();
                will(returnValue("/ibm/api/adminCenterv1/"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        router.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.AdminCenterRouter#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_directMatchHandler() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getURI();
                will(returnValue("/ibm/api/adminCenter"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                one(restResponse).setResponseHeader("X-XSS-Protection", "1");
                one(restResponse).setResponseHeader("X-Content-Type-Options", "nosniff");
                one(restResponse).setResponseHeader("X-Frame-Options", "SAMEORIGIN");
                one(restResponse).setResponseHeader("Content-Security-Policy", "default-src 'self'; form-action 'self'; frame-ancestors 'self'");

                // Test assertion
                one(baseHandler).handleRequest(restRequest, restResponse);
            }
        });

        router.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.AdminCenterRouter#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_matchHandlerTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getURI();
                will(returnValue("/ibm/api/adminCenter/"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                one(restResponse).setResponseHeader("X-XSS-Protection", "1");
                one(restResponse).setResponseHeader("X-Content-Type-Options", "nosniff");
                one(restResponse).setResponseHeader("X-Frame-Options", "SAMEORIGIN");
                one(restResponse).setResponseHeader("Content-Security-Policy", "default-src 'self'; form-action 'self'; frame-ancestors 'self'");

                // Test assertion
                one(baseHandler).handleRequest(restRequest, restResponse);
            }
        });

        router.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.AdminCenterRouter#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_matchSubHandler() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getURI();
                will(returnValue("/ibm/api/adminCenter/handler1"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                one(restResponse).setResponseHeader("X-XSS-Protection", "1");
                one(restResponse).setResponseHeader("X-Content-Type-Options", "nosniff");
                one(restResponse).setResponseHeader("X-Frame-Options", "SAMEORIGIN");
                one(restResponse).setResponseHeader("Content-Security-Policy", "default-src 'self'; form-action 'self'; frame-ancestors 'self'");

                // Test assertion
                one(handler1).handleRequest(restRequest, restResponse);
            }
        });

        router.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.AdminCenterRouter#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_matchSubHandlerTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getURI();
                will(returnValue("/ibm/api/adminCenter/handler1/"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));   

                one(restResponse).setResponseHeader("X-XSS-Protection", "1");
                one(restResponse).setResponseHeader("X-Content-Type-Options", "nosniff");
                one(restResponse).setResponseHeader("X-Frame-Options", "SAMEORIGIN");
                one(restResponse).setResponseHeader("Content-Security-Policy", "default-src 'self'; form-action 'self'; frame-ancestors 'self'");

                // Test assertion
                one(handler1).handleRequest(restRequest, restResponse);
            }
        });

        router.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.AdminCenterRouter#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_matchChildHandler() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getURI();
                will(returnValue("/ibm/api/adminCenter/handler1/child"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                one(restResponse).setResponseHeader("X-XSS-Protection", "1");
                one(restResponse).setResponseHeader("X-Content-Type-Options", "nosniff");
                one(restResponse).setResponseHeader("X-Frame-Options", "SAMEORIGIN");
                one(restResponse).setResponseHeader("Content-Security-Policy", "default-src 'self'; form-action 'self'; frame-ancestors 'self'");

                // Test assertion
                one(handler1).handleRequest(restRequest, restResponse);
            }
        });

        router.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.AdminCenterRouter#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_matchChildHandlerTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getURI();
                will(returnValue("/ibm/api/adminCenter/handler1/child/"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                one(restResponse).setResponseHeader("X-XSS-Protection", "1");
                one(restResponse).setResponseHeader("X-Content-Type-Options", "nosniff");
                one(restResponse).setResponseHeader("X-Frame-Options", "SAMEORIGIN");
                one(restResponse).setResponseHeader("Content-Security-Policy", "default-src 'self'; form-action 'self'; frame-ancestors 'self'");

                // Test assertion
                one(handler1).handleRequest(restRequest, restResponse);
            }
        });

        router.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.AdminCenterRouter#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_doNotMatchChildWhenNoChildren() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getURI();
                will(returnValue("/ibm/api/adminCenter/handler2/child"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        router.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.AdminCenterRouter#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_doNotMatchChildWhenNoChildrenTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getURI();
                will(returnValue("/ibm/api/adminCenter/handler2/child/"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        router.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.AdminCenterRouter#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_doNotMatchGrandchild() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getURI();
                will(returnValue("/ibm/api/adminCenter/handler2/child/grandchild"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        router.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.AdminCenterRouter#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_doNotMatchGrandchildTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getURI();
                will(returnValue("/ibm/api/adminCenter/handler2/child/grandchild/"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                // Test assertion
                one(restResponse).setStatus(404);
            }
        });

        router.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.AdminCenterRouter#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_matchGrandchild() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getURI();
                will(returnValue("/ibm/api/adminCenter/handler1/child/grandchild"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                one(restResponse).setResponseHeader("X-XSS-Protection", "1");
                one(restResponse).setResponseHeader("X-Content-Type-Options", "nosniff");
                one(restResponse).setResponseHeader("X-Frame-Options", "SAMEORIGIN");
                one(restResponse).setResponseHeader("Content-Security-Policy", "default-src 'self'; form-action 'self'; frame-ancestors 'self'");

                // Test assertion
                one(handler1).handleRequest(restRequest, restResponse);
            }
        });

        router.handleRequest(restRequest, restResponse);
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.AdminCenterRouter#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void handleRequest_matchGrandchildTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getURI();
                will(returnValue("/ibm/api/adminCenter/handler1/child/grandchild/"));

                one(restRequest).getContextPath();
                will(returnValue("/ibm/api"));

                one(restResponse).setResponseHeader("X-XSS-Protection", "1");
                one(restResponse).setResponseHeader("X-Content-Type-Options", "nosniff");
                one(restResponse).setResponseHeader("X-Frame-Options", "SAMEORIGIN");
                one(restResponse).setResponseHeader("Content-Security-Policy", "default-src 'self'; form-action 'self'; frame-ancestors 'self'");

                // Test assertion
                one(handler1).handleRequest(restRequest, restResponse);
            }
        });

        router.handleRequest(restRequest, restResponse);
    }
}
