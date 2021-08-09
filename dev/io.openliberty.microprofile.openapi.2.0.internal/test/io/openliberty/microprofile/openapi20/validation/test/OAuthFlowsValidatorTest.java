/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.validation.test;

import org.junit.Assert;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.test.utils.TestValidationContextHelper;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationHelper;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.OAuthFlowsValidator;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.security.OAuthFlowImpl;
import io.smallrye.openapi.api.models.security.OAuthFlowsImpl;

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
        implicit.addScope("implicit", "write:app");
        oauthflows.setImplicit(implicit);

        OAuthFlowImpl password = new OAuthFlowImpl();
        password.setTokenUrl("http://test-token-url.com");
        password.setRefreshUrl("http://test-url.com");
        password.addScope("password", "read:app");
        oauthflows.setPassword(password);

        OAuthFlowImpl clientCredentials = new OAuthFlowImpl();
        clientCredentials.setTokenUrl("http://test-client-url.com");
        clientCredentials.addScope("clientCred", "read:app");
        oauthflows.setClientCredentials(clientCredentials);

        OAuthFlowImpl authCode = new OAuthFlowImpl();
        authCode.setTokenUrl("http://test-token-url.com");
        authCode.setAuthorizationUrl("http://test-auth-url.com");
        authCode.addScope("authCode", "read:app");
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
        implicit.addScope("implicit", "write:app");
        oauthflows.setImplicit(implicit);

        validator.validate(vh, context, oauthflows);
        Assert.assertEquals(2, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The \"tokenUrl\" field with \"http://test-authorization-url.com\" value is not applicable for \"OAuth Flow Object\" of \"implicit\" type"));
        Assert.assertTrue(vh.getResult().getEvents().get(1).message.contains("Required \"authorizationUrl\" field is missing or is set to an invalid value"));
    }

    @Test
    public void testOAuthFlowsPassword() {
        OAuthFlowsValidator validator = OAuthFlowsValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OAuthFlowsImpl oauthflows = new OAuthFlowsImpl();

        OAuthFlowImpl password = new OAuthFlowImpl();
        password.setAuthorizationUrl("http://test-token-url.com");
        password.setRefreshUrl("http://test-url.com");
        password.addScope("password", "read:app");
        oauthflows.setPassword(password);

        validator.validate(vh, context, oauthflows);
        Assert.assertEquals(2, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The \"authorizationUrl\" field with \"http://test-token-url.com\" value is not applicable for \"OAuth Flow Object\" of \"password\" type"));
        Assert.assertTrue(vh.getResult().getEvents().get(1).message.contains("Required \"tokenUrl\" field is missing or is set to an invalid value"));
    }

    @Test
    public void testOAuthFlowsClientCredentials() {
        OAuthFlowsValidator validator = OAuthFlowsValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OAuthFlowsImpl oauthflows = new OAuthFlowsImpl();

        OAuthFlowImpl clientCredentials = new OAuthFlowImpl();
        clientCredentials.setAuthorizationUrl("http://test-client-url.com");
        clientCredentials.addScope("clientCred", "read:app");
        oauthflows.setClientCredentials(clientCredentials);

        validator.validate(vh, context, oauthflows);
        Assert.assertEquals(2, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The \"authorizationUrl\" field with \"http://test-client-url.com\" value is not applicable for \"OAuth Flow Object\" of \"clientCredentials\" type"));
        Assert.assertTrue(vh.getResult().getEvents().get(1).message.contains("Required \"tokenUrl\" field is missing or is set to an invalid value"));
    }

    @Test
    public void testOAuthFlowsAuthorizationCode() {
        OAuthFlowsValidator validator = OAuthFlowsValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OAuthFlowsImpl oauthflows = new OAuthFlowsImpl();

        OAuthFlowImpl authCode = new OAuthFlowImpl();
        authCode.addScope("authCode", "read:app");
        oauthflows.setAuthorizationCode(authCode);

        validator.validate(vh, context, oauthflows);
        Assert.assertEquals(2, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("Required \"tokenUrl\" field is missing or is set to an invalid value"));
        Assert.assertTrue(vh.getResult().getEvents().get(1).message.contains("Required \"authorizationUrl\" field is missing or is set to an invalid value"));
    }
}
