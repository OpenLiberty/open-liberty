/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.async.fat.config.ejb;

import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

/**
 * Bean implementation class for Enterprise Bean: StatelessLocal
 **/
@Stateless
@Local(StatelessLocal.class)
public class StatelessLocalBean {

    @Resource
    private SessionContext context;

    public final static String CLASS_NAME = StatelessLocalBean.class.getName();
    private final static Logger logger = Logger.getLogger(CLASS_NAME);

    @Asynchronous
    @RolesAllowed("Role1")
    public Future<String> role1Only() {
        logger.info("in role1Only");

        return new AsyncResult<String>(authenticate());
    }

    @Asynchronous
    @RolesAllowed("Role2")
    public Future<String> role2Only() {
        logger.info("in role2Only");

        return new AsyncResult<String>(authenticate());
    }

    @RolesAllowed("Role3")
    public String role3Only() {
        logger.info("in role3Only");
        return authenticate();
    }

    @Asynchronous
    @RolesAllowed({ "Role1", "Role3" })
    public Future<String> role1or3() {
        logger.info("in role1or3");

        return new AsyncResult<String>(authenticate());
    }

    private String authenticate() {
        java.security.Principal principal = context.getCallerPrincipal();
        String principalName = null;
        if (principal != null) {
            principalName = principal.getName();
        } else {
            principalName = "null";
        }

        return principalName;
    }

}
