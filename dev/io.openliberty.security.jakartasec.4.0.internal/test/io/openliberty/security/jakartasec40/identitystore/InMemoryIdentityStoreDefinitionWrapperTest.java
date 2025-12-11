/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec40.identitystore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.ws.security.javaeesec.JavaEESecConstants;

import io.openliberty.security.jakartasec40.JakartaSec40Constants;
import io.openliberty.security.jakartasec40.Utils;
import io.openliberty.security.jakartasec40.identitystore.InMemoryIdentityStoreDefinitionWrapper.CredentialValue;
import jakarta.security.enterprise.identitystore.IdentityStore.ValidationType;
import jakarta.security.enterprise.identitystore.InMemoryIdentityStoreDefinition;

/**
 * Unit tests for InMemoryIdentityStoreDefinitionWrapper
 */
public class InMemoryIdentityStoreDefinitionWrapperTest {

    // Test constants
    private static final String CALLER_NAME_1 = "sally";
    private static final String CALLER_NAME_2 = "bob";
    private static final String CALLER_NAME_3 = "dave";
    private static final String CALLER_NAME_4 = "jasmine";
    private static final String CALLER_NAME_5 = "frank";
    private static final String CALLER_NAME_6 = "lisa";
    // plain
    private static final String PASSWORD_1 = "X@FnF=A-+!*&Y?aq";
    private static final String PASSWORD_2 = "1N3@E2E3j6q!n93?";
    // XOR and AES-256 encoded
    private static final String PASSWORD_3 = "{xor}LDo8LTorbg==";
    private static final String PASSWORD_4 = "{aes}ARCouzGAv4MX4oLF6H0hSWvR5xafumEimEcpnEqbSd4BRXTwrXKPgM5vbLJLbjo+gaGDTsYNIWPFTlJV6/aUWumPWIabZS4nMbXBKs4uJZSWKqlYpVkVZGsJIKpPzVPdIsyojFpKrigJIw==";
    private static final String PASSWORD_5 = "{hash}ARAAAAAUUEJLREYyV2l0aEhtYWNTSEE1MTIwAAAAIB3ucqrpuU/HRoTavqupAIVF93x072TZdYCfQYTuIHxwQAAAACBE4WJ2FFa/6YT1FGSWY5Xl77xh9ORYVbostiY6KCtxxw==";

    // XOR, but bad encoding
    private static final String PASSWORD_6 = "{xor}XXXXXLDo8LTorbg==";

    private static final String[] GROUPS_1 = { "caller", "user" };
    private static final String[] GROUPS_2 = { "admin" };

    private static final int PRIORITY_VALUE = 33;
    private static final String PRIORITY_EXPRESSION_VALUE = "";
    private static final ValidationType[] USE_FOR_VALUES = { ValidationType.VALIDATE, ValidationType.PROVIDE_GROUPS };
    private static final String USE_FOR_EXPRESSION_VALUE = "";

    private InMemoryIdentityStoreDefinitionWrapper wrapper;

