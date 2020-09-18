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
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

/**
 * Bean implementation class for Enterprise Bean to be injected in SecurityEJBMExt07Bean for
 * ejb-jar.xml bindings and extension testing.
 */
@SuppressWarnings("deprecation")
@Stateless
public class SecurityEJBRunAsExtBean extends SecurityEJBBeanBase implements SecurityEJBRunAsInterface {

    private static final Class<?> c = SecurityEJBRunAsExtBean.class;
    protected Logger logger = Logger.getLogger(c.getCanonicalName());

    @Resource
    private SessionContext context;

    public SecurityEJBRunAsExtBean() {
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
    @RolesAllowed("Manager")
    public String manager() {
        return authenticate("Manager");
    }

    @Override
    @RolesAllowed("Employee")
    public String employee() {
        return authenticate("Employee");
    }

    @Override
    @RolesAllowed("DeclaredRole01")
    public String employeeAndManager() {
        return authenticate("EmployeeAndManager");
    }

}
