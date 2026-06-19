/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.ws.security.oauth.protectedresource.metadata.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

/**
 * Common test methods for the OAuth Protected Resource Metadata endpoint.
 * <p>
 * The protected resource metadata endpoint is resource-specific. For example:
 * {@code /.well-known/oauth-protected-resource/myApp/protected}.
 * </p>
 * <p>
 * Expected metadata contains the protected resource identifier in the
 * {@code resource} field and, when known, authorization server identifiers in
 * {@code authorization_servers}.
 * </p>
 */
public class OAuthProtectedResourceMetadataTests extends CommonTest {

	private static final Class<?> thisClass = OAuthProtectedResourceMetadataTests.class;

	private static final String PROTECTED_RESOURCE_METADATA_PATH = "/.well-known/oauth-protected-resource";

	/**
	 * Verifies that the protected resource metadata endpoint returns metadata for a
	 * configured protected resource path.
	 *
	 * @param serverHttpsString     the HTTPS base URL of the protected resource
	 *                              server
	 * @param protectedResourcePath the protected resource path, for example
	 *                              {@code /formlogin/SimpleServlet}
	 * @param testSettings          test settings for the current test
	 * @throws Exception if the request fails unexpectedly
	 */
	protected void testMetadataEndpointAccessible(String serverHttpsString, String protectedResourcePath,
			TestSettings testSettings) throws Exception {
		String testName = "testMetadataEndpointAccessible";
		String metadataUrl = buildMetadataUrl(serverHttpsString, protectedResourcePath);

		Log.info(thisClass, testName, "Testing protected resource metadata endpoint: " + metadataUrl);

		WebResponse response = getResponse(metadataUrl);

		assertEquals("Expected metadata endpoint to return 200", 200, response.getResponseCode());

		String responseBody = response.getText();

		assertTrue("Response did not contain resource field: " + responseBody, responseBody.contains("\"resource\""));
		assertTrue("Response did not contain expected resource URL: " + responseBody,
				responseBody.contains(serverHttpsString + protectedResourcePath));
		assertTrue("Response did not contain authorization_servers field: " + responseBody,
				responseBody.contains("\"authorization_servers\""));
	}

	/**
	 * Verifies that the protected resource metadata response contains the expected
	 * resource URL.
	 *
	 * @param serverHttpsString     the HTTPS base URL of the protected resource
	 *                              server
	 * @param protectedResourcePath the protected resource path
	 * @param testSettings          test settings for the current test
	 * @throws Exception if the request fails unexpectedly
	 */
	protected void testMetadataContainsResource(String serverHttpsString, String protectedResourcePath,
			TestSettings testSettings) throws Exception {
		String testName = "testMetadataContainsResource";
		String metadataUrl = buildMetadataUrl(serverHttpsString, protectedResourcePath);
		String expectedResourceUrl = serverHttpsString + protectedResourcePath;

		Log.info(thisClass, testName, "Testing metadata resource value: " + metadataUrl);

		WebResponse response = getResponse(metadataUrl);

		assertEquals("Expected metadata endpoint to return 200", 200, response.getResponseCode());

		String responseBody = response.getText();

		assertTrue("Response did not contain resource field: " + responseBody, responseBody.contains("\"resource\""));
		assertTrue("Response did not contain expected resource URL: " + responseBody,
				responseBody.contains(expectedResourceUrl));
	}

	/**
	 * Verifies that the protected resource metadata response contains authorization
	 * server metadata when the issuer is known.
	 *
	 * @param serverHttpsString           the HTTPS base URL of the protected
	 *                                    resource server
	 * @param protectedResourcePath       the protected resource path
	 * @param expectedAuthorizationServer expected authorization server identifier
	 * @param testSettings                test settings for the current test
	 * @throws Exception if the request fails unexpectedly
	 */
	protected void testMetadataContainsAuthorizationServer(String serverHttpsString, String protectedResourcePath,
			String expectedAuthorizationServer, TestSettings testSettings) throws Exception {
		String testName = "testMetadataContainsAuthorizationServer";
		String metadataUrl = buildMetadataUrl(serverHttpsString, protectedResourcePath);

		Log.info(thisClass, testName, "Testing metadata authorization server value: " + metadataUrl);

		WebResponse response = getResponse(metadataUrl);

		assertEquals("Expected metadata endpoint to return 200", 200, response.getResponseCode());

		String responseBody = response.getText();

		assertTrue("Response did not contain authorization_servers field: " + responseBody,
				responseBody.contains("\"authorization_servers\""));
		assertTrue("Response did not contain expected authorization server: " + responseBody,
				responseBody.contains(expectedAuthorizationServer));
	}

