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

import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.ui.internal.rest.exceptions.RESTException;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 *
 */
public class APIRootTest {
    private final Mockery mock = new JUnit4Mockery();;
    private final RESTRequest restRequest = mock.mock(RESTRequest.class);
    private final RESTResponse restResponse = mock.mock(RESTResponse.class);

    private APIRoot handler;

    @Before
    public void setUp() {
        handler = new APIRoot();
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
                     "/adminCenter", handler.baseURL());
        assertFalse("The handler should not expect to handle children",
                    handler.hasChildren());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.APIRoot#getBase(RESTRequest, RESTResponse)}.
     *
     * @throws RESTException
     */
    @Test
    public void getBase() throws RESTException {
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getURL();
                will(returnValue("/adminCenter"));
            }
        });

        Map<String, String> map = handler.getBase(restRequest, restResponse);
        assertTrue("FAIL: the API root JSON object did not contain a 'v1' field",
                   map.containsKey("v1"));
        assertEquals("FAIL: the API root 'v1' URL was incorrect",
                     "/adminCenter/v1",
                     map.get("v1"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.APIRoot#getBase(RESTRequest, RESTResponse)}.
     *
     * @throws RESTException
     */
    @Test
    public void doGETBase_trailingSlash() throws RESTException {
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(restRequest).getURL();
                will(returnValue("/adminCenter/"));
            }
        });

        Map<String, String> map = handler.getBase(restRequest, restResponse);
        assertTrue("FAIL: the API root JSON object did not contain a 'v1' field",
                   map.containsKey("v1"));
        assertEquals("FAIL: the API root 'v1' URL was incorrect",
                     "/adminCenter/v1",
                     map.get("v1"));
    }

}
