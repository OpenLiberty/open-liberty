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
package com.ibm.websphere.security.web;

import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.ibm.ws.webcontainer.security.internal.WebSecurityHelperImpl;

/**
 *
 */
public class WebSecurityHelperTest {

    /**
     * Test method for {@link com.ibm.websphere.security.web.WebSecurityHelper#getSSOCookieFromSSOToken()}.
     */
    @Test
    public void getSSOCookieFromSSOToken_noConfigSet() throws Exception {
        assertNull("When no WebAppSecurityConfiguration is set, the cookie should be null",
                   WebSecurityHelper.getSSOCookieFromSSOToken());
    }

    /**
     * Test method for {@link com.ibm.websphere.security.web.WebSecurityHelper#getSSOCookieName()}.
     */
    @Test
    public void getSSOCookieName_noConfigSet() throws Exception {
        WebSecurityHelperImpl.setWebAppSecurityConfig(null);
        assertNull("When no WebAppSecurityConfiguration is set, the cookie name should be null",
                   WebSecurityHelper.getSSOCookieName());
    }

}
