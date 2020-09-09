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
import io.openliberty.microprofile.openapi20.validation.OAuthFlowValidator;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.security.OAuthFlowImpl;

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
        oauthflow.addScope("test_scope", "read:test");

        validator.validate(vh, context, oauthflow);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The OAuth Flow Object must contain a valid URL"));
    }

    @Test
    public void testOAuthFlowWithRelativeAuthorizationUrl() {
        OAuthFlowValidator validator = OAuthFlowValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OAuthFlowImpl oauthflow = new OAuthFlowImpl();
        oauthflow.setAuthorizationUrl("/relativeUrl-example");
        oauthflow.addScope("test_scope", "read:test");

        validator.validate(vh, context, oauthflow);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testOAuthFlowWithInvalidTokenUrl() {
        OAuthFlowValidator validator = OAuthFlowValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OAuthFlowImpl oauthflow = new OAuthFlowImpl();
        oauthflow.setTokenUrl(":invalidUrl-example");
        oauthflow.addScope("test_scope", "read:test");

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
        oauthflow.addScope("test_scope", "read:test");

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
        oauthflow.addScope("test_scope", "read:test");

        validator.validate(vh, context, oauthflow);
        Assert.assertEquals(0, vh.getEventsSize());
    }
}
