/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import javax.security.auth.Subject;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.security.token.internal.SingleSignonTokenImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.internal.WebSecurityHelperImpl;
import com.ibm.wsspi.security.token.SingleSignonToken;

import test.common.SharedOutputManager;

/**
 *
 *
 */
public class OIDCBrowserStateUtilTest {
    private static String EXPECTED_OUTPUT = "MSk6/ePHZUtvvpdE3DgngS1neAkIctAwuVU5HYf97Vw=";
    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    final WebAppSecurityConfig wasc = mock.mock(WebAppSecurityConfig.class, "WebAppSecurityConfig");
    final SingleSignonToken ssot = mock.mock(SingleSignonToken.class, "SingleSignonToken");

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    @After
    public void tearDown() {
        outputMgr.resetStreams();
    }

    /**
     * test genereateOIDCBrowserState(String) method.
     ** input : null
     ** output : null
     **/
    @Test
    public void testGenerateOIDCBrowserStateNull() {
        assertNull(OIDCBrowserStateUtil.generateOIDCBrowserState(null));
    }

    /**
     * test genereateOIDCBrowserState(String) method.
     ** input : null
     ** output : null
     **/
    @Test
    public void testGenerateOIDCBrowserStateValid() {
        assertNotNull(OIDCBrowserStateUtil.generateOIDCBrowserState("test"));
    }

    /**
     * test genereateOIDCBrowserState(String) method.
     ** input : Invalid Subject.
     ** output : hash value which indicates UNAUTHENTICATED subject
     **/
    @Test
    public void testGenerateOIDCBrowserStateInvalidSubject() {
        WebSecurityHelperImpl.setWebAppSecurityConfig(wasc);
        Subject subject = new Subject();
        subject.getPrivateCredentials().add(new SingleSignonTokenImpl(null));
        subject.getPrivateCredentials().add(new SingleSignonTokenImpl(null));
        try {
            WSSubject.setRunAsSubject(subject);
            assertEquals(EXPECTED_OUTPUT, OIDCBrowserStateUtil.generateOIDCBrowserState(false));
        } catch (WSSecurityException e) {
            e.printStackTrace();
            String msg = e.getMessage();
            fail("Unexpected exception is caught." + msg);
        }
    }
}
