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

import static javax.ejb.TransactionAttributeType.SUPPORTS;

import java.util.logging.Logger;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;

import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;

@Stateless
@Local(SLLa.class)
public class SLLaBean {
    private final static String CLASSNAME = SLLaBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    @SuppressWarnings({ "unused" })
    private static final long serialVersionUID = -7581936662924057564L;
    final static String BeanName = "SLLaBean";

    public void printMsg(String beanName, String msg) {
        svLogger.info("       " + beanName + " : " + msg);
    }

    /**
     * method1
     */
    public String method1(String arg1) {
        printMsg(BeanName, "----->method1 arg = " + arg1);
        return arg1;
    }

    /**
     * method2
     */
    @TransactionAttribute(SUPPORTS)
    public byte[] method2(String arg1) {
        printMsg(BeanName, "----->method2 arg = " + arg1);

        return FATTransactionHelper.getTransactionId();
    }
}