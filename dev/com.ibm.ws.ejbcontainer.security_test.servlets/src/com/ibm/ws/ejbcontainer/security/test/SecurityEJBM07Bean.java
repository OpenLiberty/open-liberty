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
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

/**
 * Bean implementation class for Stateless Enterprise Bean to be used in testing ibm-ejb-jar-ext.xml
 * extensions for run-as settings.
 *
 * Permissions are set as follows:
 * - Annotations contain PermitAll and DeclareRoles at class level.
 * - Annotations contain - @RunAs(Employee) which applies to all methods of the SecurityEJBM07Bean.
 * - ejb-jar.xml contains no run as settings, but does contain settings for exclude-list and method-permission which should take
 * effect with ext file present.
 * - ibm-ejb-jar-bnd.xml exists and contains minimal entry.
 * - ibm-ejb-jar-ext.xml contains specific method level run-as settings to override annotations.
 *
 */
@PermitAll
@RunAs("Employee")
@DeclareRoles("DeclaredRole01")
@Stateless
public class SecurityEJBM07Bean extends SecurityEJBBeanBase implements SecurityEJBInterface {

    private static final Class<?> c = SecurityEJBM07Bean.class;
    protected Logger logger = Logger.getLogger(c.getCanonicalName());

    @EJB(beanName = "SecurityEJBRunAsExtBean")
    private SecurityEJBRunAsInterface injectedRunAs;

    @Resource
    private SessionContext context;

    public SecurityEJBM07Bean() {
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
    public String manager() {
        try {
            String result = null;
            result = authenticate("manager");
            result = result + "SecurityEJBM07Bean is invoking injected SecurityEJBRunAsExtBean manager method running as caller identity per ejb-jar: \n";
            result = result + injectedRunAs.manager();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    // Note: ibm-ejb-jar-ext.xml file overrides RunAs(Employee) annotation for this method such that this method invokes second EJB as SPECIFIED_IDENTITY
    @Override
    public String manager(String input) {
        try {
            String result = null;
            result = authenticate("manager(String)");
            result = result + "SecurityEJBM07Bean is invoking injected SecurityEJBRunAsExtBean manager method running as SPECIFIED_IDENTITY per ext file: \n";
            result = result + injectedRunAs.manager();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    @Override
    public String employee() {
        try {
            String result = null;
            result = authenticate("employee");
            result = result + "SecurityEJBM07Bean is invoking injected SecurityEJBRunAsExtBean employee method running as caller identity per ejb-jar: \n";
            result = result + injectedRunAs.employee();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    // Note: ibm-ejb-jar-ext.xml file overrides ejb-jar caller identity for this method such that this method invokes second EJB as CALLER_IDENTITY
    @Override
    public String employee(String input) {
        try {
            String result = null;
            result = authenticate("employee(String)");
            result = result + "SecurityEJBM07Bean is invoking injected SecurityEJBRunAsExtBean employee method running as caller per ext file: \n";
            result = result + injectedRunAs.employee();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    // Note: ibm-ejb-jar-ext.xml file overrides ejb-jar caller identity for this method such that this method invokes second EJB as SPECIFIED_IDENTITY
    @Override
    public String employeeAndManager() {
        try {
            String result = null;
            result = authenticate("employeeAndManager");
            result = result + "SecurityEJBM07Bean is invoking injected SecurityEJBRunAsExtBean employeeAndManager method running as Specified per ext file: \n";
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
            result = result + "SecurityEJBM07Bean is invoking injected SecurityEJBRunAsExtBean employeeAndManager method running as caller: \n";
            result = result + injectedRunAs.employeeAndManager();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    // Note: ibm-ejb-jar-ext.xml file overrides ejb-jar caller identity for this method such that this method invokes second EJB as SPECIFIED_IDENTITY
    @Override
    public String employeeAndManager(String input, String input2) {
        try {
            String result = null;
            result = authenticate("employeeAndManager(string1,string2)");
            result = result + "SecurityEJBM07Bean is invoking injected SecurityEJBRunAsExtBean employeeAndManager method running as SPECIFIED_IDENTITY: \n";
            result = result + injectedRunAs.employeeAndManager();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    // Note: ibm-ejb-jar-ext.xml file overrides ejb-jar caller identity for this method such that this method invokes second EJB as SYSTEM_IDENTITY
    @Override
    public String employeeAndManager(int i) {
        try {
            String result = null;
            result = authenticate("employeeAndManager(3)");
            result = result + "SecurityEJBM07Bean is invoking injected SecurityEJBRunAsExtBean employeeAndManager method running as system: \n";
            result = result + injectedRunAs.employeeAndManager();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    // Note: ibm-ejb-jar-ext.xml file overrides ejb-jar caller identity for this method such that this method invokes second EJB as SPECIFIED_IDENTITY declaredRole
    @Override
    public String declareRoles01() {
        try {
            String result = null;
            result = authenticate("declaredRole01");
            result = result + "SecurityEJBM07Bean is invoking injected SecurityEJBRunAsExtBean employeeAndManager method running as specified: \n";
            result = result + injectedRunAs.employeeAndManager();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

// Note: ibm-ejb-jar-ext.xml file overrides ejb-jar caller identity for this method such that this method invokes second EJB as CALLER_IDENTITY
    @Override
    public String runAsClient() {
        try {
            String result = null;
            result = authenticate("runAsClient");
            result = result + "SecurityEJBM07Bean is invoking injected SecurityEJBRunAsExtBean employeeAndManager method running as caller: \n";
            result = result + injectedRunAs.employeeAndManager();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    // Note: ibm-ejb-jar-ext.xml file overrides ejb-jar caller identity for this method such that this method invokes second EJB as SPECIFIED_IDENTITY
    @Override
    public String runAsSpecified() {
        try {
            String result = null;
            result = authenticate("runAsSpecified");
            result = result + "SecurityEJBM07Bean is invoking injected SecurityEJBRunAsExtBean employeeAndManager method running as specified: \n";
            result = result + injectedRunAs.employeeAndManager();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

}
