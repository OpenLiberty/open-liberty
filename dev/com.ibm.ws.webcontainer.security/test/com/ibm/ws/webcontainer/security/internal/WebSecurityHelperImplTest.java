/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.security.auth.Subject;
import javax.servlet.http.Cookie;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;

import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.web.WebSecurityHelper;
import com.ibm.ws.security.token.internal.SingleSignonTokenImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

/**
 *
 */
public class WebSecurityHelperImplTest {
    private final Mockery mock = new JUnit4Mockery();
    private final WebAppSecurityConfig webAppSecConfig = mock.mock(WebAppSecurityConfig.class);

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.WebSecurityHelperImpl#getSSOCookieFromSSOToken()}.
     */
    @Test
    public void getSSOCookieFromSSOToken_noConfigSet() throws Exception {
        assertNull("When no configuration is set, the cookie should be null",
                   WebSecurityHelperImpl.getSSOCookieFromSSOToken());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.WebSecurityHelperImpl#getSSOCookieFromSSOToken()}.
     */
    @Test
    public void getSSOCookieFromSSOToken_configSetNoSubject() throws Exception {
        WebSecurityHelperImpl.setWebAppSecurityConfig(webAppSecConfig);
        assertNull("When no subject is available, the cookie should be null",
                   WebSecurityHelper.getSSOCookieFromSSOToken());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.WebSecurityHelperImpl#getLTPACookie()}.
     */
    @Test
    public void getLTPACookie_noSSOToken() throws Exception {
        WebSecurityHelperImpl.setWebAppSecurityConfig(webAppSecConfig);
        Subject subject = new Subject();
        assertNull("When no SSO token exists, the cookie should be null",
                   WebSecurityHelperImpl.getLTPACookie(subject));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.WebSecurityHelperImpl#getLTPACookie()}.
     */
    @Test(expected = WSSecurityException.class)
    public void getLTPACookie_multipleSSOToken() throws Exception {
        WebSecurityHelperImpl.setWebAppSecurityConfig(webAppSecConfig);
        Subject subject = new Subject();
        subject.getPrivateCredentials().add(new SingleSignonTokenImpl(null));
        subject.getPrivateCredentials().add(new SingleSignonTokenImpl(null));
        WebSecurityHelperImpl.getLTPACookie(subject);
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.internal.WebSecurityHelperImpl#getLTPACookie()}.
     */
    @Test
    public void getLTPACookie_oneSSOToken() throws Exception {
        WebSecurityHelperImpl.setWebAppSecurityConfig(webAppSecConfig);
        Subject subject = new Subject();
        subject.getPrivateCredentials().add(new SingleSignonTokenImpl(null));

        final String cookieName = "mySSOCookie";
        mock.checking(new Expectations() {
            {
                one(webAppSecConfig).getSSOCookieName();
                will(returnValue(cookieName));
            }
        });

        Cookie c = WebSecurityHelperImpl.getLTPACookie(subject);
        assertNotNull("Cookie should not be null", c);
        assertEquals("Cookie must have the correct name",
                     cookieName, c.getName());
        assertNotNull("Cookie must have some value",
                      c.getValue());
    }

}
