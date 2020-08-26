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

import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;

/**
 * Bean implementation class for Enterprise Bean
 */

// @Stateless = see ejb-jar.xml
public class SecurityEJBMCRunAsBean extends SecurityEJBBeanBase implements SecurityEJBRunAsInterface {

    private static final Class<?> c = SecurityEJBMCRunAsBean.class;
    protected Logger logger = Logger.getLogger(c.getCanonicalName());

//    @Resource - see ejb-jar.xml
    private SessionContext context;

    public SecurityEJBMCRunAsBean() {
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
    @RolesAllowed({ "Employee", "Manager" })
    public String employeeAndManager() {
        return authenticate("Employee");
    }

}
