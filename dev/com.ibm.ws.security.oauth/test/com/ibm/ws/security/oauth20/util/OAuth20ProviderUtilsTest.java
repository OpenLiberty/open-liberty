/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.security.oauth20.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import test.common.SharedOutputManager;

/**
 * Unit tests for OAuth20ProviderUtils.handleOAuthChallenge method
 * Tests RFC 9728 resource_metadata parameter support in WWW-Authenticate header
 */
public class OAuth20ProviderUtilsTest {

    private static SharedOutputManager outputMgr;
    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private HttpServletResponse mockResponse;
    private ProviderAuthenticationResult mockAuthResult;
    private StringWriter responseWriter;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Before
    public void setUp() throws Exception {
        mockResponse = mockery.mock(HttpServletResponse.class);
        mockAuthResult = mockery.mock(ProviderAuthenticationResult.class);
        responseWriter = new StringWriter();
    }

    @After
    public void tearDown() {
        mockery.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    /**
     * Test handleOAuthChallenge without resource_metadata URL (backward compatibility)
     */
    @Test
    public void testHandleOAuthChallenge_WithoutResourceMetadata() throws IOException {
        final String errorDescription = "Test error description";
        final String expectedHeader = "Bearer realm=\"oauth\"";
        final PrintWriter writer = new PrintWriter(responseWriter);

        mockery.checking(new Expectations() {
            {
                one(mockResponse).isCommitted();
                will(returnValue(false));
                one(mockResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                one(mockResponse).getHeader(OAuth20ProviderUtils.AUTHENTICATE_HDR);
                will(returnValue(null));
                one(mockResponse).setHeader(OAuth20ProviderUtils.AUTHENTICATE_HDR, expectedHeader);
                one(mockResponse).setHeader(with(any(String.class)), with(any(String.class)));
                one(mockResponse).getWriter();
                will(returnValue(writer));
                one(mockResponse).getHeader(OAuth20ProviderUtils.AUTHENTICATE_HDR);
                will(returnValue(expectedHeader));
            }
        });

        OAuth20ProviderUtils.handleOAuthChallenge(mockResponse, mockAuthResult, errorDescription);

        String responseBody = responseWriter.toString();
        assertNotNull("Response should not be null", responseBody);
        assertTrue("Response should contain error code", responseBody.contains("401"));
        assertTrue("Response should contain error description", responseBody.contains(errorDescription));

        // Verify WWW-Authenticate header is set correctly without resource_metadata
        String actualHeader = mockResponse.getHeader(OAuth20ProviderUtils.AUTHENTICATE_HDR);
        assertNotNull("WWW-Authenticate header should be set", actualHeader);
        assertEquals("WWW-Authenticate header should only contain Bearer realm",
                expectedHeader, actualHeader);
    }

    /**
     * Test handleOAuthChallenge with resource_metadata URL (RFC 9728)
     */
    @Test
    public void testHandleOAuthChallenge_WithResourceMetadata() throws IOException {
        final String errorDescription = "Test error description";
        final String resourceMetadataUrl = "https://example.com/.well-known/oauth-protected-resource";
        final String expectedHeader = "Bearer realm=\"oauth\", resource_metadata=\"" + resourceMetadataUrl + "\"";
        final PrintWriter writer = new PrintWriter(responseWriter);

        mockery.checking(new Expectations() {
            {
                one(mockResponse).isCommitted();
                will(returnValue(false));
                one(mockResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                one(mockResponse).getHeader(OAuth20ProviderUtils.AUTHENTICATE_HDR);
                will(returnValue(null));
                one(mockResponse).setHeader(OAuth20ProviderUtils.AUTHENTICATE_HDR, expectedHeader);
                one(mockResponse).setHeader(with(any(String.class)), with(any(String.class)));
                one(mockResponse).getWriter();
                will(returnValue(writer));
                one(mockResponse).getHeader(OAuth20ProviderUtils.AUTHENTICATE_HDR);
                will(returnValue(expectedHeader));
            }
        });

        OAuth20ProviderUtils.handleOAuthChallenge(mockResponse, mockAuthResult, errorDescription, resourceMetadataUrl);

        String responseBody = responseWriter.toString();
        assertNotNull("Response should not be null", responseBody);
        assertTrue("Response should contain error code", responseBody.contains("401"));
        assertTrue("Response should contain error description", responseBody.contains(errorDescription));

        // Verify WWW-Authenticate header contains resource_metadata by retrieving it
        String actualHeader = mockResponse.getHeader(OAuth20ProviderUtils.AUTHENTICATE_HDR);
        assertNotNull("WWW-Authenticate header should be set", actualHeader);
        assertEquals("WWW-Authenticate header should contain Bearer realm and resource_metadata with comma separation",
                expectedHeader, actualHeader);
    }

    /**
     * Test handleOAuthChallenge with null resource_metadata URL
     */
    @Test
    public void testHandleOAuthChallenge_WithNullResourceMetadata() throws IOException {
        final String errorDescription = "Test error description";
        final String expectedHeader = "Bearer realm=\"oauth\"";
        final PrintWriter writer = new PrintWriter(responseWriter);

        mockery.checking(new Expectations() {
            {
                one(mockResponse).isCommitted();
                will(returnValue(false));
                one(mockResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                one(mockResponse).getHeader(OAuth20ProviderUtils.AUTHENTICATE_HDR);
                will(returnValue(null));
                one(mockResponse).setHeader(OAuth20ProviderUtils.AUTHENTICATE_HDR, expectedHeader);
                one(mockResponse).setHeader(with(any(String.class)), with(any(String.class)));
                one(mockResponse).getWriter();
                will(returnValue(writer));
                one(mockResponse).getHeader(OAuth20ProviderUtils.AUTHENTICATE_HDR);
                will(returnValue(expectedHeader));
            }
        });

        OAuth20ProviderUtils.handleOAuthChallenge(mockResponse, mockAuthResult, errorDescription, null);

        // Verify WWW-Authenticate header does NOT contain resource_metadata when null
        String actualHeader = mockResponse.getHeader(OAuth20ProviderUtils.AUTHENTICATE_HDR);
        assertNotNull("WWW-Authenticate header should be set", actualHeader);
        assertEquals("WWW-Authenticate header should only contain Bearer realm when resource_metadata is null",
                expectedHeader, actualHeader);
        assertTrue("WWW-Authenticate header should NOT contain resource_metadata when null",
                !actualHeader.contains("resource_metadata"));
    }

    /**
     * Test handleOAuthChallenge when WWW-Authenticate is already set (e.g. by oidcClientRequest.setWWWAuthenticate())
     * and a resourceMetadataUrl is provided — resource_metadata must be appended to the existing header value.
     */
    @Test
    public void testHandleOAuthChallenge_HeaderAlreadySet_AppendsResourceMetadata() throws IOException {
        final String errorDescription = "Test error description";
        final String resourceMetadataUrl = "https://example.com/.well-known/oauth-protected-resource";
        final String existingHeader = "Bearer realm=\"oauth\", error=\"invalid_token\", error_description=\"Check access token\"";
        final String expectedHeader = existingHeader + ", resource_metadata=\"" + resourceMetadataUrl + "\"";
        final PrintWriter writer = new PrintWriter(responseWriter);

        mockery.checking(new Expectations() {
            {
                one(mockResponse).isCommitted();
                will(returnValue(false));
                one(mockResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                one(mockResponse).getHeader(OAuth20ProviderUtils.AUTHENTICATE_HDR);
                will(returnValue(existingHeader));
                one(mockResponse).setHeader(OAuth20ProviderUtils.AUTHENTICATE_HDR, expectedHeader);
                one(mockResponse).setHeader(with(any(String.class)), with(any(String.class)));
                one(mockResponse).getWriter();
                will(returnValue(writer));
                one(mockResponse).getHeader(OAuth20ProviderUtils.AUTHENTICATE_HDR);
                will(returnValue(expectedHeader));
            }
        });

        OAuth20ProviderUtils.handleOAuthChallenge(mockResponse, mockAuthResult, errorDescription, resourceMetadataUrl);

        String actualHeader = mockResponse.getHeader(OAuth20ProviderUtils.AUTHENTICATE_HDR);
        assertNotNull("WWW-Authenticate header should be set", actualHeader);
        assertTrue("resource_metadata should be appended to existing WWW-Authenticate header",
                actualHeader.contains("resource_metadata=\"" + resourceMetadataUrl + "\""));
        assertTrue("Original Bearer realm content should still be present",
                actualHeader.contains("Bearer realm=\"oauth\""));
    }

    /**
     * Test handleOAuthChallenge when WWW-Authenticate is already set and resourceMetadataUrl is null —
     * the header must NOT be redundantly rewritten (no setHeader call for WWW-Authenticate).
     */
    @Test
    public void testHandleOAuthChallenge_HeaderAlreadySet_NullResourceMetadata_NoRedundantWrite() throws IOException {
        final String errorDescription = "Test error description";
        final String existingHeader = "Bearer realm=\"oauth\", error=\"invalid_token\", error_description=\"Check access token\"";
        final PrintWriter writer = new PrintWriter(responseWriter);

        mockery.checking(new Expectations() {
            {
                one(mockResponse).isCommitted();
                will(returnValue(false));
                one(mockResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                one(mockResponse).getHeader(OAuth20ProviderUtils.AUTHENTICATE_HDR);
                will(returnValue(existingHeader));
                // setHeader for AUTHENTICATE_HDR must NOT be called — no redundant write
                one(mockResponse).setHeader(with(any(String.class)), with(any(String.class)));
                one(mockResponse).getWriter();
                will(returnValue(writer));
            }
        });

        OAuth20ProviderUtils.handleOAuthChallenge(mockResponse, mockAuthResult, errorDescription, null);
    }

    /**
     * Test handleOAuthChallenge with empty resource_metadata URL
     */
    @Test
    public void testHandleOAuthChallenge_WithEmptyResourceMetadata() throws IOException {
        final String errorDescription = "Test error description";
        final String expectedHeader = "Bearer realm=\"oauth\"";
        final PrintWriter writer = new PrintWriter(responseWriter);

        mockery.checking(new Expectations() {
            {
                one(mockResponse).isCommitted();
                will(returnValue(false));
                one(mockResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                one(mockResponse).getHeader(OAuth20ProviderUtils.AUTHENTICATE_HDR);
                will(returnValue(null));
                one(mockResponse).setHeader(OAuth20ProviderUtils.AUTHENTICATE_HDR, expectedHeader);
                one(mockResponse).setHeader(with(any(String.class)), with(any(String.class)));
                one(mockResponse).getWriter();
                will(returnValue(writer));
                one(mockResponse).getHeader(OAuth20ProviderUtils.AUTHENTICATE_HDR);
                will(returnValue(expectedHeader));
            }
        });

        OAuth20ProviderUtils.handleOAuthChallenge(mockResponse, mockAuthResult, errorDescription, "");

        // Verify WWW-Authenticate header does NOT contain resource_metadata when empty
        String actualHeader = mockResponse.getHeader(OAuth20ProviderUtils.AUTHENTICATE_HDR);
        assertNotNull("WWW-Authenticate header should be set", actualHeader);
        assertEquals("WWW-Authenticate header should only contain Bearer realm when resource_metadata is empty",
                expectedHeader, actualHeader);
    }

    /**
     * Test handleOAuthChallenge when response is already committed
     */
    @Test
    public void testHandleOAuthChallenge_ResponseCommitted() throws IOException {
        final String errorDescription = "Test error description";
        final String resourceMetadataUrl = "https://example.com/.well-known/oauth-protected-resource";

        mockery.checking(new Expectations() {
            {
                one(mockResponse).isCommitted();
                will(returnValue(true));
                // No other methods should be called when response is committed
            }
        });

        OAuth20ProviderUtils.handleOAuthChallenge(mockResponse, mockAuthResult, errorDescription, resourceMetadataUrl);

        // If response is committed, nothing should be written
        assertEquals("Response writer should be empty when response is committed", "", responseWriter.toString());
    }
}
