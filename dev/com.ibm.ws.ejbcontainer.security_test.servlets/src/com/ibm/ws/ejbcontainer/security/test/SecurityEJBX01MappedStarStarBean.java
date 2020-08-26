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
import javax.ejb.EJB;
import javax.ejb.EJBAccessException;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

/**
 * Bean implementation class for Stateless Enterprise Bean to be used in
 * testing version 3.0 ejb-jar.xml with assembly-descriptor and no security annotations.
 */
@Stateless
public class SecurityEJBX01MappedStarStarBean extends SecurityEJBBeanBase implements SecurityEJBInterface {

    private static final Class<?> c = SecurityEJBX01MappedStarStarBean.class;
    protected Logger logger = Logger.getLogger(c.getCanonicalName());

    @EJB(beanName = "SecurityEJBRunAsBean")
    private SecurityEJBRunAsInterface injectedRunAs;

    @Resource
    private SessionContext context;

    public SecurityEJBX01MappedStarStarBean() {

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
        return authenticate("checkAuthenticated");
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
            result = result + "SecurityEJBX01Bean is invoking injected SecurityEJBRunAsBean running as client: \n";
            result = result + injectedRunAs.employee();
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
            result = result + "SecurityEJBX01Bean is invoking injected SecurityEJBRunAsBean running as specified Employee role: \n";
            result = result + injectedRunAs.employee();
            return result;
        } catch (EJBAccessException e) {
            return e.toString();
        }
    }
}
