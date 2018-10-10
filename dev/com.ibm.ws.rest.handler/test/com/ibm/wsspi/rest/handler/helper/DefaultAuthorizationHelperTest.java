/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.rest.handler.helper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 *
 */
public class DefaultAuthorizationHelperTest {

    /**
     * Test method for
     * {@link com.ibm.wsspi.rest.handler.helper.DefaultAuthorizationHelper#checkAdministratorRole(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void get_with_Administrator_is_authorized() throws Exception {
        DefaultAuthorizationHelper authz = new DefaultAuthorizationHelper();
        RESTRequest request = new MockRESTRequest("GET", "Administrator");
        RESTResponse response = new MockRESTResponse();
        assertTrue("GET method should be authorized when the user is in the Administrator role",
                   authz.checkAdministratorRole(request, response));
    }

    /**
     * Test method for
     * {@link com.ibm.wsspi.rest.handler.helper.DefaultAuthorizationHelper#checkAdministratorRole(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void any_method_with_Administrator_is_authorized() throws Exception {
        DefaultAuthorizationHelper authz = new DefaultAuthorizationHelper();
        RESTRequest request = new MockRESTRequest("POST", "Administrator");
        RESTResponse response = new MockRESTResponse();
        assertTrue("Any HTTP method should be authorized when the user is in the Administrator role",
                   authz.checkAdministratorRole(request, response));
    }

    /**
     * Test method for
     * {@link com.ibm.wsspi.rest.handler.helper.DefaultAuthorizationHelper#checkAdministratorRole(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void get_with_Viewer_is_authorized() throws Exception {
        DefaultAuthorizationHelper authz = new DefaultAuthorizationHelper();
        RESTRequest request = new MockRESTRequest("GET", "Viewer");
        RESTResponse response = new MockRESTResponse();
        assertTrue("GET method should be authorized when the user is in the Viewer role",
                   authz.checkAdministratorRole(request, response));
    }

    /**
     * Test method for
     * {@link com.ibm.wsspi.rest.handler.helper.DefaultAuthorizationHelper#checkAdministratorRole(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void post_with_Viewer_is_unauthorized() throws Exception {
        DefaultAuthorizationHelper authz = new DefaultAuthorizationHelper();
        RESTRequest request = new MockRESTRequest("POST", "Viewer");
        RESTResponse response = new MockRESTResponse(403, "Administrator role needed.");
        assertFalse("POST method should be authorized when the user is in the Viewer role",
                    authz.checkAdministratorRole(request, response));
    }

    /**
     * Test method for
     * {@link com.ibm.wsspi.rest.handler.helper.DefaultAuthorizationHelper#checkAdministratorRole(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void get_with_no_role_is_unauthorized() throws Exception {
        DefaultAuthorizationHelper authz = new DefaultAuthorizationHelper();
        RESTRequest request = new MockRESTRequest("GET", "");
        RESTResponse response = new MockRESTResponse(403, "Administrator or Viewer role needed.");
        assertFalse("GET method should not be authorized if the user has no role", authz.checkAdministratorRole(request, response));
    }

    /**
     * Test method for
     * {@link com.ibm.wsspi.rest.handler.helper.DefaultAuthorizationHelper#checkAdministratorRole(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)}.
     */
    @Test
    public void post_with_no_role_is_unauthorized() throws Exception {
        DefaultAuthorizationHelper authz = new DefaultAuthorizationHelper();
        RESTRequest request = new MockRESTRequest("POST", "");
        RESTResponse response = new MockRESTResponse(403, "Administrator role needed.");
        assertFalse("POST method should not be authorized if the user has no role", authz.checkAdministratorRole(request, response));
    }

}
