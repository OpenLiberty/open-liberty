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
package com.ibm.ws.ui.fat.rest.v1;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.ui.fat.APIConstants;
import com.ibm.ws.ui.fat.FATSuite;
import com.ibm.ws.ui.fat.rest.CommonRESTTest;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Validate the version 1 API root resources exposes all of the supported
 * resources of the version API. This resource only supports GET requests
 * and requires that the user be an authenticated and authorized user.
 * <p>
 * This is not an exhaustive set of tests as we can rely on unit tests in
 * many cases. The amount of variants for accessing this API is limited.
 * <p>
 * Example JSON payload:
 * <pre>
 * GET https://host:port/ibm/api/adminCenter/v1
 * {
 * "catalog": "https://host:port/ibm/api/adminCenter/v1/catalog"
 * }
 * </pre>
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class APIv1Test extends CommonRESTTest implements APIConstants {
    private static final Class<?> c = APIv1Test.class;

    public APIv1Test() {
        super(c);
        url = API_V1_ROOT;
    }

    /**
     * Access the version 1 API root page with a GET request.
     * The version 1 API root page only supports GET requests, and returns a JSON
     * object which contains links to the available version 1 API resources.
     * 
     * Test flow:
     * 1. Request GET the version 1 API root page
     * 2. Response HTTP Status Code: 200
     * 3. The JSON object contains the available resources
     */
    @Test
    public void v1RootGETContent() throws Exception {
        response = get(url, adminUser, adminPassword, 200);

        Log.info(c, method.getMethodName(), "Got JSON object: " + response);

        assertSize(response, 4);
        assertContains(response, "catalog", getHTTPSHostAndPort(FATSuite.server) + url + "/catalog");
        assertContains(response, "toolbox", getHTTPSHostAndPort(FATSuite.server) + url + "/toolbox");
        assertContains(response, "utils", getHTTPSHostAndPort(FATSuite.server) + url + "/utils");
        assertContains(response, "icons", getHTTPSHostAndPort(FATSuite.server) + url + "/icons");
    }

    /**
     * Access the version 1 API root page with a POST request.
     * The version 1 API root page does not support POST requests.
     * 
     * Test flow:
     * 1. Request POST the version 1 API root page
     * 2. Response HTTP Status Code: 405
     */
    @Test
    public void v1RootUnsupportedPOST() throws Exception {
        response = post(url, adminUser, adminPassword, null, 405);
    }

    /**
     * Access the version 1 API root page with a PUT request.
     * The version 1 API root page does not support PUT requests.
     * 
     * Test flow:
     * 1. Request PUT the version 1 API root page
     * 2. Response HTTP Status Code: 405
     */
    @Test
    public void v1RootUnsupportedPUT() throws Exception {
        response = put(url, adminUser, adminPassword, null, 405);
    }

    /**
     * Access the version 1 API root page with a DELETE request.
     * The version 1 API root page does not support DELETE requests.
     * 
     * Test flow:
     * 1. Request DELETE the version 1 API root page
     * 2. Response HTTP Status Code: 405
     */
    @Test
    public void v1RootUnsupportedDELETE() throws Exception {
        response = delete(url, adminUser, adminPassword, 405);
    }

    /**
     * Access the version 1 API root page without being authenticated.
     * The version 1 API root page requires the user be authenticated.
     * 
     * Test flow:
     * 1. GET the version 1 API root page with no credentials
     * 2. Response HTTP Status Code: 401
     */
    @Test
    public void v1RootNoCredentials() throws Exception {
        response = get(url, null, null, 401);
    }

    /**
     * Access the version 1 API root page with a non-Administrator user.
     * The version 1 API root page requires the user be an Administrator.
     * 
     * Test flow:
     * 1. GET the version 1 API root page with non-Administrator credentials
     * 2. Response HTTP Status Code: 403
     */
    @Test
    public void v1RootNonAdminCredentials() throws Exception {
        response = get(url, nonadminUser, nonadminPassword, 403);
    }

    /**
     * Request coffee from the version 1 API.
     * Unfortunately, our resource is a teapot, not a coffee machine...
     * <p>
     * See https://en.wikipedia.org/wiki/List_of_HTTP_status_codes
     * <p>
     * Details from RFC 2324:
     * <p>
     * 2.3.2 418 I'm a teapot <br>
     * Any attempt to brew coffee with a teapot should result in the error
     * code "418 I'm a teapot". The resulting entity body MAY be short and
     * stout..
     * </p>
     */
    @Test
    public void v1RootCoffeeRequest() throws Exception {
        String response = getWithStringResponse(url + "/coffee", adminUser, adminPassword, 418);
        Log.info(c, method.getMethodName(), "response: " + response);

        assertEquals("FAIL: The resource did not response stating it was a teapot",
                     "I'm a teapot!", response);
    }

}
