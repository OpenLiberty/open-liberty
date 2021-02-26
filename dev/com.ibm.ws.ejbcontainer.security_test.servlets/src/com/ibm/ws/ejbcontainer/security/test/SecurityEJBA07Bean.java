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

import javax.annotation.Resource;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.EJBAccessException;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

/**
 * Bean derived class implementation class for Enterprise Bean.
 * This class is used in conjunction with the superclass
 * SecurityEJBA07Base, in order to test Java
 * inheritance in PureAnnA07InheritanceTest.
 */

@Stateless
public class SecurityEJBA07Bean extends SecurityEJBA07Base implements SecurityEJBInterface {

    private static final Class<?> c = SecurityEJBA07Bean.class;
    protected Logger logger = Logger.getLogger(c.getCanonicalName());

    @EJB(beanName = "SecurityEJBRunAsBean")
    private SecurityEJBRunAsInterface injectedRunAs;

    @Resource
    private SessionContext context;

    public SecurityEJBA07Bean() {
        withDeprecation();
    }

    @Override
    protected SessionContext getContext() {
        return context;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    // Method denyAll() is not implemented here, so superclass method with @DenyAll is used

    // @DenyAll here overrides superclass @RolesAllowed
    @Override
    @DenyAll
    public String denyAll(String input) {
        return authenticate("denyAll(input)");
    }

    // Method permitAll() is not implemented here, so superclass method with @DenyAll is used.

    //@PermitAll here overrides superclass @RolesAllowed
    @Override
    @PermitAll
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

    // Method manager() is not implemented here, so superclass @RolesAllowed (Manager) is used.

    // Method manager(String) does not have annotation, so superclass @RolesAllowed is used
    @Override
    public String manager(String input) {
        return authenticate("manager(input)");
    }

    // Method employee() is not implemented here, so superclass method with @RolesAllowed(Employee) is used.

    //@RolesAllowed here overrides superclass @RolesAllowed
    @Override
    @RolesAllowed("Employee")
    public String employee(String input) {
        return authenticate("employee(input)");
    }

    // Method employeeAndManager() is not implemented here, so superclass method with @RolesAllowed(Employee,Manager) is used.

    //No annotation here, so uses superclass @RolesAllowed(Employee,Manager)
    @Override
    public String employeeAndManager(String input) {
        return authenticate("employeeAndManager(input)");
    }

    @Override
    @RolesAllowed({ "Employee", "Manager" })
    public String employeeAndManager(String input, String input2) {
        return authenticate("employeeAndManager(string1, string2)");
    }

    // Method employeeAndManager(Int) not implemented here and there is no annotation on the superclass method, so use superclass class level annotation

    @Override
    @PermitAll
    public String declareRoles01() {
        String result1 = authenticate("declareRoles01");
        boolean isDeclaredMgr = context.isCallerInRole("DeclaredRole01");
        int len = result1.length() + 5;
        StringBuffer result2 = new StringBuffer(len);
        result2.append(result1);
        result2.append("   isCallerInRole(DeclaredRole01)=");
        result2.append(isDeclaredMgr);
        result2.append("\n");
        logger.info("result2: " + result2);
        return result2.toString();
    }

    // This method is not used in PureAnnA07Test
    @Override
    @PermitAll
    public String runAsClient() {
        try {
            String result = null;
            result = authenticate("runAsClient");
            result = result + "SecurityEJBA07Bean is invoking injected SecurityEJBRunAsBean running as client: \n";
            result = result + injectedRunAs.employee();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    // This method overrides the superclass method.
    // The runAsSpecified method is used in PureAnnA07InheritanceTest negative test to show that the
    // class level RunAs annotation is not used by the derived class because the derived class overrides the method.
    @Override
    @PermitAll
    public String runAsSpecified() {
        try {
            String result = null;
            result = authenticate("runAsSpecified");
            result = result + "SecurityEJBA07Bean is invoking injected SecurityEJBRunAsBean running as specified Employee role: \n";
            result = result + injectedRunAs.employee();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

}
