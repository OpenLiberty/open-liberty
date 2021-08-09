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
package com.ibm.ws.security.authentication.internal;

import static org.junit.Assert.assertEquals;

import javax.security.auth.Subject;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;

import com.ibm.wsspi.security.token.SingleSignonToken;

/**
 *
 */
public class SSOTokenHelperTest {
    private final Mockery mockery = new JUnit4Mockery();

    /**
     * Test method for {@link com.ibm.ws.security.authentication.internal.SSOTokenHelper#getSSOToken(javax.security.auth.Subject)}.
     */
    @Test
    public void getSSOToken() throws Exception {
        SingleSignonToken ssoToken = mockery.mock(SingleSignonToken.class);

        Subject subject = new Subject();
        subject.getPrivateCredentials().add(ssoToken);

        assertEquals("The SSO token must be obtained from the subject.",
                     ssoToken, SSOTokenHelper.getSSOToken(subject));
    }

}
