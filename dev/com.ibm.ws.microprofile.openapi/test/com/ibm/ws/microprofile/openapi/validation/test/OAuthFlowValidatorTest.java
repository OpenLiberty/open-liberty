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
import com.ibm.ws.microprofile.openapi.impl.model.security.ScopesImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.OAuthFlowValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class OAuthFlowValidatorTest {

    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Test
    public void testNewOAuthFlowObject() {
        OAuthFlowValidator validator = OAuthFlowValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OAuthFlowImpl oauthflow = new OAuthFlowImpl();

        validator.validate(vh, context, oauthflow);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("Required \"scopes\" field is missing or is set to an invalid value"));
    }

    @Test
    public void testNullOAuthFlowObject() {
        OAuthFlowValidator validator = OAuthFlowValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OAuthFlowImpl oauthflow = null;

        validator.validate(vh, context, oauthflow);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testOAuthFlowWithInvalidAuthorizationUrl() {
        OAuthFlowValidator validator = OAuthFlowValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OAuthFlowImpl oauthflow = new OAuthFlowImpl();
        oauthflow.setAuthorizationUrl(":invalidUrl-example");
        ScopesImpl scopes = new ScopesImpl();
        scopes.addScope("test_scope", "read:test");
        oauthflow.setScopes(scopes);

        validator.validate(vh, context, oauthflow);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The OAuth Flow Object must contain a valid URL"));
    }

    @Test
    public void testOAuthFlowWithInvalidTokenUrl() {
        OAuthFlowValidator validator = OAuthFlowValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OAuthFlowImpl oauthflow = new OAuthFlowImpl();
        oauthflow.setTokenUrl(":invalidUrl-example");
        ScopesImpl scopes = new ScopesImpl();
        scopes.addScope("test_scope", "read:test");
        oauthflow.setScopes(scopes);

        validator.validate(vh, context, oauthflow);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The OAuth Flow Object must contain a valid URL"));
    }

    @Test
    public void testOAuthFlowWithInvalidRefreshUrl() {
        OAuthFlowValidator validator = OAuthFlowValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OAuthFlowImpl oauthflow = new OAuthFlowImpl();
        oauthflow.setRefreshUrl(":invalidUrl-example");
        ScopesImpl scopes = new ScopesImpl();
        scopes.addScope("test_scope", "read:test");
        oauthflow.setScopes(scopes);

        validator.validate(vh, context, oauthflow);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The OAuth Flow Object must contain a valid URL"));
    }

    @Test
    public void testOAuthFlowWithoutScope() {
        OAuthFlowValidator validator = OAuthFlowValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OAuthFlowImpl oauthflow = new OAuthFlowImpl();
        oauthflow.setAuthorizationUrl("http://valid-url.com");

        validator.validate(vh, context, oauthflow);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("Required \"scopes\" field is missing or is set to an invalid value"));
    }

    @Test
    public void testCorrectOAuthFlow() {
        OAuthFlowValidator validator = OAuthFlowValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OAuthFlowImpl oauthflow = new OAuthFlowImpl();
        oauthflow.setRefreshUrl("http://correct-url.com");
        ScopesImpl scopes = new ScopesImpl();
        scopes.addScope("test_scope", "read:test");
        oauthflow.setScopes(scopes);

        validator.validate(vh, context, oauthflow);
        Assert.assertEquals(0, vh.getEventsSize());
    }
}
