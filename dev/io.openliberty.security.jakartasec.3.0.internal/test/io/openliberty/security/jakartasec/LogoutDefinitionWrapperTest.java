/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.security.jakartasec;

import static io.openliberty.security.jakartasec.JakartaSec30Constants.EMPTY_DEFAULT;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import jakarta.security.enterprise.authentication.mechanism.http.openid.LogoutDefinition;
import test.common.SharedOutputManager;

/**
 * Verify that the {@link LogoutDefinitionWrapper} provides proper support for
 * retrieving and evaluating both EL expressions and literal settings as called for in Jakarta Security 3.0.
 */
public class LogoutDefinitionWrapperTest {

    private Map<String, Object> overrides;

    private static final String STRING_EL_EXPRESSION = "#{'blah'.concat('blah')}";
    private static final String EVALUATED_EL_EXPRESSION_STRING_RESULT = "blahblah";

    SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all");
    @Rule
    public TestRule outputRule = outputMgr;

    @Before
    public void setUp() {
        overrides = new HashMap<String, Object>();
    }

    @Test
    public void testIsNotifyProvider() {
        LogoutDefinition logoutDefinition = TestLogoutDefinition.getInstanceofAnnotation(null);
        LogoutDefinitionWrapper wrapper = new LogoutDefinitionWrapper(logoutDefinition);

        assertEquals(false, wrapper.isNotifyProvider());
    }

    /**
     * Test that the EL option works to resolve a boolean value for notifyProvider
     */
    @Test
    public void testIsNotifyProvider_EL() {
        overrides.put(TestLogoutDefinition.NOTIFY_PROVIDER_EXPRESSION, "#{ 9 > 6}");

        LogoutDefinition logoutDefinition = TestLogoutDefinition.getInstanceofAnnotation(overrides);
        LogoutDefinitionWrapper wrapper = new LogoutDefinitionWrapper(logoutDefinition);

        assertEquals(true, wrapper.isNotifyProvider());
    }

    @Test
    public void testGetRedirectURI() {
        LogoutDefinition logoutDefinition = TestLogoutDefinition.getInstanceofAnnotation(null);
        LogoutDefinitionWrapper wrapper = new LogoutDefinitionWrapper(logoutDefinition);

        assertEquals(EMPTY_DEFAULT, wrapper.getRedirectURI());
    }

    /**
     * Test that we can resolve an EL for the the redirectURI
     */
    @Test
    public void testGetRedirectURI_EL() {
        overrides.put(TestLogoutDefinition.REDIRECT_URI, STRING_EL_EXPRESSION);

        LogoutDefinition logoutDefinition = TestLogoutDefinition.getInstanceofAnnotation(overrides);
        LogoutDefinitionWrapper wrapper = new LogoutDefinitionWrapper(logoutDefinition);

        assertEquals(EVALUATED_EL_EXPRESSION_STRING_RESULT, wrapper.getRedirectURI());
    }

    @Test
    public void testIsAccessTokenExpiry() {
        LogoutDefinition logoutDefinition = TestLogoutDefinition.getInstanceofAnnotation(null);
        LogoutDefinitionWrapper wrapper = new LogoutDefinitionWrapper(logoutDefinition);

        assertEquals(false, wrapper.isAccessTokenExpiry());
    }

    /**
     * Test that the EL option works to resolve a boolean value for notifyProvider
     */
    @Test
    public void testIsAccessTokenExpiry_EL() {
        overrides.put(TestLogoutDefinition.ACCESS_TOKEN_EXPIRY_EXPRESSION, "#{ 9 > 6}");

        LogoutDefinition logoutDefinition = TestLogoutDefinition.getInstanceofAnnotation(overrides);
        LogoutDefinitionWrapper wrapper = new LogoutDefinitionWrapper(logoutDefinition);

        assertEquals(true, wrapper.isAccessTokenExpiry());
    }

    /**
     * Test with an expression that resolves to a non-Boolean value. We should default to false.
     */
    @Test
    public void testIsAccessTokenExpiryBadValue() {
        overrides.put(TestLogoutDefinition.ACCESS_TOKEN_EXPIRY_EXPRESSION, STRING_EL_EXPRESSION);

        LogoutDefinition logoutDefinition = TestLogoutDefinition.getInstanceofAnnotation(overrides);
        LogoutDefinitionWrapper wrapper = new LogoutDefinitionWrapper(logoutDefinition);

        assertEquals(false, wrapper.isAccessTokenExpiry());
    }

    @Test
    public void testIsIdentityTokenExpiry() {
        LogoutDefinition logoutDefinition = TestLogoutDefinition.getInstanceofAnnotation(null);
        LogoutDefinitionWrapper wrapper = new LogoutDefinitionWrapper(logoutDefinition);

        assertEquals(false, wrapper.isIdentityTokenExpiry());
    }

    /**
     * Test that the EL option works to resolve a boolean value for notifyProvider
     */
    @Test
    public void testIsIdentityTokenExpiry_EL() {
        overrides.put(TestLogoutDefinition.IDENTITY_TOKEN_EXPIRY_EXPRESSION, "#{ 9 > 6}");

        LogoutDefinition logoutDefinition = TestLogoutDefinition.getInstanceofAnnotation(overrides);
        LogoutDefinitionWrapper wrapper = new LogoutDefinitionWrapper(logoutDefinition);

        assertEquals(true, wrapper.isIdentityTokenExpiry());
    }

}