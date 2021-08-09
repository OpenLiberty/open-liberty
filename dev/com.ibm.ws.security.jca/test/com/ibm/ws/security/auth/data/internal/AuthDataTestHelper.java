/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.auth.data.internal;

import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.security.auth.data.AuthData;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

public class AuthDataTestHelper {

    private final Mockery mockery;
    private final ComponentContext cc;

    public AuthDataTestHelper(Mockery mockery, ComponentContext cc) {
        this.mockery = mockery;
        this.cc = cc;
    }

    public AuthData createAuthData(String name, String password) {
        AuthDataImpl authDataConfig = new AuthDataImpl();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(AuthDataImpl.CFG_KEY_USER, name);
        props.put(AuthDataImpl.CFG_KEY_PASSWORD, password == null ? null : new SerializableProtectedString(password.toCharArray()));
        authDataConfig.activate(props);
        return authDataConfig;
    }

    @SuppressWarnings("unchecked")
    public ServiceReference<AuthData> createAuthDataRef(final String authDataAlias, final String displayId, final AuthData authDataConfig) {
        final ServiceReference<AuthData> authDataRef = mockery.mock(ServiceReference.class, authDataAlias + displayId);
        mockery.checking(new Expectations() {
            {
                allowing(authDataRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(authDataRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                allowing(authDataRef).getProperty(AuthDataImpl.CFG_KEY_ID);
                will(returnValue(authDataAlias));
                allowing(authDataRef).getProperty(AuthDataImpl.CFG_KEY_DISPLAY_ID);
                will(returnValue(displayId));
                allowing(cc).locateService("authData", authDataRef);
                will(returnValue(authDataConfig));
            }
        });
        return authDataRef;
    }

}
