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

import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 *
 */
public class UtilsRootTest {
    private final Mockery mock = new JUnit4Mockery();;
    private final RESTRequest restRequest = mock.mock(RESTRequest.class);
    private final RESTResponse restResponse = mock.mock(RESTResponse.class);

    private UtilsRoot handler;

    @Before
    public void setUp() {
        handler = new UtilsRoot();
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
                     "/adminCenter/v1/utils", handler.baseURL());
        assertFalse("The handler should not expect to handle children",
                    handler.hasChildren());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.utils.UtilsRoot#getBase(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
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
                will(returnValue("/adminCenter/v1/utils"));
            }
        });

        Map<String, String> obj = handler.getBase(restRequest, restResponse);

        assertTrue("FAIL: the v1 utils JSON object did not contain a 'url' field",
                   obj.containsKey("url"));
        assertEquals("FAIL: the v1 utils 'url' URL was incorrect",
                     V1UtilsConstants.URL_UTILS_PATH,
                     obj.get("url"));

        assertTrue("FAIL: the v1 utils JSON object did not contain a 'feature' field",
                   obj.containsKey("feature"));
        assertEquals("FAIL: the v1 utils 'feature' URL was incorrect",
                     V1UtilsConstants.FEATURE_UTILS_PATH,
                     obj.get("feature"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.utils.UtilsRoot#getBase(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
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
                will(returnValue("/adminCenter/v1/utils/"));
            }
        });

        Map<String, String> obj = handler.getBase(restRequest, restResponse);

        assertTrue("FAIL: the v1 utils JSON object did not contain a 'url' field",
                   obj.containsKey("url"));
        assertEquals("FAIL: the v1 utils 'url' URL was incorrect",
                     V1UtilsConstants.URL_UTILS_PATH,
                     obj.get("url"));

        assertTrue("FAIL: the v1 utils JSON object did not contain a 'feature' field",
                   obj.containsKey("feature"));
        assertEquals("FAIL: the v1 utils 'feature' URL was incorrect",
                     V1UtilsConstants.FEATURE_UTILS_PATH,
                     obj.get("feature"));
    }

}
