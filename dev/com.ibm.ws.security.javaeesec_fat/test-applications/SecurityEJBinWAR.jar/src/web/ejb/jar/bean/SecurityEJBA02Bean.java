/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.ejb.jar.bean;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.security.enterprise.SecurityContext;

/**
 * Bean implementation class for Enterprise Bean
 */

@Singleton
@PermitAll
public class SecurityEJBA02Bean extends SecurityEJBBeanBase implements SecurityEJBInterface {

    private static final Class<?> c = SecurityEJBA02Bean.class;
    protected Logger logger = Logger.getLogger(c.getCanonicalName());

    @Inject
    SecurityContext securityContext;

    @Resource
    private SessionContext context;

    public SecurityEJBA02Bean() {}

    @Override
    protected SessionContext getContext() {
        return context;
    }

    @Override
    protected SecurityContext getSecurityContext() {
        return securityContext;

    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    @DenyAll
    public String denyAll() {
        return authenticate("denyAll");
    }

    @Override
    @DenyAll
    public String denyAll(String input) {
        return authenticate("denyAll(input)");
    }

    @Override
    public String permitAll() {
        return authenticate("permitAll");
    }

    @Override
    public String permitAll(String input) {
        return authenticate("permitAll(input)");
    }

    @Override
    public String checkAuthenticated() {
        return authenticate("checkAuthenticated()");
    }

    @Override
    @RolesAllowed("**")
    public String permitAuthenticated() {
        return authenticate("permitAuthenticated()");
    }

    @Override
    @RolesAllowed("Manager")
    public String manager() {
        return authenticate("manager");
    }

    @Override
    @RolesAllowed("Manager")
    public String manager(String input) {
        return authenticate("manager(input)");
    }

    @Override
    @RolesAllowed("Employee")
    public String employee() {
        return authenticate("employee");
    }

    @Override
    @RolesAllowed("Employee")
    public String employee(String input) {
        return authenticate("employee(input)");
    }

    @Override
    @RolesAllowed({ "Employee", "Manager" })
    public String employeeAndManager() {
        return authenticate("employeeAndManager");
    }

    @Override
    @RolesAllowed({ "Employee", "Manager" })
    public String employeeAndManager(String input) {
        return authenticate("employeeAndManager(input)");
    }

    @Override
    @RolesAllowed({ "Employee", "Manager" })
    public String employeeAndManager(String input, String input2) {
        return authenticate("employeeAndManager(string1, string2)");
    }

    @Override
    @RolesAllowed({ "Employee", "Manager" })
    public String employeeAndManager(int i) {
        return authenticate("employeeAndManager(3)");
    }

    @Override
    public String declareRoles01() {
        String result1 = authenticate("declareRoles01");

        boolean isDeclaredMgr = securityContext.isCallerInRole("DeclaredRole01");

        int len = result1.length() + 5;
        StringBuffer result2 = new StringBuffer(len);
        result2.append(result1);
        result2.append("\n");
        result2.append("   isCallerInRole(DeclaredRole01)=");
        result2.append(isDeclaredMgr);
        logger.info("result2: " + result2);
        return result2.toString();
    }

    @Override
    @PermitAll
    public String runAsClient() {
        return authenticate("runAsClient");
    }

    @Override
    @PermitAll
    public String runAsSpecified() {
        return authenticate("runAsSpecified");
    }

}
