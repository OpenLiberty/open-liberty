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

import com.ibm.ws.ui.internal.v1.IFeatureToolService;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 *
 */
public class FeatureUtilsTest {
    private final Mockery mock = new JUnit4Mockery();;
    private final RESTRequest restRequest = mock.mock(RESTRequest.class);
    private final RESTResponse restResponse = mock.mock(RESTResponse.class);
    private final IFeatureToolService mockFeatureService = mock.mock(IFeatureToolService.class);

    private FeatureUtils handler;

    @Before
    public void setUp() {
        handler = new FeatureUtils(mockFeatureService);
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
                     "/adminCenter/v1/utils/feature", handler.baseURL());
        assertTrue("The handler should expect to handle children",
                   handler.hasChildren());
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
                will(returnValue("/adminCenter/v1/utils/feature"));
            }
        });

        Map<String, String> obj = handler.getBase(restRequest, restResponse);

        assertTrue("FAIL: the feature utils JSON object did not contain the expected base response",
                   obj.containsKey("{featureName}"));
        assertEquals("FAIL: the feature utils base response URL was incorrect",
                     "/adminCenter/v1/utils/feature/{featureName}",
                     obj.get("{featureName}"));
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
                will(returnValue("/adminCenter/v1/utils/feature/"));
            }
        });

        Map<String, String> obj = handler.getBase(restRequest, restResponse);

        assertTrue("FAIL: the feature utils JSON object did not contain the expected base response",
                   obj.containsKey("{featureName}"));
        assertEquals("FAIL: the feature utils base response URL was incorrect",
                     "/adminCenter/v1/utils/feature/{featureName}",
                     obj.get("{featureName}"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.utils.URLUtils#getChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void getChild_provisionedFeature() throws Exception {
        final String provisionedFeature = "provisioned-1.0";
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockFeatureService).isFeatureProvisioned(provisionedFeature);
                will(returnValue(true));
            }
        });

        Map<String, Boolean> resp = handler.getChild(restRequest, restResponse, provisionedFeature);
        assertTrue("FAIL: response did not contain the provisioned key",
                   resp.containsKey("provisioned"));
        assertTrue("FAIL: the response value for 'provisioned' should have been true",
                   resp.get("provisioned"));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.rest.v1.utils.URLUtils#getChild(RESTRequest, RESTResponse, String)}.
     */
    @Test
    public void getChild_feature_not_provisioned() throws Exception {
        final String notProvisionedFeature = "notProvisioned-1.0";
        mock.checking(new Expectations() {
            {
                one(restRequest).getMethod();
                will(returnValue("GET"));
                one(restRequest).isUserInRole("Administrator");
                will(returnValue(true));

                one(mockFeatureService).isFeatureProvisioned(notProvisionedFeature);
                will(returnValue(false));
            }
        });

        Map<String, Boolean> resp = handler.getChild(restRequest, restResponse, notProvisionedFeature);
        assertTrue("FAIL: response did not contain the provisioned key",
                   resp.containsKey("provisioned"));
        assertFalse("FAIL: the response value for 'provisioned' should have been false",
                    resp.get("provisioned"));
    }

}
