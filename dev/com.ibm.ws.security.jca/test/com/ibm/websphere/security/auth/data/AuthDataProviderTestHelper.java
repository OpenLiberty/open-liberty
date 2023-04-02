/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.auth.data;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

public class AuthDataProviderTestHelper {

    private final AuthDataProvider authDataProvider;
    private final ComponentContext cc;

    public AuthDataProviderTestHelper(AuthDataProvider authDataProvider, ComponentContext cc) {
        this.authDataProvider = authDataProvider;
        this.cc = cc;
    }

    public void setAuthData(ServiceReference<AuthData> authDataRef) {
        authDataProvider.setAuthData(authDataRef);
    }

    public void unsetAuthData(ServiceReference<AuthData> authDataRef) {
        authDataProvider.unsetAuthData(authDataRef);
    }

    public void activate() {
        authDataProvider.activate(cc);
    }

    public void deactivate() {
        authDataProvider.deactivate(cc);
    }
}
