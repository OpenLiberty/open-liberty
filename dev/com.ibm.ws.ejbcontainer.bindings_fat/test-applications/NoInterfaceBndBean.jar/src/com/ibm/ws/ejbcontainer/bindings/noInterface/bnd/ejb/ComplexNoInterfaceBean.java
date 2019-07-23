/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.bindings.noInterface.bnd.ejb;

import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.TransactionAttribute;

public class ComplexNoInterfaceBean {
    private static final String CLASS_NAME = ComplexNoInterfaceBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @Resource(name = "beanName")
    String ivBeanName;

    @TransactionAttribute(NOT_SUPPORTED)
    public String getBeanName() {
        svLogger.info("getBeanName : " + ivBeanName);
        return ivBeanName;
    }

    @TransactionAttribute(NOT_SUPPORTED)
    public String localMethod(String text) {
        String rtnVal = "localMethod : " + ivBeanName + ":" + text;
        svLogger.info(rtnVal);
        return rtnVal;
    }

    @TransactionAttribute(NOT_SUPPORTED)
    public String remoteMethod(String text) {
        String rtnVal = "remoteMethod : " + ivBeanName + ":" + text;
        svLogger.info(rtnVal);
        return rtnVal;
    }

    // Required for Stateful beans with a home interface (Init method)
    public void initialize() {
        svLogger.info("initialize : " + ivBeanName);
    }
}
