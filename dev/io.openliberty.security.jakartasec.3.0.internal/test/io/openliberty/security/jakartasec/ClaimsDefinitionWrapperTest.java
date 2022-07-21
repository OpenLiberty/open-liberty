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

import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;

/**
 * Verify that the {@link ClaimsDefinitionWrapper} provides proper support for
 * retrieving and evaluating both EL expressions and literal settings as called for in Jakarta Security 3.0.
 */
public class ClaimsDefinitionWrapperTest {

    @Test
    public void testGetCallerNameClaim() {
        ClaimsDefinition claimsDefinition = TestClaimsDefinition.getInstanceofAnnotation(null);
        ClaimsDefinitionWrapper wrapper = new ClaimsDefinitionWrapper(claimsDefinition);

        assertEquals(TestClaimsDefinition.CALLER_NAME_CLAIM_DEFAULT, wrapper.getCallerNameClaim());
    }

    @Test
    public void testGetCallerGroupsClaim() {
        ClaimsDefinition claimsDefinition = TestClaimsDefinition.getInstanceofAnnotation(null);
        ClaimsDefinitionWrapper wrapper = new ClaimsDefinitionWrapper(claimsDefinition);

        assertEquals(TestClaimsDefinition.CALLER_GROUPS_CLAIM_DEFAULT, wrapper.getCallerGroupsClaim());
    }

}