    @Before
    public void setUp() {
        InMemoryIdentityStoreDefinition defaultDefinition;
        InMemoryIdentityStoreDefinition.Credentials credential_1;
        InMemoryIdentityStoreDefinition.Credentials credential_2;
        InMemoryIdentityStoreDefinition.Credentials credential_3;
        InMemoryIdentityStoreDefinition.Credentials credential_4;
        InMemoryIdentityStoreDefinition.Credentials credential_5;
        InMemoryIdentityStoreDefinition.Credentials credential_6;
        InMemoryIdentityStoreDefinition.Credentials[] value = new InMemoryIdentityStoreDefinition.Credentials[6];

        Map<String, Object> identityStoreProperties = new HashMap<String, Object>();
        Map<String, Object> credentialProperties_1 = new HashMap<String, Object>();
        Map<String, Object> credentialProperties_2 = new HashMap<String, Object>();
        Map<String, Object> credentialProperties_3 = new HashMap<String, Object>();
        Map<String, Object> credentialProperties_4 = new HashMap<String, Object>();
        Map<String, Object> credentialProperties_5 = new HashMap<String, Object>();
        Map<String, Object> credentialProperties_6 = new HashMap<String, Object>();

        identityStoreProperties.put(JavaEESecConstants.PRIORITY, PRIORITY_VALUE);
        identityStoreProperties.put(JavaEESecConstants.PRIORITY_EXPRESSION, PRIORITY_EXPRESSION_VALUE);
        identityStoreProperties.put(JavaEESecConstants.USE_FOR, USE_FOR_VALUES);
        identityStoreProperties.put(JavaEESecConstants.USE_FOR_EXPRESSION, USE_FOR_EXPRESSION_VALUE);

        credentialProperties_1.put(Utils.CALLER_NAME_NAME, CALLER_NAME_1);
        credentialProperties_1.put(Utils.PASSWORD_NAME, PASSWORD_1);
        credentialProperties_1.put(Utils.GROUPS_NAME, GROUPS_1);
        credential_1 = Utils.getInstanceOfCredentialsAnnotation(credentialProperties_1);
        value[0] = credential_1;

        credentialProperties_2.put(Utils.CALLER_NAME_NAME, CALLER_NAME_2);
        credentialProperties_2.put(Utils.PASSWORD_NAME, PASSWORD_2);
        credentialProperties_2.put(Utils.GROUPS_NAME, GROUPS_2);
        credential_2 = Utils.getInstanceOfCredentialsAnnotation(credentialProperties_2);
        value[1] = credential_2;

        credentialProperties_3.put(Utils.CALLER_NAME_NAME, CALLER_NAME_3);
        credentialProperties_3.put(Utils.PASSWORD_NAME, PASSWORD_3);
        credential_3 = Utils.getInstanceOfCredentialsAnnotation(credentialProperties_3);
        value[2] = credential_3;

        credentialProperties_4.put(Utils.CALLER_NAME_NAME, CALLER_NAME_4);
        credentialProperties_4.put(Utils.PASSWORD_NAME, PASSWORD_4);
        credential_4 = Utils.getInstanceOfCredentialsAnnotation(credentialProperties_4);
        value[3] = credential_4;

        credentialProperties_5.put(Utils.CALLER_NAME_NAME, CALLER_NAME_5);
        credentialProperties_5.put(Utils.PASSWORD_NAME, PASSWORD_5);
        credential_5 = Utils.getInstanceOfCredentialsAnnotation(credentialProperties_5);
        value[4] = credential_5;

        credentialProperties_6.put(Utils.CALLER_NAME_NAME, CALLER_NAME_6);
        credentialProperties_6.put(Utils.PASSWORD_NAME, PASSWORD_6);
        credential_6 = Utils.getInstanceOfCredentialsAnnotation(credentialProperties_6);
        value[5] = credential_6;

        identityStoreProperties.put(JakartaSec40Constants.VALUE, value);

        defaultDefinition = Utils.getInstanceOfInMemoryAnnotation(identityStoreProperties);
        wrapper = new InMemoryIdentityStoreDefinitionWrapper(defaultDefinition);
    }

