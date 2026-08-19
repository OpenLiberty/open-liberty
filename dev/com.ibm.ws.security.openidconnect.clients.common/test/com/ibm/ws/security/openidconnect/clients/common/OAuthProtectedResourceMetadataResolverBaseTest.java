/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Test;

import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.openidconnect.clients.common.OAuthProtectedResourceMetadataResolverBase.ProtectedResourceRequestWrapper;

public class OAuthProtectedResourceMetadataResolverBaseTest {

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final HttpServletRequest mockRequest = mock.mock(HttpServletRequest.class, "request");

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    // ---- ProtectedResourceRequestWrapper ------------------------------------

    @Test
    public void wrapperGetRequestURIReturnsProtectedResourcePath() {
        ProtectedResourceRequestWrapper wrapper = new ProtectedResourceRequestWrapper(mockRequest, "/myApp/protected");

        assertEquals("/myApp/protected", wrapper.getRequestURI());
    }

    @Test
    public void wrapperGetRequestURLStripsWellKnownPrefix() {
        mock.checking(new Expectations() {
            {
                oneOf(mockRequest).getRequestURL();
                will(returnValue(new StringBuffer("https://localhost:9443/.well-known/oauth-protected-resource/myApp/protected")));
            }
        });

        ProtectedResourceRequestWrapper wrapper = new ProtectedResourceRequestWrapper(mockRequest, "/myApp/protected");

        assertEquals("https://localhost:9443/myApp/protected", wrapper.getRequestURL().toString());
    }

    @Test
    public void wrapperGetRequestURLReturnsOriginalWhenNoWellKnownPrefix() {
        mock.checking(new Expectations() {
            {
                oneOf(mockRequest).getRequestURL();
                will(returnValue(new StringBuffer("https://localhost:9443/myApp/protected")));
            }
        });

        ProtectedResourceRequestWrapper wrapper = new ProtectedResourceRequestWrapper(mockRequest, "/myApp/protected");

        assertEquals("https://localhost:9443/myApp/protected", wrapper.getRequestURL().toString());
    }

    @Test
    public void wrapperGetServletPathReturnsProtectedResourcePath() {
        ProtectedResourceRequestWrapper wrapper = new ProtectedResourceRequestWrapper(mockRequest, "/myApp/protected");

        assertEquals("/myApp/protected", wrapper.getServletPath());
    }

    @Test
    public void wrapperGetContextPathReturnsEmpty() {
        ProtectedResourceRequestWrapper wrapper = new ProtectedResourceRequestWrapper(mockRequest, "/myApp/protected");

        assertEquals("", wrapper.getContextPath());
    }

    @Test
    public void wrapperGetPathInfoReturnsNull() {
        ProtectedResourceRequestWrapper wrapper = new ProtectedResourceRequestWrapper(mockRequest, "/myApp/protected");

        assertNull(wrapper.getPathInfo());
    }

    // ---- createSignedMetadata null/blank guard ------------------------------

    @Test
    public void createSignedMetadataReturnsNullWhenBuilderIdIsNull() {
        TestResolver resolver = new TestResolver();

        String result = resolver.createSignedMetadata(null, null);

        assertNull("Expected null when jwtBuilderRef is null", result);
    }

    @Test
    public void createSignedMetadataReturnsNullWhenBuilderIdIsBlank() {
        TestResolver resolver = new TestResolver();
        resolver.jwtBuilderId = "   ";

        String result = resolver.createSignedMetadata("  ", null);

        assertNull("Expected null when jwtBuilderRef is blank", result);
    }

    // ---- createMetadataJson (existing) --------------------------------------

    @Test
    public void createMetadataJsonUsesIssuerIdentifierWhenPresent() {
        TestResolver resolver = new TestResolver();
        resolver.issuerIdentifier = "https://issuer.example.com";

        String metadata = resolver.createMetadataJson(null, "https://localhost:9443/mcp");

        assertEquals("{\"resource\":\"https:\\/\\/localhost:9443\\/mcp\",\"authorization_servers\":[\"https:\\/\\/issuer.example.com\"]}", metadata);
    }

    @Test
    public void createMetadataJsonOmitsAuthorizationServersWhenIssuerIsMissing() throws Exception {
        TestResolver resolver = new TestResolver();

        String metadataJson = resolver.createMetadataJson(null, "https://localhost:9443/myApp/protected");

        JSONObject metadata = JSONObject.parse(metadataJson);

        assertEquals("https://localhost:9443/myApp/protected", metadata.get("resource"));
        assertFalse(metadata.containsKey("authorization_servers"));
    }

    // ---- createMetadataJson with jwtBuilderRef ------------------------------

    @Test
    public void createMetadataJsonIncludesSignedMetadataWhenJwtBuilderRefIsConfigured() throws Exception {
        TestResolver resolver = new TestResolver();
        resolver.jwtBuilderId = "myBuilder";
        resolver.mockSignature = "test.signed.jwt";

        String metadataJson = resolver.createMetadataJson(null, "https://localhost:9443/myApp/protected");

        JSONObject metadata = JSONObject.parse(metadataJson);
        assertEquals("test.signed.jwt", metadata.get("signed_metadata"));
    }

    @Test
    public void createMetadataJsonOmitsSignedMetadataWhenJwtBuilderRefIsNull() throws Exception {
        TestResolver resolver = new TestResolver();
        resolver.mockSignature = "test.signed.jwt";

        String metadataJson = resolver.createMetadataJson(null, "https://localhost:9443/myApp/protected");

        JSONObject metadata = JSONObject.parse(metadataJson);
        assertFalse("signed_metadata should be absent when jwtBuilderRef is null", metadata.containsKey("signed_metadata"));
    }

    private static class TestResolver extends OAuthProtectedResourceMetadataResolverBase<Void> {

        public String issuerIdentifier;
        public String jwtBuilderId;
        public String mockSignature;

        @Override
        protected List<String> getAdvertisedScopes(Void config) {
            return null;
        }

        @Override
        protected String getJwtBuilderId(Void config) {
            return jwtBuilderId;
        }

        @Override
        protected String getConfigId(Void config) {
            return "testClient";
        }

        @Override
        protected String getAuthorizationServer(Void config) {
            return issuerIdentifier;
        }

        @Override
        public String resolveMetadataJson(HttpServletRequest request, String protectedResourcePath, String absoluteResourceUrl) {
            return null;
        }

        @Override
        protected String createSignedMetadata(String jwtBuilderRef, JSONObject metadata) {
            if (mockSignature != null) {
                return mockSignature;
            } else {
                return super.createSignedMetadata(jwtBuilderRef, metadata);
            }
        }
    }

}
