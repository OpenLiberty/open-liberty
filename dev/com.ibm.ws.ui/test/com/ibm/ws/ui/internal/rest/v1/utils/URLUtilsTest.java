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
package com.ibm.ws.ui.internal.rest.v1.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.ui.internal.rest.HTTPConstants;
import com.ibm.ws.ui.internal.rest.exceptions.BadRequestException;
import com.ibm.ws.ui.internal.rest.exceptions.NoSuchResourceException;
import com.ibm.ws.ui.internal.v1.pojo.Message;
import com.ibm.ws.ui.internal.v1.utils.URLUtility;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 *
 */
public class URLUtilsTest {
    private final Mockery mock = new JUnit4Mockery();;
    private final RESTRequest restRequest = mock.mock(RESTRequest.class);
    private final RESTResponse restResponse = mock.mock(RESTResponse.class);
    private final URLUtility mockUtils = mock.mock(URLUtility.class);

    private URLUtils handler;

    @Before
    public void setUp() {
        handler = new URLUtils(mockUtils);
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    /**
     * Validates the setup of the object matches our expectations. This is
     * not really a business logic test.
     */
    @Test
    public void objectValidation() {
        assertEquals("The handler's base URL was not what was expected",
                     "/adminCenter/v1/utils/url", handler.baseURL());
        assertTrue("The handler should expect to handle children",
                   handler.hasChildren());
    }

    @Test
    public void isKnownChildResource_getTool() {
        assertTrue("'getTool' should not be a known child",
                   handler.isKnownChildResource("getTool", null));
    }

    @Test
    public void isKnownChildResource_other() {
        assertFalse("'other' should not be a known child",
                    handler.isKnownChildResource("other", null));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.utils.URLUtils#getBase(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void getBase_root() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getURL();
                will(returnValue("/adminCenter/v1/utils/url"));
            }
        });

        Map<String, String> obj = handler.getBase(restRequest, restResponse);

        assertTrue("FAIL: the URL utils JSON object did not contain a 'getTool' field",
                   obj.containsKey("getTool"));
        assertEquals("FAIL: the URL utils 'getTool' URL was incorrect",
                     "/adminCenter/v1/utils/url/getTool",
                     obj.get("getTool"));

        assertTrue("FAIL: the URL utils JSON object did not contain a 'getTool' field",
                   obj.containsKey("getTool"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.utils.URLUtils#getBase(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void getBase_rootTrailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getURL();
                will(returnValue("/adminCenter/v1/utils/url/"));
            }
        });

        Map<String, String> obj = handler.getBase(restRequest, restResponse);

        assertTrue("FAIL: the URL utils JSON object did not contain a 'getTool' field",
                   obj.containsKey("getTool"));
        assertEquals("FAIL: the URL utils 'getTool' URL was incorrect",
                     "/adminCenter/v1/utils/url/getTool",
                     obj.get("getTool"));

        assertTrue("FAIL: the URL utils JSON object did not contain a 'getTool' field",
                   obj.containsKey("getTool"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.utils.URLUtils#getChild(RESTRequest, RESTResponse, String)}.
     */
    @Test(expected = NoSuchResourceException.class)
    public void getChild_getStatus_undefinedChild() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));
            }
        });
        handler.getChild(restRequest, restResponse, "other");
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.utils.URLUtils#getChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void getChild_getTool_noURL() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getParameter("url");
                will(returnValue(null));
            }
        });

        try {
            handler.getChild(restRequest, restResponse, "getTool");
        } catch (BadRequestException e) {
            assertEquals("Content type should be JSON",
                         HTTPConstants.MEDIA_TYPE_APPLICATION_JSON, e.getContentType());

            Message error = (Message) e.getPayload();
            assertEquals("ErrorMessage status should be 400",
                         400, error.getStatus());
            assertTrue("ErrorMessage message start with CWWKX1017E and contain getTool",
                       error.getMessage().matches("CWWKX1017E:.*getTool.*"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.utils.URLUtils#getChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void getChild_getTool_badURL() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getParameter("url");
                will(returnValue("notAURL"));
            }
        });

        try {
            handler.getChild(restRequest, restResponse, "getTool");
        } catch (BadRequestException e) {
            assertEquals("Content type should be JSON",
                         HTTPConstants.MEDIA_TYPE_APPLICATION_JSON, e.getContentType());

            Message error = (Message) e.getPayload();
            assertEquals("ErrorMessage status should be 400",
                         400, error.getStatus());
            assertTrue("ErrorMessage message start with CWWKX1018E and contain getTool",
                       error.getMessage().matches("CWWKX1018E:.*getTool.*"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.utils.URLUtils#getChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void getChild_getTool_invoked() throws Exception {
        final String url = "http://www.ibm.com";
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getParameter("url");
                will(returnValue(url));

                // Test assertion
                one(mockUtils).analyzeURL(with(any(URL.class)));
            }
        });

        handler.getChild(restRequest, restResponse, "getTool");
    }

}
