/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.server.rest.helpers;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.helper.RESTHandlerJsonException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RESTHelperTest {

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @Test
    public void testRepairSlashes() {

        // If the object name doesn't contain slashes, repairSlashes should be a no-op, regardless of the URI
        assertEquals("anObjectName", RESTHelper.repairSlashes("anObjectName", new RESTRequestTestImpl("aURI")));
         
        // Noddy cases
        assertEquals("anObject/Name", RESTHelper.repairSlashes("anObject/Name", new RESTRequestTestImpl("anObject/Name")));
        assertEquals("anObject//Name", RESTHelper.repairSlashes("anObject/Name", new RESTRequestTestImpl("anObject//Name")));
        assertEquals("anObject///Name", RESTHelper.repairSlashes("anObject/Name", new RESTRequestTestImpl("anObject///Name")));

        // Tests with more realistic URIs
        assertEquals("com.foo.mybean:foo=bar", RESTHelper.repairSlashes("com.foo.mybean:foo=bar", new RESTRequestTestImpl(" https://9.20.198.170:9443/IBMJMXConnectorREST/mbeans/com.foo.mybean:foo=bar")));
        assertEquals("com.foo.mybean/with/slash:foo=bar", RESTHelper.repairSlashes("com.foo.mybean/with/slash:foo=bar", new RESTRequestTestImpl(" https://9.20.198.170:9443/IBMJMXConnectorREST/mbeans/com.foo.mybean/with/slash:foo=bar")));
        assertEquals("com.foo.mybean/with//slash:foo=bar", RESTHelper.repairSlashes("com.foo.mybean/with/slash:foo=bar", new RESTRequestTestImpl(" https://9.20.198.170:9443/IBMJMXConnectorREST/mbeans/com.foo.mybean/with//slash:foo=bar")));
        assertEquals("com.foo.mybean/with/slash:foo=bar", RESTHelper.repairSlashes("com.foo.mybean/with/slash:foo=bar", new RESTRequestTestImpl(" https://9.20.198.170:9443/IBMJMXConnectorREST/mbeans/com.foo.mybean%2Fwith%2Fslash%3Afoo%3Dbar")));
        assertEquals("com.foo.mybean/with//slash:foo=bar", RESTHelper.repairSlashes("com.foo.mybean/with/slash:foo=bar", new RESTRequestTestImpl(" https://9.20.198.170:9443/IBMJMXConnectorREST/mbeans/com.foo.mybean%2Fwith%2F%2Fslash%3Afoo%3Dbar")));


        // test the non servlet 6 case, i.e. where there are repeated slashes and they are not collapsed
        assertEquals("com.foo.mybean/with//slash:foo=bar", RESTHelper.repairSlashes("com.foo.mybean/with//slash:foo=bar", new RESTRequestTestImpl(" https://9.20.198.170:9443/IBMJMXConnectorREST/mbeans/com.foo.mybean/with//slash:foo=bar")));
        assertEquals("com.foo.mybean/with//slash:foo=bar", RESTHelper.repairSlashes("com.foo.mybean/with//slash:foo=bar", new RESTRequestTestImpl(" https://9.20.198.170:9443/IBMJMXConnectorREST/mbeans/com.foo.mybean%2Fwith%2F%2Fslash%3Afoo%3Dbar")));


        // test the case with more path segments after the interesting one
        assertEquals("com.foo.mybean/with/slash:foo=bar", RESTHelper.repairSlashes("com.foo.mybean/with/slash:foo=bar", new RESTRequestTestImpl(" https://9.20.198.170:9443/IBMJMXConnectorREST/mbeans/com.foo.mybean/with/slash:foo=bar/seg1/seg2")));
        assertEquals("com.foo.mybean/with//slash:foo=bar", RESTHelper.repairSlashes("com.foo.mybean/with/slash:foo=bar", new RESTRequestTestImpl(" https://9.20.198.170:9443/IBMJMXConnectorREST/mbeans/com.foo.mybean/with//slash:foo=bar/seg1/seg2")));
        assertEquals("com.foo.mybean/with//slash:foo=bar", RESTHelper.repairSlashes("com.foo.mybean/with/slash:foo=bar", new RESTRequestTestImpl(" https://9.20.198.170:9443/IBMJMXConnectorREST/mbeans/com.foo.mybean%2Fwith%2F%2Fslash%3Afoo%3Dbar/seg1/seg2")));

        // Tests with more thatn 2 repeated slashes
        assertEquals("com.foo.mybean/with///slash:foo=bar", RESTHelper.repairSlashes("com.foo.mybean/with/slash:foo=bar", new RESTRequestTestImpl(" https://9.20.198.170:9443/IBMJMXConnectorREST/mbeans/com.foo.mybean/with///slash:foo=bar")));
        assertEquals("com.foo.mybean//with///slash:foo=bar", RESTHelper.repairSlashes("com.foo.mybean/with/slash:foo=bar", new RESTRequestTestImpl(" https://9.20.198.170:9443/IBMJMXConnectorREST/mbeans/com.foo.mybean%2F%2Fwith%2F%2F%2Fslash%3Afoo%3Dbar")));

 
        // Tests for fail cases.
        // There is only one case, which is where the object name can't be found in the request URI. It
        // should never happen
        try {
            String newName = RESTHelper.repairSlashes("an/Object/Name", new RESTRequestTestImpl("aURI"));
            Assert.fail("unexpected name found: " + newName);
        } catch (RESTHandlerJsonException e) {
            // expected do nothing
        }
    }


    static class RESTRequestTestImpl implements RESTRequest {

        private String uri;

        public RESTRequestTestImpl(String uri) {
            this.uri = uri;
        }
        public Reader getInput() throws IOException {
            return null;
	}

        public InputStream getInputStream() throws IOException {
            return null;
	}

        public String getHeader(String key) {
            return null;
	}

        public String getMethod() {
            return null;
	}

        public String getCompleteURL() {
            return null;
	}

        public String getURL() {
            return null;
	}

        public String getURI() {
            return uri;
	}

        public String getContextPath() {
            return null;
	}

        public String getPath() {
            return null;
	}

        public String getQueryString() {
            return null;
	}

        public String getParameter(String name) {
            return null;
	}

        public String[] getParameterValues(String name) {
            return null;
	}

        public Map<String, String[]> getParameterMap() {
            return null;
	}

        public Principal getUserPrincipal() {
            return null;
	}

        public boolean isUserInRole(String role) {
            return false;
	}

        public String getPathVariable(String variable) {
            return null;
	}

        public Locale getLocale() {
            return null;
	}

        public Enumeration<Locale> getLocales() {
            return null;
	}

        public String getRemoteAddr() {
            return null;
	}

        public String getRemoteHost() {
            return null;
	}

        public int getRemotePort() {
            return 0;
	}

        public InputStream getPart(String partName) throws IOException {
            return null;
	}

        public boolean isMultiPartRequest() {
            return false;
	}

        public String getContentType() {
            return null;
	}

        public String getSessionId() {
            return null;
	}

    }


}

