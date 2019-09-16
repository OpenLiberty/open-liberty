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

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * Bean implementation class for Singleton Enterprise Bean
 **/
@Startup
@Singleton
@RunAs("Role3")
public class SingletonLocalBean {

    @Resource
    private SessionContext context;

    @EJB
    StatelessLocal ivBean;

    public final static String CLASS_NAME = SingletonLocalBean.class.getName();
    private final static Logger logger = Logger.getLogger(CLASS_NAME);

    @PostConstruct
    @PermitAll
    private void postConstruct() {
        logger.info("> @Startup @Singleton PostConstruct");
        // FIXME(131630) - cannot be enabled until security fixes problem where not ready during app start
        // logger.info("  principal = " + ivBean.role3Only());
        // ivBean.role1or3();
        logger.info("< @Startup @Singleton PostConstruct");
    }

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
