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
package com.ibm.ws.ui.internal.rest.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.ui.internal.rest.HTTPConstants;
import com.ibm.ws.ui.internal.rest.exceptions.NoSuchResourceException;
import com.ibm.ws.ui.internal.rest.exceptions.RESTException;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 *
 */
public class V1RootTest {
    private final Mockery mock = new JUnit4Mockery();;
    private final RESTRequest restRequest = mock.mock(RESTRequest.class);
    private final RESTResponse restResponse = mock.mock(RESTResponse.class);

    private V1Root handler;

    @Before
    public void setUp() {
        handler = new V1Root();
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    /**
     * Validates the setup of the object matches our expectations. This is a
     * validation and not really a business logic test.
     */
    @Test
    public void objectValidation() {
        assertEquals("The handler's base URL was not what was expected",
                     "/adminCenter/v1", handler.baseURL());
        assertTrue("The handler should expect to handle children",
                   handler.hasChildren());
    }

    @Test
    public void isKnownChildResource_coffee() {
        assertTrue("'coffee' should be a known child",
                   handler.isKnownChildResource("coffee", null));
    }

    @Test
    public void isKnownChildResource_other() {
        assertFalse("'other' is not a known child",
                    handler.isKnownChildResource("other", null));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.V1Root#getBase(RESTRequest, RESTResponse)}.
     */
    @Test
    public void getBase() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getURL();
                will(returnValue("/adminCenter/v1"));
            }
        });

        Map<String, String> obj = handler.getBase(restRequest, restResponse);

        assertTrue("FAIL: the v1 JSON object did not contain a 'catalog' field",
                   obj.containsKey("catalog"));
        assertEquals("FAIL: the v1 'catalog' URL was incorrect",
                     V1Constants.CATALOG_PATH,
                     obj.get("catalog"));

        assertTrue("FAIL: the v1 JSON object did not contain a 'toolbox' field",
                   obj.containsKey("toolbox"));
        assertEquals("FAIL: the v1 'toolbox' URL was incorrect",
                     V1Constants.TOOLBOX_PATH,
                     obj.get("toolbox"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.V1Root#getBase(RESTRequest, RESTResponse)}.
     */
    @Test
    public void getBase_trailingSlash() throws Exception {
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getURL();
                will(returnValue("/adminCenter/v1/"));
            }
        });

        Map<String, String> obj = handler.getBase(restRequest, restResponse);

        assertTrue("FAIL: the v1 JSON object did not contain a 'catalog' field",
                   obj.containsKey("catalog"));
        assertEquals("FAIL: the v1 'catalog' URL was incorrect",
                     V1Constants.CATALOG_PATH,
                     obj.get("catalog"));

        assertTrue("FAIL: the v1 JSON object did not contain a 'toolbox' field",
                   obj.containsKey("toolbox"));
        assertEquals("FAIL: the v1 'toolbox' URL was incorrect",
                     V1Constants.TOOLBOX_PATH,
                     obj.get("toolbox"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.V1Root#getChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void getChild_coffee() throws Exception {
        try {
            handler.getChild(restRequest, restResponse, "coffee");
            fail("Should have caught a RESTException");
        } catch (RESTException e) {
            assertEquals("FAIL: did not get back expected \"I'm a teapot\" status",
                         418,
                         e.getStatus());
            assertEquals("FAIL: did not get back expected \"I'm a teapot\" entity",
                         HTTPConstants.MEDIA_TYPE_TEXT_PLAIN,
                         e.getContentType());
            assertEquals("FAIL: did not get back expected \"I'm a teapot\" entity",
                         "I'm a teapot!",
                         e.getPayload());
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.V1Root#getChild(RESTRequest, RESTResponse, String)}.
     */
    @Test(expected = NoSuchResourceException.class)
    public void getChild_undefinedChild() throws Exception {
        handler.getChild(restRequest, restResponse, "other");
    }
}
