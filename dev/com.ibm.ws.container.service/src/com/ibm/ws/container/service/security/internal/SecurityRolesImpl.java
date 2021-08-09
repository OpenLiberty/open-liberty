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
package com.ibm.ws.container.service.security.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.security.SecurityRoles;
import com.ibm.ws.javaee.dd.appbnd.SecurityRole;

class SecurityRolesImpl implements SecurityRoles {
    private static final TraceComponent tc = Tr.register(SecurityRolesImpl.class);

    private List<SecurityRole> securityRolesList = null;

    /**
     * @param containerToAdapt
     * @param securityRoles
     */
    public SecurityRolesImpl(List<SecurityRole> allSecurityRoles) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "The security roles from the application bind file and server.xml are: " + allSecurityRoles);
        }

        HashMap<String, SecurityRole> mergedRoles = new HashMap<String, SecurityRole>();
        for (SecurityRole role : allSecurityRoles) {
            // Security roles configured in server.xml always appear after roles from the bindings files, so this will result in
            // server.xml configured entries overriding entries from the bindings files.
            SecurityRole previous = mergedRoles.put(role.getName(), role);
            if (previous != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Overriding security role with name " + previous.getName() + "old: " + previous + "new: " + role);
                }
            }
        }
        this.securityRolesList = new ArrayList<SecurityRole>(mergedRoles.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SecurityRole> getSecurityRoles() {
        return securityRolesList;
    }

}