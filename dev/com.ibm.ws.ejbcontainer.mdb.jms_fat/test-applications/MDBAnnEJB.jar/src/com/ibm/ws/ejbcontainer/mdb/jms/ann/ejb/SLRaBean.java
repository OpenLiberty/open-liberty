/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.mdb.jms.ann.ejb;

import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;

import java.util.logging.Logger;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;

@Stateless
@Remote(SLRa.class)
public class SLRaBean {
    private final static String CLASSNAME = SLRaBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    @SuppressWarnings("unused")
    private static final long serialVersionUID = 1181647011303722826L;
    final static String BeanName = "SLRaBean";

    /**
     * Insert the method's description here.
     */
    public void printMsg(String beanName, String msg) {
        svLogger.info("       " + beanName + " : " + msg);
    }

    /**
     * method1
     */
    @TransactionAttribute(NOT_SUPPORTED)
    public String method1(String arg1) {
        printMsg(BeanName, "----->method1 arg = " + arg1);
        return arg1;
    }
}