	/**
	 * Verifies that an unknown protected resource path does not return metadata.
	 *
	 * @param serverHttpsString            the HTTPS base URL of the protected
	 *                                     resource server
	 * @param unknownProtectedResourcePath unknown protected resource path
	 * @param testSettings                 test settings for the current test
	 * @throws Exception if the request fails unexpectedly
	 */
	protected void testMetadataEndpointReturns404ForUnknownResource(String serverHttpsString,
			String unknownProtectedResourcePath, TestSettings testSettings) throws Exception {
		String testName = "testMetadataEndpointReturns404ForUnknownResource";
		String metadataUrl = buildMetadataUrl(serverHttpsString, unknownProtectedResourcePath);

		Log.info(thisClass, testName, "Testing unknown protected resource metadata endpoint: " + metadataUrl);

		WebResponse response = getResponseAllowingErrorStatus(metadataUrl);

		assertEquals("Expected unknown protected resource metadata endpoint to return 404", 404,
				response.getResponseCode());
	}

	/**
	 * Verifies that the base metadata endpoint does not return metadata without a
	 * protected resource path.
	 *
	 * @param serverHttpsString the HTTPS base URL of the protected resource server
	 * @param testSettings      test settings for the current test
	 * @throws Exception if the request fails unexpectedly
	 */
	protected void testMetadataEndpointBasePathReturns404(String serverHttpsString, TestSettings testSettings)
			throws Exception {
		String testName = "testMetadataEndpointBasePathReturns404";
		String metadataUrl = serverHttpsString + PROTECTED_RESOURCE_METADATA_PATH;

		Log.info(thisClass, testName, "Testing base protected resource metadata endpoint: " + metadataUrl);

		WebResponse response = getResponseAllowingErrorStatus(metadataUrl);

		assertEquals("Expected base protected resource metadata endpoint to return 404", 404,
				response.getResponseCode());
	}

	/**
	 * Verifies that the protected resource metadata endpoint returns JSON content.
	 *
	 * @param serverHttpsString     the HTTPS base URL of the protected resource
	 *                              server
	 * @param protectedResourcePath the protected resource path
	 * @param testSettings          test settings for the current test
	 * @throws Exception if the request fails unexpectedly
	 */
	protected void testMetadataEndpointReturnsJsonContentType(String serverHttpsString, String protectedResourcePath,
			TestSettings testSettings) throws Exception {
		String testName = "testMetadataEndpointReturnsJsonContentType";
		String metadataUrl = buildMetadataUrl(serverHttpsString, protectedResourcePath);

		Log.info(thisClass, testName, "Testing metadata content type: " + metadataUrl);

		WebResponse response = getResponse(metadataUrl);

		assertEquals("Expected metadata endpoint to return 200", 200, response.getResponseCode());
		assertTrue("Expected Content-Type to contain application/json but was: " + response.getContentType(),
				response.getContentType().contains("application/json"));
	}

	/**
	 * Builds the protected resource metadata endpoint URL for a protected resource
	 * path.
	 *
	 * @param serverHttpsString     the HTTPS base URL of the protected resource
	 *                              server
	 * @param protectedResourcePath the protected resource path
	 * @return the full protected resource metadata endpoint URL
	 */
	protected String buildMetadataUrl(String serverHttpsString, String protectedResourcePath) {
		return serverHttpsString + PROTECTED_RESOURCE_METADATA_PATH
				+ normalizeProtectedResourcePath(protectedResourcePath);
	}

	private String normalizeProtectedResourcePath(String protectedResourcePath) {
		if (protectedResourcePath == null || protectedResourcePath.isEmpty() || "/".equals(protectedResourcePath)) {
			return "/";
		}

		if (protectedResourcePath.startsWith("/")) {
			return protectedResourcePath;
		}

		return "/" + protectedResourcePath;
	}

	private WebResponse getResponse(String url) throws Exception {
		WebConversation wc = new WebConversation();

		WebRequest request = new GetMethodWebRequest(url);

		return wc.getResponse(request);
	}

	private WebResponse getResponseAllowingErrorStatus(String url) throws Exception {
		WebConversation wc = new WebConversation();
		wc.setExceptionsThrownOnErrorStatus(false);

		WebRequest request = new GetMethodWebRequest(url);

		return wc.getResponse(request);
	}
}