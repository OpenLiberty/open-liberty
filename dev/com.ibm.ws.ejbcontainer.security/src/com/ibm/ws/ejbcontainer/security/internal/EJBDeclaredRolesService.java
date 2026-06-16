/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.security.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.ws.security.authorization.DeclaredRolesService;

@Component(service = { DeclaredRolesService.class, EJBDeclaredRolesService.class }, configurationPolicy = ConfigurationPolicy.IGNORE,
           property = "service.vendor=IBM")
public class EJBDeclaredRolesService extends DeclaredRolesService {
    void addDeclaredRoles(BeanMetaData bmd) {
        Set<String> roles = new HashSet<>();
        EJBMethodInfoImpl list[][] = { bmd.homeMethodInfos, bmd.localHomeMethodInfos, bmd.methodInfos, bmd.localMethodInfos, bmd.timedMethodInfos, bmd.wsEndpointMethodInfos,
                                       bmd.lifecycleInterceptorMethodInfos };
        for (EJBMethodInfoImpl methodInfos[] : list) {
            if (methodInfos != null && methodInfos.length > 0) {
                for (EJBMethodInfoImpl methodInfo : methodInfos) {
                    List<String> rolesAllowed = methodInfo.getRolesAllowed();
                    if (rolesAllowed != null && rolesAllowed.size() > 0) {
                        roles.addAll(rolesAllowed);
                    }
                }
            }
        }
        super.addDeclaredRoles(bmd.j2eeName.getApplication(), bmd.j2eeName.getModule(), roles);
    }
}
