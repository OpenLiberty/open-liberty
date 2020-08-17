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
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.EJBAccessException;
import javax.ejb.Remove;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.ejb.StatefulTimeout;

/**
 * Bean implementation class for Stateful Enterprise Bean to be used in testing ibm-ejb-jar-ext.xml
 * extensions for run-as settings.
 *
 * Permissions are set as follows:
 * - Annotations contain - @RunAs(Employee) which applies to all methods of the SecurityEJBM08Bean.
 * - Annotations contain - method level @PermitAll, @DenyAll and @RolesAllowed which should take effect with ext file present.
 * - ejb-jar.xml does not exist.
 * - ibm-ejb-jar-xml.bnd does not exist.
 * - ibm-ejb-jar-ext.xml contains specific method level run-as settings to override annotations.
 *
 */

@RunAs("Employee")
@DeclareRoles("DeclaredRole01")
@Stateful
@StatefulTimeout(value = 30)
public class SecurityEJBM08Bean extends SecurityEJBBeanBase implements SecurityEJBStatefulInterface {

    private static final Class<?> c = SecurityEJBM08Bean.class;
    protected Logger logger = Logger.getLogger(c.getCanonicalName());

    @EJB(beanName = "SecurityEJBRunAsExtBean")
    private SecurityEJBRunAsInterface injectedRunAs;

    @Resource
    private SessionContext context;

    public SecurityEJBM08Bean() {
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
    @PermitAll
    public String permitAll() {
        return authenticate("permitAll");
    }

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

    // Note: ibm-ejb-jar-ext.xml file overrides RunAs(Employee) annotation for this method such that this method invokes second EJB as SPECIFIED_IDENTITY
    @Override
    @RolesAllowed("Manager")
    public String manager() {
        try {
            String result = null;
            result = authenticate("manager");
            result = result + "SecurityEJBM08Bean is invoking injected SecurityEJBRunAsExtBean manager method running as SPECIFIED_IDENTITY per ext file: \n";
            result = result + injectedRunAs.manager();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    @Override
    @RolesAllowed("Manager")
    // Note: ibm-ejb-jar-ext.xml file overrides RunAs(Employee) annotation for this method such that this method invokes second EJB as CALLER_IDENTITY
    public String manager(String input) {
        try {
            String result = null;
            result = authenticate("manager(String)");
            result = result + "SecurityEJBM08Bean is invoking injected SecurityEJBRunAsExtBean manager method running as CALLER_IDENTITY  per ext file: \n";
            result = result + injectedRunAs.manager();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    // Note: ibm-ejb-jar-ext.xml file overrides RunAs(Employee) annotation for this method such that this method invokes second EJB as SPECIFIED_IDENTITY
    @Override
    @RolesAllowed("Employee")
    public String employee() {
        try {
            String result = null;
            result = authenticate("employee");
            result = result + "SecurityEJBM08Bean is invoking injected SecurityEJBRunAsExtBean employee method running as SPECIFIED_IDENTITY per ext file: \n";
            result = result + injectedRunAs.employee();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    @Override
    @RolesAllowed("Employee")
    // Note: ibm-ejb-jar-ext.xml file overrides RunAs(Employee) annotation for this method such that this method invokes second EJB as CALLER_IDENTITY
    public String employee(String input) {
        try {
            String result = null;
            result = authenticate("employee(String)");
            result = result + "SecurityEJBM08Bean is invoking injected SecurityEJBRunAsExtBean employee method running as caller per ext file: \n";
            result = result + injectedRunAs.employee();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    // Note: ibm-ejb-jar-ext.xml file overrides RunAs(Employee) annotation for this method such that this method invokes second EJB as CALLER_IDENTITY
    @Override
    public String employeeAndManager() {
        try {
            String result = null;
            result = authenticate("employeeAndManager");
            result = result + "SecurityEJBM08Bean is invoking injected SecurityEJBRunAsExtBean employeeAndManager method running as CALLER per ext file: \n";
            result = result + injectedRunAs.employeeAndManager();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    @Override
    public String employeeAndManager(String input) {
        try {
            String result = null;
            result = authenticate("employeeAndManager(input)");
            result = result + "SecurityEJBM08Bean is invoking injected SecurityEJBRunAsExtBean employeeAndManager method running as Employee per annotation: \n";
            result = result + injectedRunAs.employeeAndManager();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    // Note: ibm-ejb-jar-ext.xml file overrides RunAs(Employee) annotation for this method such that this method invokes second EJB as SPECIFIED_IDENTITY
    @Override
    public String employeeAndManager(String input, String input2) {
        try {
            String result = null;
            result = authenticate("employeeAndManager(string1,string2)");
            result = result + "SecurityEJBM08Bean is invoking injected SecurityEJBRunAsExtBean employeeAndManager method running as SPECIFIED per ext file: \n";
            result = result + injectedRunAs.employeeAndManager();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    // Note: ibm-ejb-jar-ext.xml file overrides RunAs(Employee) annotation for this method such that this method invokes second EJB as SYSTEM_IDENTITY
    @Override
    public String employeeAndManager(int i) {
        try {
            String result = null;
            result = authenticate("employeeAndManager(3)");
            result = result + "SecurityEJBM08Bean is invoking injected SecurityEJBRunAsExtBean employeeAndManager method running as system: \n";
            result = result + injectedRunAs.employeeAndManager();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    // Note: ibm-ejb-jar-ext.xml file overrides RunAs(Employee) annotation for this method such that this method invokes second EJB as SPECIFIED_IDENTITY
    @Override
    public String declareRoles01() {
        try {
            String result = null;
            result = authenticate("declaredRole01");
            result = result + "SecurityEJBM08Bean is invoking injected SecurityEJBRunAsExtBean employeeAndManager method running as specified: \n";
            result = result + injectedRunAs.employeeAndManager();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    @Override
    public String runAsClient() {
        try {
            String result = null;
            result = authenticate("runAsClient");
            result = result + "SecurityEJBM08Bean is invoking injected SecurityEJBRunAsExtBean employeeAndManager method running as client: \n";
            result = result + injectedRunAs.employeeAndManager();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    @Override
    public String runAsSpecified() {
        try {
            String result = null;
            result = authenticate("runAsSpecified");
            result = result + "SecurityEJBM08Bean is invoking injected SecurityEJBRunAsExtBean employeeAndManager method running as specified user in Manager role \n";
            result = result + injectedRunAs.employeeAndManager();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    @Override
    @Remove
    public void remove() {

    }

}
