/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.security.test;

import java.security.Principal;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBAccessException;
import javax.ejb.Remove;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.ejb.StatefulTimeout;
import javax.security.jacc.PolicyContext;

/**
 * Bean implementation class for Stateful Enterprise Bean to be used in
 * testing ejb-jar.xml only with no annotations.
 */

@Stateful
@StatefulTimeout(value = 30)
public class SecurityEJBX02Bean extends SecurityEJBBeanBase implements SecurityEJBStatefulInterface {

    private static final Class<?> c = SecurityEJBX02Bean.class;
    protected Logger logger = Logger.getLogger(c.getCanonicalName());

    @EJB(beanName = "SecurityEJBRunAsBean")
    private SecurityEJBRunAsInterface injectedRunAs;

    @Resource
    private SessionContext context;

    public SecurityEJBX02Bean() {

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
    public String permitAuthenticated() {
        return authenticate("permitAuthenticated()");
    }

    @Override
    public String manager() {
        return authenticate("manager");
    }

    @Override
    public String manager(String input) {
        return authenticate("manager(input)");
    }

    @Override
    public String employee() {
        return authenticate("employee");
    }

    @Override
    public String employee(String input) {
        return authenticate("employee(input)");
    }

    @Override
    public String employeeAndManager() {
        return authenticate("employeeAndManager");
    }

    @Override
    public String employeeAndManager(String input) {
        return authenticate("employeeAndManager(input)");
    }

    @Override
    public String employeeAndManager(String input, String input2) {
        return authenticate("employeeAndManager(string1, string2)");
    }

    @Override
    public String employeeAndManager(int i) {
        return authenticate("employeeAndManager(3)");
    }

    @Override
    public String declareRoles01() {
        String result1 = authenticate("declareRoles01");
        boolean isDeclared = context.isCallerInRole("DeclaredRole01");
        int len = result1.length() + 5;
        StringBuffer result2 = new StringBuffer(len);
        result2.append(result1);
        result2.append("\n");
        result2.append("   isCallerInRole(DeclaredRole01)=");
        result2.append(isDeclared);
        logger.info("result2: " + result2);
        return result2.toString();
    }

    @Override
    public String runAsClient() {
        try {
            String result = null;
            result = authenticate("runAsClient");
            result = result + "SecurityEJBX02Bean is invoking injected SecurityEJBRunAsBean running as client: \n";
            result = result + injectedRunAs.manager();
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
            result = result + "SecurityEJBX02Bean is invoking injected SecurityEJBRunAsBean running as specified Manager role: \n";
            result = result + injectedRunAs.employee();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }

    @Override
    protected String authenticate(String method) {
        Principal principal = context.getCallerPrincipal();
        String principalName = null;
        if (principal != null) {
            principalName = principal.getName();
        } else {
            principalName = "null";
        }

        boolean isMgr = false;
        boolean isEmp = false;
        isMgr = context.isCallerInRole("Mgr");
        isEmp = context.isCallerInRole("Emp");
        int len = principalName.length() + 12;
        StringBuffer result = new StringBuffer(len);
        result.append("EJB  = " + this.getClass().getSimpleName() + "\n");
        result.append("Method = " + method + "\n");
        result.append("   getCallerPrincipal()=");
        result.append(principalName);
        result.append("\n");
        result.append("   isCallerInRole(Mgr)=");
        result.append(isMgr);
        result.append("\n");
        result.append("   isCallerInRole(Emp)=");
        result.append(isEmp);
        result.append("\n");
        Set<String> handlerKeys = PolicyContext.getHandlerKeys();
        for (String key : handlerKeys) {
            result.append("handlerKey(");
            result.append(key);
            result.append(")=true\n");
        }
        logger.info("result: " + result);
        return result.toString();
    }

    @Override
    @Remove
    public void remove() {

    }

}
