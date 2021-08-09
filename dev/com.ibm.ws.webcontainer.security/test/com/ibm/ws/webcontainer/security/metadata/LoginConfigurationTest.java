/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.metadata;

import static org.junit.Assert.assertEquals;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.webcontainer.security.metadata.FormLoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.LoginConfigurationImpl;

import test.common.SharedOutputManager;

/**
 *
 */
public class LoginConfigurationTest {

    private static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final static String REALM_NAME = "WebRealm";

    private final Mockery mockery = new JUnit4Mockery();
    private final FormLoginConfiguration FORM_LOGIN_CONFIG = mockery.mock(FormLoginConfiguration.class);
    private LoginConfiguration loginConfiguration = new LoginConfigurationImpl(LoginConfiguration.FORM, REALM_NAME, FORM_LOGIN_CONFIG);

    @Test
    public void getAuthenticationMethod() {
        String actualAuthenticationMethod = loginConfiguration.getAuthenticationMethod();
        assertEquals("The authentication method must be the same as the one used in the constructor.",
                     LoginConfiguration.FORM, actualAuthenticationMethod);
    }

    @Test
    public void getAuthenticationMethod_CLIENT_DASH_CERT() {
        loginConfiguration = new LoginConfigurationImpl(LoginConfigurationImpl.CLIENT_CERT_AUTH_METHOD, REALM_NAME, FORM_LOGIN_CONFIG);
        String actualAuthenticationMethod = loginConfiguration.getAuthenticationMethod();
        assertEquals("The CLIENT-CERT authentication method must be converted to CLIENT_CERT",
                     LoginConfiguration.CLIENT_CERT, actualAuthenticationMethod);
    }

    @Test
    public void getAuthenticationMethod_CLIENT_UNDERSCORE_CERT() {
        loginConfiguration = new LoginConfigurationImpl(LoginConfiguration.CLIENT_CERT, REALM_NAME, FORM_LOGIN_CONFIG);
        String actualAuthenticationMethod = loginConfiguration.getAuthenticationMethod();
        assertEquals("The CLIENT_CERT authentication method value should stay as CLIENT_CERT",
                     LoginConfiguration.CLIENT_CERT, actualAuthenticationMethod);
    }

    @Test
    public void getRealmName() {
        String actualRealmName = loginConfiguration.getRealmName();
        assertEquals("The realm name must be the same as the one used in the constructor.",
                         REALM_NAME, actualRealmName);
    }

    @Test
    public void getFormLoginConfiguration() {
        FormLoginConfiguration actualFormLoginConfig = loginConfiguration.getFormLoginConfiguration();
        assertEquals("The form login configuration must be the same as the one used in the constructor.",
                         FORM_LOGIN_CONFIG, actualFormLoginConfig);
    }

}
