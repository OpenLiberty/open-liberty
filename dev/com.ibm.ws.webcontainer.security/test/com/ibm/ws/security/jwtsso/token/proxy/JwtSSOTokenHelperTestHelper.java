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
package com.ibm.ws.security.jwtsso.token.proxy;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

/**
 * Helper class to be able to set the JWT configuration
 */
public class JwtSSOTokenHelperTestHelper {

    private final Mockery mockery;

    private final JwtSSOTokenHelper jwtSSOTokenHelper;
    private final ComponentContext cc;
    private ServiceReference<JwtSSOTokenProxy> jwtSSOTokenProxyServiceRef = null;

    public JwtSSOTokenHelperTestHelper(Mockery mock) {
        this.mockery = mock;
        cc = mockery.mock(ComponentContext.class);
        jwtSSOTokenHelper = new JwtSSOTokenHelper();
    }

    @SuppressWarnings("unchecked")
    public void setJwtSSOTokenProxyWithCookieName(final String jwtCookieName) {
        jwtSSOTokenProxyServiceRef = mockery.mock(ServiceReference.class);
        final JwtSSOTokenProxy jwtSSOTokenProxy = mockery.mock(JwtSSOTokenProxy.class);
        jwtSSOTokenHelper.setJwtSSOToken(jwtSSOTokenProxyServiceRef);

        mockery.checking(new Expectations() {
            {
                one(cc).locateService("JwtSSOTokenProxy", jwtSSOTokenProxyServiceRef);
                will(returnValue(jwtSSOTokenProxy));
                one(jwtSSOTokenProxy).getJwtCookieName();
                will(returnValue(jwtCookieName));
            }
        });

        jwtSSOTokenHelper.activate(cc);
    }

    public void tearDown() {
        if (jwtSSOTokenProxyServiceRef != null) {
            jwtSSOTokenHelper.unsetJwtSSOToken(jwtSSOTokenProxyServiceRef);
            jwtSSOTokenHelper.deactivate(cc);
        }
    }

}
