/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.appbnd.internal.delegation;

import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;

import com.ibm.ws.container.service.security.SecurityRoles;
import com.ibm.ws.javaee.dd.appbnd.RunAs;
import com.ibm.ws.javaee.dd.appbnd.SecurityRole;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 * This helper class builds the test SecurityRoles objects. The usage pattern is to first invoke
 * buildSecurityRole in order to create a single role. Continue invoking buildSecurityRole until
 * all the desired roles are created for a single SecurityRoles object. Finally, invoke getSecurityRoles
 * to get the built SecurityRoles object.
 */
class SecurityRolesBuilder {

    private final Mockery mockery;
    private final List<SecurityRole> currentSecurityRoleList;

    public SecurityRolesBuilder(Mockery mockery) {
        this.mockery = mockery;
        currentSecurityRoleList = new ArrayList<SecurityRole>();
    }

    public void buildSecurityRole(String roleName, String runasUserName, String runasUserPassword) {
        RunAs runAs = createRunAs(runasUserName, runasUserPassword);
        SecurityRole securityRole = createSecurityRole(roleName, runAs);
        currentSecurityRoleList.add(securityRole);
    }

    public SecurityRoles getSecurityRoles() throws UnableToAdaptException {
        SecurityRoles securityRoles = createSecurityRoles();
        clearCurrentSecurityRoleListForNextBuildCycle();
        return securityRoles;
    }

    private RunAs createRunAs(final String userName, final String password) {
        final RunAs runAs = mockery.mock(RunAs.class, "RunAs:" + userName + ":" + password + ":" + System.currentTimeMillis());
        mockery.checking(new Expectations() {
            {
                allowing(runAs).getUserid();
                will(returnValue(userName));
                allowing(runAs).getPassword();
                will(returnValue(password));
            }
        });
        return runAs;
    }

    private SecurityRole createSecurityRole(final String roleName, final RunAs runAs) {
        final SecurityRole securityRole = mockery.mock(SecurityRole.class, "SecurityRole:" + runAs.toString());
        mockery.checking(new Expectations() {
            {
                allowing(securityRole).getName();
                will(returnValue(roleName));
                allowing(securityRole).getRunAs();
                will(returnValue(runAs));
            }
        });
        return securityRole;
    }

    private SecurityRoles createSecurityRoles() throws UnableToAdaptException {
        final List<SecurityRole> securityRoleList = new ArrayList<SecurityRole>(currentSecurityRoleList);
        final SecurityRoles securityRoles = mockery.mock(SecurityRoles.class, "SecurityRoles:" + securityRoleList.toString());
        mockery.checking(new Expectations() {
            {
                allowing(securityRoles).getSecurityRoles();
                will(returnValue(securityRoleList));
            }
        });
        return securityRoles;
    }

    private void clearCurrentSecurityRoleListForNextBuildCycle() {
        currentSecurityRoleList.clear();
    }

}
