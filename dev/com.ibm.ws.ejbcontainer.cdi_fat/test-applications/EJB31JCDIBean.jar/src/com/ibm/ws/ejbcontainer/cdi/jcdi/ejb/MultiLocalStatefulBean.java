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
import javax.ejb.LocalBean;
import javax.ejb.Stateful;

/**
 * Stateful bean with multiple local interfaces.
 **/
@Stateful(name = "MultiLocalStateful")
@LocalBean
@Local({ MultiLocalStatefulOne.class, MultiLocalStatefulTwo.class })
public class MultiLocalStatefulBean {
    private static final String CLASS_NAME = MultiLocalStatefulBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @Resource(name = "EJBName")
    private String ivEJBName = "MultiLocalStateful";

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
