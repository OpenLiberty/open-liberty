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
package com.ibm.ws.security.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

/**
 *
 */
public class SecurityConfigurationTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private SecurityConfiguration secConfig;

    @Before
    public void setUp() {
        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(SecurityConfiguration.CFG_KEY_AUTHENTICATION_REF, "authenticationService");
        properties.put(SecurityConfiguration.CFG_KEY_AUTHORIZATION_REF, "authorizationService");
        properties.put(SecurityConfiguration.CFG_KEY_USERREGISTRY_REF, "userRegistryService");
        secConfig = new SecurityConfiguration();
        secConfig.activate(properties);
    }

    @After
    public void tearDown() {
        secConfig.deactivate();
        secConfig = null;
    }

    /**
     * @param service
     * @param e
     */
    private void handleExpectedExceptionAndMessage(String service, IllegalArgumentException e) {
        assertEquals("The exception should be a fully variably subsituted message of the right type",
                     "CWWKS0000E: A configuration exception has occurred. No " + service
                                                                                                      + " attribute is defined for a <securityConfiguration> element.",
                     e.getMessage());
        assertTrue("Expected error message was not logged",
                   outputMgr.checkForStandardErr("CWWKS0000E: A configuration exception has occurred. No " + service
                                                 + " attribute is defined for a <securityConfiguration> element."));
    }

    /**
     * Drive activation of the SecurityConfiguration and validate that it
     * results in an IllegalArgumentException, because {@code service} is {@code reason} from the {@code properties}.
     *
     * @param properties
     * @param service
     * @param reason
     */
    private void activateShouldFailWith(final Map<String, Object> properties, String service, String reason) {
        SecurityConfiguration secConfig = new SecurityConfiguration();
        try {
            secConfig.activate(properties);
            fail("Should have thrown an IllegalArgumentException since the " + service + " is " + reason);
        } catch (IllegalArgumentException e) {
            handleExpectedExceptionAndMessage(service, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityConfiguration#activate(org.osgi.service.component.ComponentContext)}.
     */
    @Test
    public void activateWithoutAuthentication() {
        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(SecurityConfiguration.CFG_KEY_AUTHORIZATION_REF, "authorizationService");
        properties.put(SecurityConfiguration.CFG_KEY_USERREGISTRY_REF, "userRegistryService");

        activateShouldFailWith(properties, SecurityConfiguration.CFG_KEY_AUTHENTICATION_REF, "missing");
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityConfiguration#activate(org.osgi.service.component.ComponentContext)}.
     */
    @Test
    public void activateEmptyAuthentication() {
        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(SecurityConfiguration.CFG_KEY_AUTHENTICATION_REF, "");
        properties.put(SecurityConfiguration.CFG_KEY_AUTHORIZATION_REF, "authorizationService");
        properties.put(SecurityConfiguration.CFG_KEY_USERREGISTRY_REF, "userRegistryService");

        activateShouldFailWith(properties, SecurityConfiguration.CFG_KEY_AUTHENTICATION_REF, "empty");
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityConfiguration#activate(org.osgi.service.component.ComponentContext)}.
     */
    @Test
    public void activateWithoutAuthorization() {
        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(SecurityConfiguration.CFG_KEY_AUTHENTICATION_REF, "authenticationService");
        properties.put(SecurityConfiguration.CFG_KEY_USERREGISTRY_REF, "userRegistryService");

        activateShouldFailWith(properties, SecurityConfiguration.CFG_KEY_AUTHORIZATION_REF, "missing");
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityConfiguration#activate(org.osgi.service.component.ComponentContext)}.
     */
    @Test
    public void activateEmptyAuthorization() {
        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(SecurityConfiguration.CFG_KEY_AUTHENTICATION_REF, "authenticationService");
        properties.put(SecurityConfiguration.CFG_KEY_AUTHORIZATION_REF, "");
        properties.put(SecurityConfiguration.CFG_KEY_USERREGISTRY_REF, "userRegistryService");

        activateShouldFailWith(properties, SecurityConfiguration.CFG_KEY_AUTHORIZATION_REF, "empty");
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityConfiguration#activate(org.osgi.service.component.ComponentContext)}.
     */
    @Test
    public void activateWithoutUserRegistry() {
        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(SecurityConfiguration.CFG_KEY_AUTHENTICATION_REF, "authenticationService");
        properties.put(SecurityConfiguration.CFG_KEY_AUTHORIZATION_REF, "authorizationService");

        activateShouldFailWith(properties, SecurityConfiguration.CFG_KEY_USERREGISTRY_REF, "missing");
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityConfiguration#activate(org.osgi.service.component.ComponentContext)}.
     */
    @Test
    public void activateEmptyUserRegistry() {
        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(SecurityConfiguration.CFG_KEY_AUTHENTICATION_REF, "authenticationService");
        properties.put(SecurityConfiguration.CFG_KEY_AUTHORIZATION_REF, "authorizationService");
        properties.put(SecurityConfiguration.CFG_KEY_USERREGISTRY_REF, "");

        activateShouldFailWith(properties, SecurityConfiguration.CFG_KEY_USERREGISTRY_REF, "empty");
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityConfiguration#activate(org.osgi.service.component.ComponentContext)}.
     */
    @Test
    public void activateWithAllConfig() {
        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(SecurityConfiguration.CFG_KEY_AUTHENTICATION_REF, "authenticationService");
        properties.put(SecurityConfiguration.CFG_KEY_AUTHORIZATION_REF, "authorizationService");
        properties.put(SecurityConfiguration.CFG_KEY_USERREGISTRY_REF, "userRegistryService");

        SecurityConfiguration secConfig = new SecurityConfiguration();
        secConfig.activate(properties);
    }

    /**
     * Drive modification of the SecurityConfiguration and validate that it
     * results in an IllegalArgumentException, because {@code service} is {@code reason} from the {@code properties}.
     *
     * @param properties
     * @param service
     * @param reason
     */
    private void modifyShouldFailWith(final Map<String, Object> properties, String service, String reason) {
        try {
            secConfig.modify(properties);
            fail("Should have thrown an IllegalArgumentException since the " + service + " is " + reason);
        } catch (IllegalArgumentException e) {
            handleExpectedExceptionAndMessage(service, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityConfiguration#modify(java.util.Map)}.
     */
    @Test
    public void modifyWithoutAuthentication() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SecurityConfiguration.CFG_KEY_AUTHORIZATION_REF, "authorizationService");
        props.put(SecurityConfiguration.CFG_KEY_USERREGISTRY_REF, "userRegistryService");

        modifyShouldFailWith(props, SecurityConfiguration.CFG_KEY_AUTHENTICATION_REF, "missing");
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityConfiguration#modify(java.util.Map)}.
     */
    @Test
    public void modifyEmptyAuthentication() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SecurityConfiguration.CFG_KEY_AUTHENTICATION_REF, "");
        props.put(SecurityConfiguration.CFG_KEY_AUTHORIZATION_REF, "authorizationService");
        props.put(SecurityConfiguration.CFG_KEY_USERREGISTRY_REF, "userRegistryService");

        modifyShouldFailWith(props, SecurityConfiguration.CFG_KEY_AUTHENTICATION_REF, "empty");
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityConfiguration#modify(java.util.Map)}.
     */
    @Test
    public void modifyWithoutAuthorization() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SecurityConfiguration.CFG_KEY_AUTHENTICATION_REF, "authenticationService");
        props.put(SecurityConfiguration.CFG_KEY_USERREGISTRY_REF, "userRegistryService");

        modifyShouldFailWith(props, SecurityConfiguration.CFG_KEY_AUTHORIZATION_REF, "missing");
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityConfiguration#modify(java.util.Map)}.
     */
    @Test
    public void modifyEmptyAuthorization() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SecurityConfiguration.CFG_KEY_AUTHENTICATION_REF, "authenticationService");
        props.put(SecurityConfiguration.CFG_KEY_AUTHORIZATION_REF, "");
        props.put(SecurityConfiguration.CFG_KEY_USERREGISTRY_REF, "userRegistryService");

        modifyShouldFailWith(props, SecurityConfiguration.CFG_KEY_AUTHORIZATION_REF, "empty");
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityConfiguration#modify(java.util.Map)}.
     */
    @Test
    public void modifyWithoutUserRegistry() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SecurityConfiguration.CFG_KEY_AUTHENTICATION_REF, "authenticationService");
        props.put(SecurityConfiguration.CFG_KEY_AUTHORIZATION_REF, "authorizationService");

        modifyShouldFailWith(props, SecurityConfiguration.CFG_KEY_USERREGISTRY_REF, "missing");
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityConfiguration#modify(java.util.Map)}.
     */
    @Test
    public void modifyEmptyUserRegistry() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SecurityConfiguration.CFG_KEY_AUTHENTICATION_REF, "authenticationService");
        props.put(SecurityConfiguration.CFG_KEY_AUTHORIZATION_REF, "authorizationService");
        props.put(SecurityConfiguration.CFG_KEY_USERREGISTRY_REF, "");

        modifyShouldFailWith(props, SecurityConfiguration.CFG_KEY_USERREGISTRY_REF, "empty");
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityConfiguration#modify(java.util.Map)}.
     */
    @Test
    public void modifyWithAllConfig() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SecurityConfiguration.CFG_KEY_AUTHENTICATION_REF, "authenticationService");
        props.put(SecurityConfiguration.CFG_KEY_AUTHORIZATION_REF, "authorizationService");
        props.put(SecurityConfiguration.CFG_KEY_USERREGISTRY_REF, "userRegistryService");
        secConfig.modify(props);
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityConfiguration#getAuthenticationServiceId()}.
     */
    @Test
    public void getAuthenticationServiceId() {
        assertEquals("Configured authentication id should be 'authenticationService'",
                     "authenticationService", secConfig.getAuthenticationServiceId());
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityConfiguration#getAuthorizationServiceId()}.
     */
    @Test
    public void getAuthorizationServiceId() {
        assertEquals("Configured authorization id should be 'authorizationService'",
                     "authorizationService", secConfig.getAuthorizationServiceId());
    }

    /**
     * Test method for {@link com.ibm.ws.security.internal.SecurityConfiguration#getUserRegistryServiceId()}.
     */
    @Test
    public void getUserRegistryServiceId() {
        assertEquals("Configured userRegistry id should be 'userRegistryService'",
                     "userRegistryService", secConfig.getUserRegistryServiceId());
    }

}
