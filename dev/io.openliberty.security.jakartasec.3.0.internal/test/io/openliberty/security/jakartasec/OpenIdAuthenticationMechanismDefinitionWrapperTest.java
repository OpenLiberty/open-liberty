/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import io.openliberty.security.oidcclientcore.client.ClaimsMappingConfig;
import io.openliberty.security.oidcclientcore.client.LogoutConfig;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.DisplayType;
import jakarta.security.enterprise.authentication.mechanism.http.openid.LogoutDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdProviderMetadata;
import jakarta.security.enterprise.authentication.mechanism.http.openid.PromptType;
import test.common.SharedOutputManager;

/**
 * Verify that the {@link OpenIdAuthenticationMechanismDefinitionWrapper} provides proper support for
 * retrieving and evaluating both EL expressions and literal settings as called for in Jakarta Security 3.0.
 */
public class OpenIdAuthenticationMechanismDefinitionWrapperTest {

    private static final String PROVIDER_URI = "providerURI";
    private static final String CLIENT_ID = "clientId";
    private static final String CLIENT_SECRET = "clientSecret";
    private static final String REDIRECT_URI = "redirectURI";
    private static final String REDIRECT_TO_ORIGINAL_RESOURCE = "redirectToOriginalResource";
    private static final String REDIRECT_TO_ORIGINAL_RESOURCE_EXPRESSION = "redirectToOriginalResourceExpression";
    private static final String SCOPE = "scope";
    private static final String SCOPE_EXPRESSION = "scopeExpression";
    private static final String RESPONSE_TYPE = "responseType";
    private static final String RESPONSE_MODE = "responseMode";
    private static final String PROMPT = "prompt";
    private static final String PROMPT_EXPRESSION = "promptExpression";
    private static final String DISPLAY = "display";
    private static final String DISPLAY_EXPRESSION = "displayExpression";
    private static final String USE_NONCE = "useNonce";
    private static final String USE_NONCE_EXPRESSION = "useNonceExpression";
    private static final String USE_SESSION = "useSession";
    private static final String USE_SESSION_EXPRESSION = "useSessionExpression";
    private static final String EXTRA_PARAMETERS = "extraParameters";
    private static final String EXTRA_PARAMETERS_EXPRESSION = "extraParametersExpression";
    private static final String JWKS_CONNECT_TIMEOUT = "jwksConnectTimeout";
    private static final String JWKS_CONNECT_TIMEOUT_EXPRESSION = "jwksConnectTimeoutExpression";
    private static final String JWKS_READ_TIMEOUT = "jwksReadTimeout";
    private static final String JWKS_READ_TIMEOUT_EXPRESSION = "jwksReadTimeoutExpression";
    private static final String TOKEN_AUTO_REFRESH = "tokenAutoRefresh";
    private static final String TOKEN_AUTO_REFRESH_EXPRESSION = "tokenAutoRefreshExpression";
    private static final String TOKEN_MIN_VALIDITY = "tokenMinValidity";
    private static final String TOKEN_MIN_VALIDITY_EXPRESSION = "tokenMinValidityExpression";
    private static final String EMPTY_DEFAULT = "";
    private static final String STRING_EL_EXPRESSION = "#{'blah'.concat('blah')}";
    private static final String EVALUATED_EL_EXPRESSION_STRING_RESULT = "blahblah";
    private static final String TRUE_EL_EXPRESSION = "#{true}";
    private static final String FALSE_EL_EXPRESSION = "#{false}";
    private static final String INTEGER_EL_EXPRESSION = "#{1000}";
    private static final int INTEGER_EL_EXPRESSION_RESULT = 1000;
    private static final int TIMEOUT_DEFAULT = 500;
    private static final int TOKEN_MIN_VALIDITY_DEFAULT = 10000;
    private static final String[] SCOPE_DEFAULT = new String[] { OpenIdConstant.OPENID_SCOPE,
                                                                 OpenIdConstant.EMAIL_SCOPE,
                                                                 OpenIdConstant.PROFILE_SCOPE };

    private static final String constructedBaseURL = "https:/localhost:9443/myServlet/";
    private Map<String, Object> overrides;

    SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all");
    @Rule
    public TestRule outputRule = outputMgr;

    @Before
    public void setUp() {
        overrides = new HashMap<String, Object>();
    }

