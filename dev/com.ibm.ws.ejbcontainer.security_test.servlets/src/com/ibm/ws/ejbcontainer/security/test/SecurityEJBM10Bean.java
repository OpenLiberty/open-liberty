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
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.EJBAccessException;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

/**
 * Bean implementation class for Stateless Enterprise Bean to be used in testing ibm-ejb-jar-ext.xml
 * extensions for run-as settings.
 *
 * Permissions are set as follows:
 * - Annotations - there are no security annotations.
 * - ejb-jar.xml contains the use-caller-identity for run-as at EJB level.
 * - ibm-ejb-jar-xml.bnd does not exist.
 * - ibm-ejb-jar-ext.xml contains run-as SPECIFIED_IDENTITY Employee role for all methods as indicated by name="*"
 *
 */

@Stateless
public class SecurityEJBM10Bean extends SecurityEJBBeanBase implements SecurityEJBInterface {

    private static final Class<?> c = SecurityEJBM10Bean.class;
    protected Logger logger = Logger.getLogger(c.getCanonicalName());

    @EJB(beanName = "SecurityEJBRunAsExtBean")
    private SecurityEJBRunAsInterface injectedRunAs;

    @Resource
    private SessionContext context;

    @Override
    protected SessionContext getContext() {
        return context;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
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

    // Note: ibm-ejb-jar-ext.xml file overrides ejb-jar use-caller-identity for this method such that this method invokes second EJB as SPECIFIED_IDENTITY Employee
    @Override
    public String manager() {
        try {
            String result = null;
            result = authenticate("manager");
            result = result + "SecurityEJBM10Bean is invoking injected SecurityEJBRunAsExtBean manager method running as specified Employee role per ext file: \n";
            result = result + injectedRunAs.manager();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    // Note: ibm-ejb-jar-ext.xml file overrides ejb-jar use-caller-identity for this method such that this method invokes second EJB as SPECIFIED_IDENTITY Employee
    @Override
    public String manager(String input) {
        try {
            String result = null;
            result = authenticate("manager(String)");
            result = result + "SecurityEJBM10Bean is invoking injected SecurityEJBRunAsExtBean manager method running as specified Employee role per ext file: \n";
            result = result + injectedRunAs.manager();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    // Note: ibm-ejb-jar-ext.xml file overrides ejb-jar use-caller-identity for this method such that this method invokes second EJB as SPECIFIED_IDENTITY Employee
    @Override
    public String employee() {
        try {
            String result = null;
            result = authenticate("employee");
            result = result + "SecurityEJBM10Bean is invoking injected SecurityEJBRunAsExtBean employee method running as specified Employee role per ext file: \n";
            result = result + injectedRunAs.employee();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    // Note: ibm-ejb-jar-ext.xml file overrides ejb-jar use-caller-identity for this method such that this method invokes second EJB as SPECIFIED_IDENTITY Employee
    @Override
    public String employee(String input) {
        try {
            String result = null;
            result = authenticate("employee(String)");
            result = result + "SecurityEJBM10Bean is invoking injected SecurityEJBRunAsExtBean employee method running as specified Employee role per ext file: \n";
            result = result + injectedRunAs.employee();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    @Override
    public String employeeAndManager() {
        try {
            String result = null;
            result = authenticate("employeeAndManager");
            result = result + "SecurityEJBM10Bean is invoking injected SecurityEJBRunAsExtBean employeeAndManager method running as specified Employee role per ext file: \n";
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
            result = result + "SecurityEJBM10Bean is invoking injected SecurityEJBRunAsExtBean employeeAndManager method running as specified Employee role per ext file: \n";
            result = result + injectedRunAs.employeeAndManager();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    @Override
    public String employeeAndManager(String input, String input2) {
        try {
            String result = null;
            result = authenticate("employeeAndManager(string1,string2)");
            result = result + "SecurityEJBM10Bean is invoking injected SecurityEJBRunAsExtBean employee method running as specified Employee role per ext file: \n";
            result = result + injectedRunAs.employeeAndManager();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    @Override
    public String employeeAndManager(int i) {
        try {
            String result = null;
            result = authenticate("employeeAndManager(3)");
            result = result + "SecurityEJBM10Bean is invoking injected SecurityEJBRunAsExtBean employee method running as specified Employee role per ext file: \n";
            result = result + injectedRunAs.employeeAndManager();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    @Override
    public String declareRoles01() {
        try {
            String result = null;
            result = authenticate("declaredRole01");
            result = result + "SecurityEJBM10Bean is invoking injected SecurityEJBRunAsExtBean employee method running as specified Employee role per ext file: \n";
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
            result = result + "SecurityEJBM10Bean is invoking injected SecurityEJBRunAsExtBean employee method running as specified Employee role per ext file: \n";
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
            result = result + "SecurityEJBM10Bean is invoking injected SecurityEJBRunAsExtBean employee method running as specified Employee role per ext file: \n";
            result = result + injectedRunAs.employeeAndManager();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

}
