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
package io.openliberty.security.rest.jwt.tai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;

import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.jwt.config.JwtConsumerConfig;
import com.ibm.ws.security.jwt.config.MpConfigProperties;
import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.security.token.AttributeNameConstants;

public class RestJwtTAITest {

    private final Mockery mockery = new JUnit4Mockery();

    private RestJwtTAI tai;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private JwtToken jwtToken;
    private Claims claims;

    @Before
    public void setUp() {
        tai = new RestJwtTAI();
        request = mockery.mock(HttpServletRequest.class);
        response = mockery.mock(HttpServletResponse.class);
        jwtToken = mockery.mock(JwtToken.class);
        claims = mockery.mock(Claims.class);
    }

    /** Builds an unsigned-looking JWT; the TAI only reads the claims in isTargetInterceptor. */
    private static String createToken(String payloadJson) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".signature";
    }

    private void expectAdminEndpointRequestWithBearer(final String token) {
        mockery.checking(new Expectations() {
            {
                allowing(request).getRequestURI();
                will(returnValue("/IBMJMXConnectorREST/adminEndpoint"));
                allowing(request).getRequestURL();
                will(returnValue(new StringBuffer("https://localhost:9443/IBMJMXConnectorREST/adminEndpoint")));
                allowing(request).getServletPath();
                will(returnValue("/adminEndpoint"));
                allowing(request).getContextPath();
                will(returnValue("/IBMJMXConnectorREST"));
                allowing(request).getHeader("Authorization");
                will(returnValue("Bearer " + token));
            }
        });
    }

    @Test
    public void testIsTargetInterceptor_MatchesAdminEndpointWithBearerToken() throws Exception {
        expectAdminEndpointRequestWithBearer(createToken("{\"iss\":\"IBMJMXConnectorRESTIssuer\",\"aud\":\"IBMJMXConnectorREST\",\"sub\":\"adminUser\"}"));

        assertTrue("Should target request to admin endpoint with Bearer token from the REST issuer for the REST audience", tai.isTargetInterceptor(request));
    }

    @Test
    public void testIsTargetInterceptor_MatchesRestAudienceInAudienceArray() throws Exception {
        expectAdminEndpointRequestWithBearer(createToken("{\"iss\":\"IBMJMXConnectorRESTIssuer\",\"aud\":[\"other\",\"IBMJMXConnectorREST\"],\"sub\":\"adminUser\"}"));

        assertTrue("Should target a token whose audience array contains the REST audience", tai.isTargetInterceptor(request));
    }

    @Test
    public void testIsTargetInterceptor_RejectsBearerTokenFromOtherIssuer() throws Exception {
        expectAdminEndpointRequestWithBearer(createToken("{\"iss\":\"https://other.issuer\",\"aud\":\"IBMJMXConnectorREST\",\"sub\":\"adminUser\"}"));

        assertFalse("Should not target a token from another issuer", tai.isTargetInterceptor(request));
    }

    @Test
    public void testIsTargetInterceptor_RejectsBearerTokenForOtherAudience() throws Exception {
        expectAdminEndpointRequestWithBearer(createToken("{\"iss\":\"IBMJMXConnectorRESTIssuer\",\"aud\":\"someOtherAudience\",\"sub\":\"adminUser\"}"));

        assertFalse("Should not target a token for another audience", tai.isTargetInterceptor(request));
    }

    @Test
    public void testIsTargetInterceptor_RejectsBearerTokenWithoutAudience() throws Exception {
        expectAdminEndpointRequestWithBearer(createToken("{\"iss\":\"IBMJMXConnectorRESTIssuer\",\"sub\":\"adminUser\"}"));

        assertFalse("Should not target a token without an audience", tai.isTargetInterceptor(request));
    }

    @Test
    public void testIsTargetInterceptor_RejectsMalformedBearerToken() throws Exception {
        expectAdminEndpointRequestWithBearer("not-a-jwt");

        assertFalse("Should not target a malformed token", tai.isTargetInterceptor(request));
    }

    @Test
    public void testIsTargetInterceptor_RejectsNonAdminAppCall() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(request).getRequestURI();
                will(returnValue("/myApp/api/service"));
                allowing(request).getRequestURL();
                will(returnValue(new StringBuffer("https://localhost:9443/myApp/api/service")));
                allowing(request).getServletPath();
                will(returnValue("/service"));
                allowing(request).getContextPath();
                will(returnValue("/myApp"));
            }
        });

        assertFalse("Should not target regular application request", tai.isTargetInterceptor(request));
    }

    @Test
    public void testIsTargetInterceptor_RejectsWhenNoAuthorizationHeader() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(request).getRequestURI();
                will(returnValue("/IBMJMXConnectorREST/adminEndpoint"));
                allowing(request).getRequestURL();
                will(returnValue(new StringBuffer("https://localhost:9443/IBMJMXConnectorREST/adminEndpoint")));
                allowing(request).getServletPath();
                will(returnValue("/adminEndpoint"));
                allowing(request).getContextPath();
                will(returnValue("/IBMJMXConnectorREST"));
                allowing(request).getHeader("Authorization");
                will(returnValue(null));
                allowing(request).getCookies();
                will(returnValue(null));
            }
        });

        assertFalse("Should not target request without Authorization header", tai.isTargetInterceptor(request));
    }

    @Test
    public void testIsTargetInterceptor_MatchesLtpaToken2Cookie() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(request).getRequestURI();
                will(returnValue("/IBMJMXConnectorREST/mbeans"));
                allowing(request).getRequestURL();
                will(returnValue(new StringBuffer("https://localhost:9443/IBMJMXConnectorREST/mbeans")));
                allowing(request).getServletPath();
                will(returnValue("/mbeans"));
                allowing(request).getContextPath();
                will(returnValue("/IBMJMXConnectorREST"));
                allowing(request).getHeader("Authorization");
                will(returnValue(null));
                allowing(request).getCookies();
                will(returnValue(new Cookie[] { new Cookie("LtpaToken2", "abc") }));
            }
        });

        assertTrue("Should claim a request that carries an LtpaToken2 cookie so it can be rejected", tai.isTargetInterceptor(request));
    }

    @Test
    public void testNegotiate_LtpaToken2CookieWithoutBearer_Unauthorized() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(request).getHeader("Authorization");
                will(returnValue(null));
                allowing(request).getCookies();
                will(returnValue(new Cookie[] { new Cookie("LtpaToken2", "abc") }));
            }
        });

        TAIResult result = tai.negotiateValidateandEstablishTrust(request, response);
        assertEquals("An LtpaToken2 cookie must be rejected", HttpServletResponse.SC_UNAUTHORIZED, result.getStatus());
    }

    @Test
    public void testIsTargetInterceptor_RejectsNonBearerAuthorizationHeader() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(request).getRequestURI();
                will(returnValue("/IBMJMXConnectorREST/adminEndpoint"));
                allowing(request).getRequestURL();
                will(returnValue(new StringBuffer("https://localhost:9443/IBMJMXConnectorREST/adminEndpoint")));
                allowing(request).getServletPath();
                will(returnValue("/adminEndpoint"));
                allowing(request).getContextPath();
                will(returnValue("/IBMJMXConnectorREST"));
                allowing(request).getHeader("Authorization");
                will(returnValue("Basic dXNlcjpwYXNz"));
                allowing(request).getCookies();
                will(returnValue(null));
            }
        });

        assertFalse("Should not target request with Basic auth", tai.isTargetInterceptor(request));
    }

    @Test
    public void testGetBearerToken_CaseInsensitivePrefix() {
        mockery.checking(new Expectations() {
            {
                allowing(request).getHeader("Authorization");
                will(returnValue("bearer my-token-123"));
            }
        });

        assertEquals("my-token-123", tai.getBearerToken(request));
    }

    @Test
    public void testMappingHelper_ExtractsUsernameAndClaims() {
        mockery.checking(new Expectations() {
            {
                allowing(jwtToken).getClaims();
                will(returnValue(claims));
                allowing(claims).getClaim("upn", String.class);
                will(returnValue("adminUser@ibm.com"));
                allowing(claims).getClaim("preferred_username", String.class);
                will(returnValue(null));
                allowing(claims).getSubject();
                will(returnValue("adminUser"));
                allowing(claims).getClaim("username", String.class);
                will(returnValue("adminUser"));
                allowing(claims).getIssuer();
                will(returnValue("https://issuer.ibm.com"));
                allowing(claims).get("groups");
                will(returnValue(Arrays.asList("Administrator", "Reader")));
            }
        });

        RestJwtMappingHelper helper = new RestJwtMappingHelper(jwtToken);
        assertEquals("adminUser@ibm.com", helper.getUsername());
        assertEquals("https://issuer.ibm.com", helper.getRealm());
        assertEquals(Arrays.asList("Administrator", "Reader"), helper.getGroups());

        Subject subject = helper.createSubject();
        assertNotNull(subject);
        Set<Hashtable> creds = subject.getPrivateCredentials(Hashtable.class);
        assertFalse(creds.isEmpty());
        Hashtable customProps = creds.iterator().next();
        assertEquals("adminUser@ibm.com", customProps.get(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME));
        assertEquals("https://issuer.ibm.com/adminUser@ibm.com", customProps.get(AttributeNameConstants.WSCREDENTIAL_UNIQUEID));
        assertEquals("https://issuer.ibm.com", customProps.get(AttributeNameConstants.WSCREDENTIAL_REALM));
        assertEquals(Arrays.asList("Administrator", "Reader"), customProps.get(AttributeNameConstants.WSCREDENTIAL_GROUPS));
        assertEquals(Boolean.TRUE, customProps.get(AuthenticationConstants.INTERNAL_ASSERTION_KEY));
        assertEquals(Boolean.TRUE, customProps.get(RestJwtMappingHelper.INTERNAL_DISABLE_SSO_LTPA_CACHE));
    }

    @Test
    public void testMappingHelper_FallsBackToSubjectClaim() {
        mockery.checking(new Expectations() {
            {
                allowing(jwtToken).getClaims();
                will(returnValue(claims));
                allowing(claims).getClaim("upn", String.class);
                will(returnValue(null));
                allowing(claims).getClaim("preferred_username", String.class);
                will(returnValue(null));
                allowing(claims).getSubject();
                will(returnValue("adminSubject"));
                allowing(claims).getClaim("username", String.class);
                will(returnValue(null));
                allowing(claims).getIssuer();
                will(returnValue(null));
                allowing(claims).get("groups");
                will(returnValue("Administrator,Auditor"));
            }
        });

        RestJwtMappingHelper helper = new RestJwtMappingHelper(jwtToken);
        assertEquals("adminSubject", helper.getUsername());
        assertNull(helper.getRealm());
        assertEquals(Arrays.asList("Administrator", "Auditor"), helper.getGroups());
    }

    @Test
    public void testKeyLocationProperties_NoJwtConsumerConfigReturnsNull() {
        assertNull("No MP properties expected without a jwtConsumer", tai.getKeyLocationProperties(null));
    }

    @Test
    public void testKeyLocationProperties_JwkEndpointUrlNotSetReturnsNull() {
        final JwtConsumerConfig config = mockery.mock(JwtConsumerConfig.class);
        mockery.checking(new Expectations() {
            {
                allowing(config).getJwkEndpointUrl();
                will(returnValue(null));
            }
        });
        assertNull("No MP properties expected when jwkEndpointUrl is not set", tai.getKeyLocationProperties(config));
    }

    @Test
    public void testKeyLocationProperties_BlankJwkEndpointUrlReturnsNull() {
        final JwtConsumerConfig config = mockery.mock(JwtConsumerConfig.class);
        mockery.checking(new Expectations() {
            {
                allowing(config).getJwkEndpointUrl();
                will(returnValue("   "));
            }
        });
        assertNull("A blank jwkEndpointUrl must be ignored", tai.getKeyLocationProperties(config));
    }

    @Test
    public void testKeyLocationProperties_FileUrlMapsToKeyLocation() {
        final JwtConsumerConfig config = mockery.mock(JwtConsumerConfig.class);
        mockery.checking(new Expectations() {
            {
                allowing(config).getJwkEndpointUrl();
                will(returnValue(" file:///tmp/jwk.json "));
            }
        });
        MpConfigProperties props = tai.getKeyLocationProperties(config);
        assertNotNull(props);
        assertEquals("file:///tmp/jwk.json", props.get(MpConfigProperties.KEY_LOCATION));
    }

    @Test
    public void testTaiProperties() throws Exception {
        assertEquals(0, tai.initialize(null));
        assertEquals("1.0", tai.getVersion());
        assertEquals(RestJwtTAI.class.getName(), tai.getType());
    }
}