    @Test
    public void testGetProviderURI() {
        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(null);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(EMPTY_DEFAULT, wrapper.getProviderURI());
    }

    @Test
    public void testGetProviderURI_EL() {
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = createWrapperWithSimpleELExpressionForAttribute(PROVIDER_URI);

        assertEquals(EVALUATED_EL_EXPRESSION_STRING_RESULT, wrapper.getProviderURI());
    }

    // TODO: Unit test getProviderMetadata

    @Test
    public void testGetClientId() {
        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(null);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(EMPTY_DEFAULT, wrapper.getClientId());
    }

    @Test
    public void testGetClientId_EL() {
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = createWrapperWithSimpleELExpressionForAttribute(CLIENT_ID);

        assertEquals(EVALUATED_EL_EXPRESSION_STRING_RESULT, wrapper.getClientId());
    }

    @Test
    public void testGetClientSecret() {
        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(null);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(EMPTY_DEFAULT, String.valueOf(wrapper.getClientSecret().getChars()));
    }

    @Test
    public void testGetClientSecret_EL() {
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = createWrapperWithSimpleELExpressionForAttribute(CLIENT_SECRET);

        assertEquals(EVALUATED_EL_EXPRESSION_STRING_RESULT, String.valueOf(wrapper.getClientSecret().getChars()));
    }

    @Test
    public void testGetClaimsMappingConfig() {
        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(null);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        ClaimsMappingConfig claimsMappingConfig = wrapper.getClaimsMappingConfig();
        assertNotNull(claimsMappingConfig);
    }

    @Test
    public void testGetLogoutConfig() {
        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(null);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        LogoutConfig logoutConfig = wrapper.getLogoutConfig();
        assertNotNull(logoutConfig);
    }

    @Test
    public void testGetRedirectURI() {
        overrides.put(REDIRECT_URI, "localhost");

        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(overrides);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals("localhost", wrapper.getRedirectURI());
    }

    /**
     * Test with a sample EL expression in a composite expression. The evaluated expression should be added to the non expression String
     */
    @Test
    public void testGetRedirectURI_EL() {
        overrides.put(REDIRECT_URI, STRING_EL_EXPRESSION + "/Callback");

        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(overrides);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(EVALUATED_EL_EXPRESSION_STRING_RESULT + "/Callback", wrapper.getRedirectURI());
    }

    /**
     * Test with the baseURL variable and another EL expression in a concatenated singled expression. Both should be evaluated and put together.
     */
    @Test
    public void testGetRedirectURI_EL_plus() {
        overrides.put(REDIRECT_URI, "#{" + JakartaSec30Constants.BASE_URL_VARIABLE + " += 'blah'.concat('blah')}");

        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(overrides);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(constructedBaseURL + EVALUATED_EL_EXPRESSION_STRING_RESULT, wrapper.getRedirectURI());
    }

    /**
     * Test with a composite expression using the baseURL variable which should be replaced with a constructed baseURL.
     */
    @Test
    public void testGetRedirectURI_EL_default() {
        overrides.put(REDIRECT_URI, JakartaSec30Constants.BASE_URL_DEFAULT);

        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(overrides);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(constructedBaseURL + "/Callback", wrapper.getRedirectURI());
    }

    /**
     * Test with an extra closing bracket. The first closing bracket should be used to evaluate the
     * expression and the extra bracket left in the rest of the provided string.
     */
    @Test
    public void testGetRedirectURI_EL_extraBracket() {
        overrides.put(REDIRECT_URI, STRING_EL_EXPRESSION + "/Callback}back");

        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(overrides);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(REDIRECT_URI, EVALUATED_EL_EXPRESSION_STRING_RESULT + "/Callback}back", wrapper.getRedirectURI());
    }

    /**
     * Test with a missing closing bracket, the same expression passed in should be returned without changes.
     */
    @Test
    public void testGetRedirectURI_EL_missingBracket() {
        overrides.put(REDIRECT_URI, "#{'blah'.concat('blah')" + "/Callback");

        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(overrides);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(REDIRECT_URI, "#{'blah'.concat('blah')" + "/Callback", wrapper.getRedirectURI());
    }

    @Test
    public void testIsRedirectToOriginalResource() {
        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(null);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(false, wrapper.isRedirectToOriginalResource());
    }