    /**
     * Test that constructor throws IllegalArgumentException when null definition is provided
     */
    @Test
    public void testConstructorWithNullDefinition() {
        try {
            new InMemoryIdentityStoreDefinitionWrapper(null);
            fail("Expected IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            assertEquals("The InMemoryIdentityStoreDefinition cannot be null.", e.getMessage());
        }
    }

    /**
     * Test getPriority returns the expected priority value
     */
    @Test
    public void testGetPriority() {
        assertEquals("Priority value should match the configured value",
                     PRIORITY_VALUE, wrapper.getPriority());
    }

    /**
     * Test getUseFor returns the expected ValidationType values
     */
    @Test
    public void testGetUseFor() {
        Set<ValidationType> expectedUseFor = new HashSet<>(Arrays.asList(USE_FOR_VALUES));
        assertEquals("UseFor values should match the configured values",
                     expectedUseFor, wrapper.getUseFor());
    }

    /**
     * Test getGroups returns the expected values
     */
    @Test
    public void testGetGroups() {
        CredentialValue credential = wrapper.getCredential(CALLER_NAME_1);
        assertNotNull("CredentialValue should not be null", credential);

        String[] groups1 = credential.getGroups();
        assertEquals("Group count for " + CALLER_NAME_1 + " should match",
                     GROUPS_1.length, groups1.length);
        for (int i = 0; i < GROUPS_1.length; i++) {
            assertEquals("Group at index " + i + " for " + CALLER_NAME_1 + " should match",
                         GROUPS_1[i], groups1[i]);
        }

        // Test groups for second user
        credential = wrapper.getCredential(CALLER_NAME_2);
        assertNotNull("CredentialValue should not be null", credential);

        String[] groups2 = credential.getGroups();
        assertEquals("Group count for " + CALLER_NAME_2 + " should match",
                     GROUPS_2.length, groups2.length);
        for (int i = 0; i < GROUPS_2.length; i++) {
            assertEquals("Group at index " + i + " for " + CALLER_NAME_2 + " should match",
                         GROUPS_2[i], groups2[i]);
        }
    }

    /**
     * Test password validation using reflection to access private methods
     */
    @Test
    public void testPasswordValidation() throws Exception {
        CredentialValue credValue = wrapper.getCredential(CALLER_NAME_1);

        assertNotNull("credential value for caller [" + CALLER_NAME_1 + "] should not be null.", credValue);

        // Test with correct password
        ProtectedString correctPassword = new ProtectedString(PASSWORD_1.toCharArray());
        Boolean result = credValue.validate(correctPassword);
        assertTrue("Password validation should succeed with correct password", result);

        // Test with incorrect password
        ProtectedString incorrectPassword = new ProtectedString("wrongpassword".toCharArray());
        result = credValue.validate(incorrectPassword);
        assertFalse("Password validation should fail with incorrect password", result);

        // Test with null password
        result = credValue.validate(null);
        assertFalse("Password validation should fail with null password", result);

        // Test with empty password
        result = credValue.validate(new ProtectedString("".toCharArray()));
        assertFalse("Password validation should fail with empty password", result);

        final ProtectedString hiddenPassword = new ProtectedString("secret1".toCharArray());

        credValue = wrapper.getCredential(CALLER_NAME_3);
        result = credValue.validate(hiddenPassword);
        assertTrue("Password validation should succeed with correct password", result);

        credValue = wrapper.getCredential(CALLER_NAME_4);
        result = credValue.validate(hiddenPassword);
        assertTrue("Password validation should succeed with correct password", result);

        credValue = wrapper.getCredential(CALLER_NAME_5);
        result = credValue.validate(hiddenPassword);
        assertTrue("Password validation should succeed with correct password", result);

        credValue = wrapper.getCredential(CALLER_NAME_6);
        result = credValue.validate(hiddenPassword);
        assertFalse("Password validation should fail with bad XOR encoding", result);
    }

    /**
     * Test default useFor values when not specified
     */
    @Test
    public void testDefaultUseFor() {
        InMemoryIdentityStoreDefinition defaultDefinition = Utils.getInstanceOfInMemoryAnnotation(null);

        InMemoryIdentityStoreDefinitionWrapper defaultWrapper = new InMemoryIdentityStoreDefinitionWrapper(defaultDefinition);
        Set<ValidationType> useFor = defaultWrapper.getUseFor();

        assertNotNull("UseFor set should not be null", useFor);
        assertEquals("UseFor should contain default validation types",
                     Set.of(JakartaSec40Constants.SPEC_DEFAULT_USEFOR), useFor);
    }

    @Test
    public void testDefaultPriority() {

        InMemoryIdentityStoreDefinition defaultDefinition = Utils.getInstanceOfInMemoryAnnotation(null);

        InMemoryIdentityStoreDefinitionWrapper defaultWrapper = new InMemoryIdentityStoreDefinitionWrapper(defaultDefinition);
        int priority = defaultWrapper.getPriority();

        assertEquals("Priority should be the default value",
                     JakartaSec40Constants.SPEC_DEFAULT_PRIORITY, priority);
    }
}
