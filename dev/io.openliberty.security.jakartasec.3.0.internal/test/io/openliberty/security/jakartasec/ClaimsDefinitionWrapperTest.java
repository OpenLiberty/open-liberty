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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;

/**
 * Verify that the {@link ClaimsDefinitionWrapper} provides proper support for
 * retrieving and evaluating both EL expressions and literal settings as called for in Jakarta Security 3.0.
 */
public class ClaimsDefinitionWrapperTest {

    private Map<String, Object> overrides;

    private static final String STRING_EL_EXPRESSION = "#{'blah'.concat('blah')}";
    private static final String EVALUATED_EL_EXPRESSION_STRING_RESULT = "blahblah";

    @Before
    public void setUp() {
        overrides = new HashMap<String, Object>();
    }

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

    @Test
    public void testGetCallerNameClaim_EL() {
        overrides.put(TestClaimsDefinition.CALLER_NAME_CLAIM, STRING_EL_EXPRESSION);

        ClaimsDefinition claimsDefinition = TestClaimsDefinition.getInstanceofAnnotation(overrides);
        ClaimsDefinitionWrapper wrapper = new ClaimsDefinitionWrapper(claimsDefinition);

        assertEquals(EVALUATED_EL_EXPRESSION_STRING_RESULT, wrapper.getCallerNameClaim());
    }

    @Test
    public void testGetCallerGroupsClaim_EL() {
        overrides.put(TestClaimsDefinition.CALLER_GROUPS_CLAIM, STRING_EL_EXPRESSION);

        ClaimsDefinition claimsDefinition = TestClaimsDefinition.getInstanceofAnnotation(overrides);
        ClaimsDefinitionWrapper wrapper = new ClaimsDefinitionWrapper(claimsDefinition);

        assertEquals(EVALUATED_EL_EXPRESSION_STRING_RESULT, wrapper.getCallerGroupsClaim());
    }

}