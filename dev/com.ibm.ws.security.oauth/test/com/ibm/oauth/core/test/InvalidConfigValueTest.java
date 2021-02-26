/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.test;

import com.ibm.oauth.core.api.OAuthComponentFactory;
import com.ibm.oauth.core.api.OAuthComponentInstance;
import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.config.OAuthComponentConfigurationConstants;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.test.base.BaseConfig;
import com.ibm.oauth.core.test.base.BaseTestCase;

/*
 * OAuthComponentConfiguration negative test cases
 */
public class InvalidConfigValueTest extends BaseTestCase {

    // Baseline validation to confirm healthy drivers
    public void testGoodDefaultConfiguration() {
        BaseConfig config = new BaseConfig();
        initializeOAuthFramework(config);

        try {
            OAuthResult result = processResourceRequestAttributes();
            assertNotNull(result);
            assertEquals(OAuthResult.STATUS_OK, result.getStatus());
        } catch (OAuthException e) {
            fail("got an exception: " + e);
            e.printStackTrace();
        }
    }

    public void testNullClientProvider() {
        BaseConfig config = new BaseConfig();
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_CLIENT_PROVIDER_CLASSNAME,
                null);
        try {
            OAuthComponentInstance testcomp = OAuthComponentFactory.getOAuthComponentInstance(config);
            testcomp.getOAuth20Component();
            fail("Expected exception not received");
        } catch (OAuthException oae) {
            // Exception received, test successful
        }
    }

    public void testBadClientProvider() {
        BaseConfig config = new BaseConfig();
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_CLIENT_PROVIDER_CLASSNAME,
                new String[] { "com.ibm.just.a.bad.class.name" });
        try {
            OAuthComponentInstance testcomp = OAuthComponentFactory.getOAuthComponentInstance(config);
            testcomp.getOAuth20Component();
            fail("Expected exception not received");
        } catch (OAuthException oae) {
            // Exception received, test successful
        }
    }

    public void testNullTokenCache() {
        BaseConfig config = new BaseConfig();
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_TOKEN_CACHE_CLASSNAME,
                null);
        try {
            OAuthComponentInstance testcomp = OAuthComponentFactory.getOAuthComponentInstance(config);
            testcomp.getOAuth20Component();
            fail("Expected exception not received");
        } catch (OAuthException oae) {
            // Exception received, test successful
        }
    }

    public void testBadTokenCache() {
        BaseConfig config = new BaseConfig();
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_TOKEN_CACHE_CLASSNAME,
                new String[] { "com.ibm.just.a.bad.class.name" });
        try {
            OAuthComponentInstance testcomp = OAuthComponentFactory.getOAuthComponentInstance(config);
            testcomp.getOAuth20Component();
            fail("Expected exception not received");
        } catch (OAuthException oae) {
            // Exception received, test successful
        }
    }

    public void testNullTokenTypeHandler() {
        BaseConfig config = new BaseConfig();
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_ACCESS_TOKENTYPEHANDLER_CLASSNAME,
                null);
        try {
            OAuthComponentInstance testcomp = OAuthComponentFactory.getOAuthComponentInstance(config);
            testcomp.getOAuth20Component();
            fail("Expected exception not received");
        } catch (OAuthException oae) {
            // Exception received, test successful
        }
    }

    public void testBadTokenTypeHandler() {
        BaseConfig config = new BaseConfig();
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_ACCESS_TOKENTYPEHANDLER_CLASSNAME,
                new String[] { "com.ibm.just.a.bad.class.name" });
        try {
            OAuthComponentInstance testcomp = OAuthComponentFactory.getOAuthComponentInstance(config);
            testcomp.getOAuth20Component();
            fail("Expected exception not received");
        } catch (OAuthException oae) {
            // Exception received, test successful
        }
    }

    public void testNullMediators() {
        // Skip test, the core currently provides a default mediator for null
    }

    public void testBadMediators() {
        BaseConfig config = new BaseConfig();
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_MEDIATOR_CLASSNAMES,
                new String[] { "com.ibm.just.a.bad.class.name",
                        "com.ibm.just.another.bad.class.name" });
        try {
            OAuthComponentInstance testcomp = OAuthComponentFactory.getOAuthComponentInstance(config);
            testcomp.getOAuth20Component();
            fail("Expected exception not received");
        } catch (OAuthException oae) {
            // Exception received, test successful
        }
    }

    public void testBadAuthGrantLifetime() {
        BaseConfig config = new BaseConfig();
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_MAX_AUTHORIZATION_GRANT_LIFETIME_SECONDS,
                new String[] { "-100" });
        try {
            OAuthComponentInstance testcomp = OAuthComponentFactory.getOAuthComponentInstance(config);
            testcomp.getOAuth20Component();
            fail("Expected exception not received");
        } catch (OAuthException oae) {
            // Exception received, test successful
        }
    }

    public void testBadCodeLifetime() {
        BaseConfig config = new BaseConfig();
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_CODE_LIFETIME_SECONDS,
                new String[] { "-100" });
        try {
            OAuthComponentInstance testcomp = OAuthComponentFactory.getOAuthComponentInstance(config);
            testcomp.getOAuth20Component();
            fail("Expected exception not received");
        } catch (OAuthException oae) {
            // Exception received, test successful
        }
    }

    public void testBadCodeLength() {
        BaseConfig config = new BaseConfig();
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_CODE_LENGTH,
                new String[] { "-100" });
        try {
            OAuthComponentInstance testcomp = OAuthComponentFactory.getOAuthComponentInstance(config);
            testcomp.getOAuth20Component();
            fail("Expected exception not received");
        } catch (OAuthException oae) {
            // Exception received, test successful
        }
    }

    public void testBadTokenLifetime() {
        BaseConfig config = new BaseConfig();
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_TOKEN_LIFETIME_SECONDS,
                new String[] { "-100" });
        try {
            OAuthComponentInstance testcomp = OAuthComponentFactory.getOAuthComponentInstance(config);
            testcomp.getOAuth20Component();
            fail("Expected exception not received");
        } catch (OAuthException oae) {
            // Exception received, test successful
        }
    }

    public void testBadTokenLength() {
        BaseConfig config = new BaseConfig();
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_ACCESS_TOKEN_LENGTH,
                new String[] { "-100" });
        try {
            OAuthComponentInstance testcomp = OAuthComponentFactory.getOAuthComponentInstance(config);
            testcomp.getOAuth20Component();
            fail("Expected exception not received");
        } catch (OAuthException oae) {
            // Exception received, test successful
        }
    }

    public void testBadRefreshTokenLength() {
        BaseConfig config = new BaseConfig();
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_REFRESH_TOKEN_LENGTH,
                new String[] { "-100" });
        try {
            OAuthComponentInstance testcomp = OAuthComponentFactory.getOAuthComponentInstance(config);
            testcomp.getOAuth20Component();
            fail("Expected exception not received");
        } catch (OAuthException oae) {
            // Exception received, test successful
        }
    }

    public void testBadIssueRefreshToken() {
        // nothing to test, api always returns true or false or exception
    }

    public void testBadAllowPublicClients() {
        // nothing to test, api always returns true or false or exception
    }

}
