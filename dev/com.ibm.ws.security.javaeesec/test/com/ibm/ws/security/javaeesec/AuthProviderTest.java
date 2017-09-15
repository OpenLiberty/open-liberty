/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.security.auth.Subject;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.ServerAuthConfig;
import javax.security.auth.message.config.ServerAuthContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.jaspi.JaspiMessageInfo;

/**
 * Test AuthProvider, AuthConfig, and AuthContext, and AuthModule.
 */
public class AuthProviderTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static final String LAYER = "HttpServlet";
    private static final String APP_CONTEXT = "localhost /contextRoot";
    private static final String IS_MANDATORY_POLICY = "javax.security.auth.message.MessagePolicy.isMandatory";

    private AuthProvider authProvider;
    private Map<String, String> props;
    private AuthConfigFactory authConfigFactory;

    @Before
    public void setUp() throws Exception {
        props = new ConcurrentHashMap<String, String>();
        authConfigFactory = mockery.mock(AuthConfigFactory.class);
        authProvider = new AuthProvider(props, authConfigFactory);
    }

    @Test
    public void testGetServerAuthConfig() throws Exception {
        ServerAuthConfig authConfig = authProvider.getServerAuthConfig(LAYER, APP_CONTEXT, null);
        assertNotNull("There must be a ServerAuthConfig instance for the application context.", authConfig);
        assertEquals("The ServerAuthConfig's message layer must be set.", LAYER, authConfig.getMessageLayer());
        assertTrue("The ServerAuthConfig must be protected.", authConfig.isProtected());
    }

    @Test
    public void testServerAuthConfigNotMandatory() throws Exception {
        ServerAuthConfig authConfig = authProvider.getServerAuthConfig(LAYER, APP_CONTEXT, null);
        MessageInfo messageInfo = createMessageInfo(false);
        assertEquals("The ServerAuthConfig's auth context ID must be JASPI_UNPROTECTED.", "JASPI_UNPROTECTED", authConfig.getAuthContextID(messageInfo));
    }

    @Test
    public void testServerAuthConfigMandatory() throws Exception {
        ServerAuthConfig authConfig = authProvider.getServerAuthConfig(LAYER, APP_CONTEXT, null);
        MessageInfo messageInfo = createMessageInfo(true);
        assertEquals("The ServerAuthConfig's auth context ID must be JASPI_PROTECTED.", "JASPI_PROTECTED", authConfig.getAuthContextID(messageInfo));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetClientAuthConfig() throws Exception {
        authProvider.getClientAuthConfig(LAYER, APP_CONTEXT, null);
    }

    @Test
    public void testGetAuthContext() throws Exception {
        ServerAuthConfig authConfig = authProvider.getServerAuthConfig(LAYER, APP_CONTEXT, null);
        MessageInfo messageInfo = createMessageInfo(true);
        String authContextID = authConfig.getAuthContextID(messageInfo);

        Subject serviceSubject = null;
        Map<String, String> authContextProps = new HashMap<String, String>(); // TODO: Add jacc policy context
        ServerAuthContext authContext = authConfig.getAuthContext(authContextID, serviceSubject, authContextProps);
        assertNotNull("There must be a ServerAuthContext instance.", authContext);
    }

    private MessageInfo createMessageInfo(boolean mandatory) {
        HttpServletRequest req = mockery.mock(HttpServletRequest.class);
        HttpServletResponse rsp = mockery.mock(HttpServletResponse.class);
        MessageInfo messageInfo = new JaspiMessageInfo(req, rsp);
        messageInfo.getMap().put(IS_MANDATORY_POLICY, Boolean.toString(mandatory));
        return messageInfo;
    }
}
