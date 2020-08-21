/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.security.test;

import java.util.logging.Logger;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

/**
 * Bean superclass implementation class for Enterprise Bean
 * This class is used as superclass in conjunction with the
 * derived class, SecurityEJBA07Bean, in order to test Java
 * inheritance in PureAnnA07InheritanceTest.
 */

@Stateless
@RolesAllowed({ "Manager" })
@DeclareRoles("DeclaredRole01")
@RunAs("Employee")
public class SecurityEJBA07Base extends SecurityEJBBeanBase {

    @Override
    protected SessionContext getContext() {
        return null; // Class cannot be abstract
    }

    @Override
    protected Logger getLogger() {
        return null; // Class cannot be abstract
    }

    @DenyAll
    public String denyAll() {
        return authenticate("denyAll");
    }

    public String denyAll(String input) {
        return authenticate("denyAll(input)");
    }

    @PermitAll
    public String permitAll() {
        return authenticate("permitAll");
    }

    public String permitAll(String input) {
        return authenticate("permitAll(input)");
    }

    public String manager() {
        return authenticate("manager");
    }

    // no annotations here or in derived class, so method permissions not specified
    public String manager(String input) {
        return authenticate("manager(input)");
    }

    @RolesAllowed("Employee")
    public String employee() {
        return authenticate("employee");
    }

    public String employee(String input) {
        return authenticate("employee(input)");
    }

    @RolesAllowed({ "Employee", "Manager" })
    public String employeeAndManager() {
        return authenticate("employeeAndManager");
    }

    @RolesAllowed({ "Employee", "Manager" })
    public String employeeAndManager(String input) {
        return authenticate("employeeAndManager(input)");
    }

// No annotation here, derived class provides
    public String employeeAndManager(String input, String input2) {
        return authenticate("employeeAndManager(string1, string2)");
    }

// No annotation here or on derived class, inherit class level annotation
    public String employeeAndManager(int i) {
        return authenticate("employeeAndManager(3)");
    }

    public String declareRoles01() {
        String result1 = authenticate("declareRoles01");
        boolean isDeclaredMgr = getContext().isCallerInRole("DeclaredRole01");
        int len = result1.length() + 5;
        StringBuffer result2 = new StringBuffer(len);
        result2.append(result1);
        result2.append("   isCallerInRole(DeclaredRole01)=");
        result2.append(isDeclaredMgr);
        result2.append("\n");
        getLogger().info("result2: " + result2);
        return result2.toString();
    }

    public String runAsClient() {
        String result = null;
        return result;
    }

    public String runAsSpecified() {
        String result = null;
        return result;
    }

}
