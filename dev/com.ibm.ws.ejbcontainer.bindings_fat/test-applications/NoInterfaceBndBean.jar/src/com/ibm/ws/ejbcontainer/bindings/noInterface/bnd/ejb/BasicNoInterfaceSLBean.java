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
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;

@Stateless
public class BasicNoInterfaceSLBean {
    private static final String CLASS_NAME = BasicNoInterfaceSLBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @Resource(name = "beanName")
    String ivBeanName = "BasicNoInterfaceSLBean";

    @TransactionAttribute(NOT_SUPPORTED)
    public String getBeanName() {
        svLogger.info("getBeanName : " + ivBeanName);
        return ivBeanName;
    }
}
