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
package com.ibm.ws.microprofile.openapi.validation.test;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.OpenAPIImpl;
import com.ibm.ws.microprofile.openapi.impl.model.security.OAuthFlowImpl;
import com.ibm.ws.microprofile.openapi.impl.model.security.OAuthFlowsImpl;
import com.ibm.ws.microprofile.openapi.impl.model.security.ScopesImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.OAuthFlowsValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class OAuthFlowsValidatorTest {

    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Test
    public void testNewOAuthFlowsObject() {
        OAuthFlowsValidator validator = OAuthFlowsValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OAuthFlowsImpl oauthflows = new OAuthFlowsImpl();

        validator.validate(vh, context, oauthflows);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testNullOAuthFlowsObject() {
        OAuthFlowsValidator validator = OAuthFlowsValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OAuthFlowsImpl oauthflows = null;

        validator.validate(vh, context, oauthflows);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testCorrectOAuthFlows() {
        OAuthFlowsValidator validator = OAuthFlowsValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OAuthFlowsImpl oauthflows = new OAuthFlowsImpl();

        OAuthFlowImpl implicit = new OAuthFlowImpl();
        implicit.setAuthorizationUrl("http://test-authorization-url.com");
        implicit.setRefreshUrl("http://test-url.com");
        ScopesImpl scopesImplicit = new ScopesImpl();
        scopesImplicit.addScope("implicit", "write:app");
        implicit.setScopes(scopesImplicit);
        oauthflows.setImplicit(implicit);

        OAuthFlowImpl password = new OAuthFlowImpl();
        password.setTokenUrl("http://test-token-url.com");
        password.setRefreshUrl("http://test-url.com");
        ScopesImpl scopesPassword = new ScopesImpl();
        scopesPassword.addScope("password", "read:app");
        password.setScopes(scopesPassword);
        oauthflows.setPassword(password);

        OAuthFlowImpl clientCredentials = new OAuthFlowImpl();
        clientCredentials.setTokenUrl("http://test-client-url.com");
        ScopesImpl scopesClientCredentials = new ScopesImpl();
        scopesClientCredentials.addScope("clientCred", "read:app");
        clientCredentials.setScopes(scopesClientCredentials);
        oauthflows.setClientCredentials(clientCredentials);

        OAuthFlowImpl authCode = new OAuthFlowImpl();
        authCode.setTokenUrl("http://test-token-url.com");
        authCode.setAuthorizationUrl("http://test-auth-url.com");
        ScopesImpl scopesAuthorizationCode = new ScopesImpl();
        scopesAuthorizationCode.addScope("authCode", "read:app");
        authCode.setScopes(scopesAuthorizationCode);
        oauthflows.setAuthorizationCode(authCode);

        validator.validate(vh, context, oauthflows);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testOAuthFlowsImplicit() {
        OAuthFlowsValidator validator = OAuthFlowsValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OAuthFlowsImpl oauthflows = new OAuthFlowsImpl();

        OAuthFlowImpl implicit = new OAuthFlowImpl();
        implicit.setTokenUrl("http://test-authorization-url.com");
        implicit.setRefreshUrl("http://test-url.com");
        ScopesImpl scopesImplicit = new ScopesImpl();
        scopesImplicit.addScope("implicit", "write:app");
        implicit.setScopes(scopesImplicit);
        oauthflows.setImplicit(implicit);

        validator.validate(vh, context, oauthflows);
        Assert.assertEquals(2, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The \"tokenUrl\" field with value \"http://test-authorization-url.com\" is not applicable for \"implicit OAuthFlow\"."));
        Assert.assertTrue(vh.getResult().getEvents().get(1).message.contains("Required \"authorizationUrl\" field is missing or is set to an invalid value."));
    }

    @Test
    public void testOAuthFlowsPassword() {
        OAuthFlowsValidator validator = OAuthFlowsValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OAuthFlowsImpl oauthflows = new OAuthFlowsImpl();

        OAuthFlowImpl password = new OAuthFlowImpl();
        password.setAuthorizationUrl("http://test-token-url.com");
        password.setRefreshUrl("http://test-url.com");
        ScopesImpl scopesPassword = new ScopesImpl();
        scopesPassword.addScope("password", "read:app");
        password.setScopes(scopesPassword);
        oauthflows.setPassword(password);

        validator.validate(vh, context, oauthflows);
        Assert.assertEquals(2, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The \"authorizationUrl\" field with value \"http://test-token-url.com\" is not applicable for \"password OAuthFlow\"."));
        Assert.assertTrue(vh.getResult().getEvents().get(1).message.contains("Required \"tokenUrl\" field is missing or is set to an invalid value."));
    }

    @Test
    public void testOAuthFlowsClientCredentials() {
        OAuthFlowsValidator validator = OAuthFlowsValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OAuthFlowsImpl oauthflows = new OAuthFlowsImpl();

        OAuthFlowImpl clientCredentials = new OAuthFlowImpl();
        clientCredentials.setAuthorizationUrl("http://test-client-url.com");
        ScopesImpl scopesClientCredentials = new ScopesImpl();
        scopesClientCredentials.addScope("clientCred", "read:app");
        clientCredentials.setScopes(scopesClientCredentials);
        oauthflows.setClientCredentials(clientCredentials);

        validator.validate(vh, context, oauthflows);
        Assert.assertEquals(2, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The \"authorizationUrl\" field with value \"http://test-client-url.com\" is not applicable for \"clientCredentials OAuthFlow\"."));
        Assert.assertTrue(vh.getResult().getEvents().get(1).message.contains("Required \"tokenUrl\" field is missing or is set to an invalid value."));
    }

    @Test
    public void testOAuthFlowsAuthorizationCode() {
        OAuthFlowsValidator validator = OAuthFlowsValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OAuthFlowsImpl oauthflows = new OAuthFlowsImpl();

        OAuthFlowImpl authCode = new OAuthFlowImpl();
        ScopesImpl scopesAuthorizationCode = new ScopesImpl();
        scopesAuthorizationCode.addScope("authCode", "read:app");
        authCode.setScopes(scopesAuthorizationCode);
        oauthflows.setAuthorizationCode(authCode);

        validator.validate(vh, context, oauthflows);
        Assert.assertEquals(2, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("Required \"tokenUrl\" field is missing or is set to an invalid value."));
        Assert.assertTrue(vh.getResult().getEvents().get(1).message.contains("Required \"authorizationUrl\" field is missing or is set to an invalid value."));
    }
}
