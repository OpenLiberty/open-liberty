/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
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
package com.ibm.ws.ui.fat.rest;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.ui.fat.APIConstants;
import com.ibm.ws.ui.fat.FATSuite;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;

/**
 * Validate the API root resources exposes all of the supported versions of
 * the API. This resource only supports GET requests and requires that the
 * user be an authenticated and authorized user.
 * <p>
 * This is not an exhaustive set of tests as we can rely on unit tests in
 * many cases. The amount of variants for accessing this API is limited.
 * <p>
 * Example JSON payload:
 * <pre>
 * GET https://host:port/ibm/api/adminCenter
 * {
 * "v1": "https://host:port/ibm/api/adminCenter/v1"
 * }
 * </pre>
 */
@RunWith(FATRunner.class)
public class APITest extends CommonRESTTest implements APIConstants {
    private static final Class<?> c = APITest.class;

    public APITest() {
        super(c);
        url = API_ROOT;
    }

    /**
     * Access the API root page with a GET request.
     * The API root page only supports GET requests, and returns a JSON
     * object which contains links to the available API versions.
     * 
     * Test flow:
     * 1. Request GET the API root page
     * 2. Response HTTP Status Code: 200
     * 3. Response Content-Type: JSON
     * 4. Response body contains JSON object
     * 5. The JSON object contains the available versions
     */
    @Test
    public void apiRootGETContent() throws Exception {
        response = get(url, adminUser, adminPassword, 200);
        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        assertSize(response, 1);
        assertContains(response, "v1", getHTTPSHostAndPort(FATSuite.server) + url + "/v1");
    }

    /**
     * Access the API root page with a POST request.
     * The API root page does not support POST requests.
     * 
     * Test flow:
     * 1. Request POST the API root page
     * 2. Response HTTP Status Code: 405
     */
    @Test
    public void apiRootUnsupportedPOST() throws Exception {
        response = post(url, adminUser, adminPassword, null, 405);
    }

    /**
     * Access the API root page with a PUT request.
     * The API root page does not support PUT requests.
     * 
     * Test flow:
     * 1. Request PUT the API root page
     * 2. Response HTTP Status Code: 405
     */
    @Test
    public void apiRootUnsupportedPUT() throws Exception {
        response = put(url, adminUser, adminPassword, null, 405);
    }

    /**
     * Access the API root page with a DELETE request.
     * The API root page does not support DELETE requests.
     * 
     * Test flow:
     * 1. Request DELETE the API root page
     * 2. Response HTTP Status Code: 405
     */
    @Test
    public void apiRootUnsupportedDELETE() throws Exception {
        response = delete(url, adminUser, adminPassword, 405);
    }

    /**
     * Access the API root page without being authenticated.
     * The API root page requires the user be authenticated.
     * 
     * Test flow:
     * 1. GET the API root page with no credentials
     * 2. Response HTTP Status Code: 401
     */
    @Test
    public void apiRootNoCredentials() throws Exception {
        response = get(url, null, null, 401);
    }

    /**
     * Access the API root page with a non-Administrator user.
     * The API root page requires the user be an Administrator.
     * 
     * Test flow:
     * 1. GET the API root page with non-Administrator credentials
     * 2. Response HTTP Status Code: 403
     */
    @Test
    public void apiRootNonAdminCredentials() throws Exception {
        response = get(url, nonadminUser, nonadminPassword, 403);
    }

}