    @Test
    public void testIsRedirectToOriginalResource_EL() {
        overrides.put(REDIRECT_TO_ORIGINAL_RESOURCE_EXPRESSION, TRUE_EL_EXPRESSION);

        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(overrides);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(true, wrapper.isRedirectToOriginalResource());
    }

    @Test
    public void testGetScope() {
        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(null);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertTrue(wrapper.getScope().contains(OpenIdConstant.OPENID_SCOPE));
        assertTrue(wrapper.getScope().contains(OpenIdConstant.EMAIL_SCOPE));
        assertTrue(wrapper.getScope().contains(OpenIdConstant.PROFILE_SCOPE));
    }

    // TODO: Determine an EL expression that evaluates to String[]
    @Test
    @Ignore("Need to determine an EL expression that evaluates to String[].")
    public void testGetScope_EL() {
        overrides.put(SCOPE_EXPRESSION, "['phone', 'offline_access'].stream().toArray()");

        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(overrides);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertTrue(wrapper.getScope().contains(OpenIdConstant.PHONE_SCOPE));
        assertTrue(wrapper.getScope().contains(OpenIdConstant.OFFLINE_ACCESS_SCOPE));
    }

    @Test
    public void testGetResponseType() {
        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(null);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(OpenIdConstant.CODE, wrapper.getResponseType());
    }

    @Test
    public void testGetResponseType_EL() {
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = createWrapperWithSimpleELExpressionForAttribute(RESPONSE_TYPE);

        assertEquals(EVALUATED_EL_EXPRESSION_STRING_RESULT, wrapper.getResponseType());
    }

    @Test
    public void testGetResponseMode() {
        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(null);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(EMPTY_DEFAULT, wrapper.getResponseMode());
    }

    @Test
    public void testGetResponseMode_EL() {
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = createWrapperWithSimpleELExpressionForAttribute(RESPONSE_MODE);

        assertEquals(EVALUATED_EL_EXPRESSION_STRING_RESULT, wrapper.getResponseMode());
    }

    @Test
    public void testGetPromptParameter() {
        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(null);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(EMPTY_DEFAULT, wrapper.getPromptParameter());
    }

    // TODO: Unit test getPromptParameter with EL expression

    @Test
    public void testGetDisplayParameter() {
        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(null);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals("page", wrapper.getDisplayParameter());
    }

    @Test
    @Ignore("This will fail until the EL implementation can handle inner enums.")
    public void testGetDisplayParameter_EL() {
        overrides.put(DISPLAY_EXPRESSION, "DisplayType.POPUP");

        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(overrides);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals("popup", wrapper.getDisplayParameter());
    }

    @Test
    public void testIsUseNonce() {
        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(null);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(true, wrapper.isUseNonce());
    }

    @Test
    public void testIsUseNonce_EL() {
        overrides.put(USE_NONCE_EXPRESSION, FALSE_EL_EXPRESSION);

        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(overrides);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(false, wrapper.isUseNonce());
    }

    @Test
    public void testIsUseSession() {
        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(null);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(true, wrapper.isUseSession());
    }

    @Test
    public void testIsUseSession_EL() {
        overrides.put(USE_SESSION_EXPRESSION, FALSE_EL_EXPRESSION);

        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(overrides);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(false, wrapper.isUseSession());
    }

    // TODO: Unit test getExtraParameters

    // TODO: Unit test getExtraParameters with EL expression

    @Test
    public void testGetJwksConnectTimeout() {
        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(null);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(TIMEOUT_DEFAULT, wrapper.getJwksConnectTimeout());
    }

    @Test
    public void testGetJwksConnectTimeout_EL() {
        overrides.put(JWKS_CONNECT_TIMEOUT_EXPRESSION, INTEGER_EL_EXPRESSION);

        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(overrides);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(INTEGER_EL_EXPRESSION_RESULT, wrapper.getJwksConnectTimeout());
    }

    @Test
    public void testGetJwksReadTimeout() {
        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(null);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(TIMEOUT_DEFAULT, wrapper.getJwksReadTimeout());
    }

    @Test
    public void testGetJwksReadTimeout_EL() {
        overrides.put(JWKS_READ_TIMEOUT_EXPRESSION, INTEGER_EL_EXPRESSION);

        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(overrides);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(INTEGER_EL_EXPRESSION_RESULT, wrapper.getJwksReadTimeout());
    }

    @Test
    public void testIsTokenAutoRefresh() {
        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(null);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(false, wrapper.isTokenAutoRefresh());
    }

