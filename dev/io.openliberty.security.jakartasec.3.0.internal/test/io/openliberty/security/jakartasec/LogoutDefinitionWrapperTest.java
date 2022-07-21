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

import org.junit.Test;

import jakarta.security.enterprise.authentication.mechanism.http.openid.LogoutDefinition;

/**
 * Verify that the {@link LogoutDefinitionWrapper} provides proper support for
 * retrieving and evaluating both EL expressions and literal settings as called for in Jakarta Security 3.0.
 */
public class LogoutDefinitionWrapperTest {

    @Test
    public void testIsNotifyProvider() {
        LogoutDefinition logoutDefinition = TestLogoutDefinition.getInstanceofAnnotation(null);
        LogoutDefinitionWrapper wrapper = new LogoutDefinitionWrapper(logoutDefinition);

        assertEquals(false, wrapper.isNotifyProvider());
    }

    @Test
    public void testGetRedirectURI() {
        LogoutDefinition logoutDefinition = TestLogoutDefinition.getInstanceofAnnotation(null);
        LogoutDefinitionWrapper wrapper = new LogoutDefinitionWrapper(logoutDefinition);

        assertEquals(TestLogoutDefinition.EMPTY_DEFAULT, wrapper.getRedirectURI());
    }

    @Test
    public void testIsAccessTokenExpiry() {
        LogoutDefinition logoutDefinition = TestLogoutDefinition.getInstanceofAnnotation(null);
        LogoutDefinitionWrapper wrapper = new LogoutDefinitionWrapper(logoutDefinition);

        assertEquals(false, wrapper.isAccessTokenExpiry());
    }

    @Test
    public void testIsIdentityTokenExpiry() {
        LogoutDefinition logoutDefinition = TestLogoutDefinition.getInstanceofAnnotation(null);
        LogoutDefinitionWrapper wrapper = new LogoutDefinitionWrapper(logoutDefinition);

        assertEquals(false, wrapper.isIdentityTokenExpiry());
    }

}