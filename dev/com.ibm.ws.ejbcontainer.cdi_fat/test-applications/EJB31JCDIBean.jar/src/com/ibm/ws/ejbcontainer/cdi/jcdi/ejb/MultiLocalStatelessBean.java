/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.cdi.jcdi.ejb;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Stateless;

/**
 * Stateless bean with multiple local interfaces that should be able to lookup a BeanManager.
 **/
@Stateless(name = "MultiLocalStateless")
@Local({ MultiLocalStatelessOne.class, MultiLocalStatelessTwo.class })
public class MultiLocalStatelessBean {
    private static final String CLASS_NAME = MultiLocalStatelessBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @Resource(name = "EJBName")
    private String ivEJBName = "MultiLocalStateless";

    /**
     * Returns the EJB name.
     **/
    public String getName() {
        svLogger.info("- " + ivEJBName + ".getName() : ivEJBName");
        return ivEJBName;
    }

    /**
     * Returns the EJB name.
     **/
    public String getEjbName() {
        svLogger.info("- " + ivEJBName + ".getEjbName() : ivEJBName");
        return ivEJBName;
    }
}