    @Test
    public void testIsTokenAutoRefresh_EL() {
        overrides.put(TOKEN_AUTO_REFRESH_EXPRESSION, TRUE_EL_EXPRESSION);

        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(overrides);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(true, wrapper.isTokenAutoRefresh());
    }

    @Test
    public void testGetTokenMinValidity() {
        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(null);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(TOKEN_MIN_VALIDITY_DEFAULT, wrapper.getTokenMinValidity());
    }

    @Test
    public void testGetTokenMinValidity_EL() {
        overrides.put(TOKEN_MIN_VALIDITY_EXPRESSION, INTEGER_EL_EXPRESSION);

        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(overrides);
        OpenIdAuthenticationMechanismDefinitionWrapper wrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);

        assertEquals(INTEGER_EL_EXPRESSION_RESULT, wrapper.getTokenMinValidity());
    }

    private OpenIdAuthenticationMechanismDefinitionWrapper createWrapperWithSimpleELExpressionForAttribute(String attributeName) {
        overrides.put(attributeName, STRING_EL_EXPRESSION);

        OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition = getInstanceofAnnotation(overrides);
        return new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition, constructedBaseURL);
    }

    private OpenIdAuthenticationMechanismDefinition getInstanceofAnnotation(final Map<String, Object> overrides) {
        OpenIdAuthenticationMechanismDefinition annotation = new OpenIdAuthenticationMechanismDefinition() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String providerURI() {
                return (overrides != null && overrides.containsKey(PROVIDER_URI)) ? (String) overrides.get(PROVIDER_URI) : EMPTY_DEFAULT;
            }

            @Override
            public OpenIdProviderMetadata providerMetadata() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String clientId() {
                return (overrides != null && overrides.containsKey(CLIENT_ID)) ? (String) overrides.get(CLIENT_ID) : EMPTY_DEFAULT;
            }

            @Override
            public String clientSecret() {
                return (overrides != null && overrides.containsKey(CLIENT_SECRET)) ? (String) overrides.get(CLIENT_SECRET) : EMPTY_DEFAULT;
            }

            @Override
            public ClaimsDefinition claimsDefinition() {
                return TestClaimsDefinition.getInstanceofAnnotation(null);
            }

            @Override
            public LogoutDefinition logout() {
                return TestLogoutDefinition.getInstanceofAnnotation(null);
            }

            @Override
            public String redirectURI() {
                return (overrides != null && overrides.containsKey(REDIRECT_URI)) ? (String) overrides.get(REDIRECT_URI) : "${baseURL}/Callback";
            }

            @Override
            public boolean redirectToOriginalResource() {
                return (overrides != null && overrides.containsKey(REDIRECT_TO_ORIGINAL_RESOURCE)) ? (Boolean) overrides.get(REDIRECT_TO_ORIGINAL_RESOURCE) : false;
            }

            @Override
            public String redirectToOriginalResourceExpression() {
                return (overrides != null
                        && overrides.containsKey(REDIRECT_TO_ORIGINAL_RESOURCE_EXPRESSION)) ? (String) overrides.get(REDIRECT_TO_ORIGINAL_RESOURCE_EXPRESSION) : EMPTY_DEFAULT;
            }

            @Override
            public String[] scope() {
                return (overrides != null && overrides.containsKey(SCOPE)) ? (String[]) overrides.get(SCOPE) : SCOPE_DEFAULT;
            }

            @Override
            public String scopeExpression() {
                return (overrides != null && overrides.containsKey(SCOPE_EXPRESSION)) ? (String) overrides.get(SCOPE_EXPRESSION) : EMPTY_DEFAULT;
            }

            @Override
            public String responseType() {
                return (overrides != null && overrides.containsKey(RESPONSE_TYPE)) ? (String) overrides.get(RESPONSE_TYPE) : OpenIdConstant.CODE;
            }

            @Override
            public String responseMode() {
                return (overrides != null && overrides.containsKey(RESPONSE_MODE)) ? (String) overrides.get(RESPONSE_MODE) : EMPTY_DEFAULT;
            }

            @Override
            public PromptType[] prompt() {
                return (overrides != null && overrides.containsKey(PROMPT)) ? (PromptType[]) overrides.get(PROMPT) : new PromptType[] {};
            }

            @Override
            public String promptExpression() {
                return (overrides != null && overrides.containsKey(PROMPT_EXPRESSION)) ? (String) overrides.get(PROMPT_EXPRESSION) : EMPTY_DEFAULT;
            }

            @Override
            public DisplayType display() {
                return (overrides != null && overrides.containsKey(DISPLAY)) ? (DisplayType) overrides.get(DISPLAY) : DisplayType.PAGE;
            }

            @Override
            public String displayExpression() {
                return (overrides != null && overrides.containsKey(DISPLAY_EXPRESSION)) ? (String) overrides.get(DISPLAY_EXPRESSION) : EMPTY_DEFAULT;
            }

            @Override
            public boolean useNonce() {
                return (overrides != null && overrides.containsKey(USE_NONCE)) ? (Boolean) overrides.get(USE_NONCE) : true;
            }

            @Override
            public String useNonceExpression() {
                return (overrides != null && overrides.containsKey(USE_NONCE_EXPRESSION)) ? (String) overrides.get(USE_NONCE_EXPRESSION) : EMPTY_DEFAULT;
            }

            @Override
            public boolean useSession() {
                return (overrides != null && overrides.containsKey(USE_SESSION)) ? (Boolean) overrides.get(USE_SESSION) : true;
            }

            @Override
            public String useSessionExpression() {
                return (overrides != null && overrides.containsKey(USE_SESSION_EXPRESSION)) ? (String) overrides.get(USE_SESSION_EXPRESSION) : EMPTY_DEFAULT;
            }

            @Override
            public String[] extraParameters() {
                return (overrides != null && overrides.containsKey(EXTRA_PARAMETERS)) ? (String[]) overrides.get(EXTRA_PARAMETERS) : new String[] {};
            }

            @Override
            public String extraParametersExpression() {
                return (overrides != null && overrides.containsKey(EXTRA_PARAMETERS_EXPRESSION)) ? (String) overrides.get(EXTRA_PARAMETERS_EXPRESSION) : EMPTY_DEFAULT;
            }

            @Override
            public int jwksConnectTimeout() {
                return (overrides != null && overrides.containsKey(JWKS_CONNECT_TIMEOUT)) ? (Integer) overrides.get(JWKS_CONNECT_TIMEOUT) : TIMEOUT_DEFAULT;
            }

            @Override
            public String jwksConnectTimeoutExpression() {
                return (overrides != null && overrides.containsKey(JWKS_CONNECT_TIMEOUT_EXPRESSION)) ? (String) overrides.get(JWKS_CONNECT_TIMEOUT_EXPRESSION) : EMPTY_DEFAULT;
            }

            @Override
            public int jwksReadTimeout() {
                return (overrides != null && overrides.containsKey(JWKS_READ_TIMEOUT)) ? (Integer) overrides.get(JWKS_READ_TIMEOUT) : TIMEOUT_DEFAULT;
            }

            @Override
            public String jwksReadTimeoutExpression() {
                return (overrides != null && overrides.containsKey(JWKS_READ_TIMEOUT_EXPRESSION)) ? (String) overrides.get(JWKS_READ_TIMEOUT_EXPRESSION) : EMPTY_DEFAULT;
            }

            @Override
            public boolean tokenAutoRefresh() {
                return (overrides != null && overrides.containsKey(TOKEN_AUTO_REFRESH)) ? (Boolean) overrides.get(TOKEN_AUTO_REFRESH) : false;
            }

            @Override
            public String tokenAutoRefreshExpression() {
                return (overrides != null && overrides.containsKey(TOKEN_AUTO_REFRESH_EXPRESSION)) ? (String) overrides.get(TOKEN_AUTO_REFRESH_EXPRESSION) : EMPTY_DEFAULT;
            }

            @Override
            public int tokenMinValidity() {
                return (overrides != null && overrides.containsKey(TOKEN_MIN_VALIDITY)) ? (Integer) overrides.get(TOKEN_MIN_VALIDITY) : TOKEN_MIN_VALIDITY_DEFAULT;
            }

            @Override
            public String tokenMinValidityExpression() {
                return (overrides != null && overrides.containsKey(TOKEN_MIN_VALIDITY_EXPRESSION)) ? (String) overrides.get(TOKEN_MIN_VALIDITY_EXPRESSION) : EMPTY_DEFAULT;
            }

        };

        return annotation;
    }

}