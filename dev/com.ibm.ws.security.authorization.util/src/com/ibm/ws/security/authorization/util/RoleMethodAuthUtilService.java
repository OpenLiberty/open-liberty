/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.authorization.util;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.security.authorization.AuthorizationService;

/**
 * OSGi DS 1.4 component that injects the {@link AuthorizationService} into
 * {@link RoleMethodAuthUtil} via constructor injection so that Everyone
 * special-subject role checks can be performed without requiring each caller
 * to pass the service explicitly.
 *
 * The component is activated whenever AuthorizationService is available
 * (i.e. when security is enabled) and deactivated when it goes away,
 * at which point the static reference is cleared.
 */
@Component(service = RoleMethodAuthUtilService.class, immediate=true)
public class RoleMethodAuthUtilService {

    @Activate
    public RoleMethodAuthUtilService(@Reference AuthorizationService authorizationService) {
        RoleMethodAuthUtil.setAuthorizationService(authorizationService);
    }

    @Deactivate
    protected void deactivate() {
        RoleMethodAuthUtil.setAuthorizationService(null);
    }
}
