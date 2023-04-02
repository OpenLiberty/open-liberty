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
package com.ibm.ws.ejbcontainer.mdb.jms.mix.ejb;

import java.util.logging.Logger;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;

public class SLLaBean implements SessionBean {
    private final static String CLASSNAME = SLLaBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private static final long serialVersionUID = -7581936662924057564L;
    private SessionContext mySessionCtx;
    final static String BeanName = "SLLaBean";

    /**
     * getSessionContext
     */
    public SessionContext getSessionContext() {
        printMsg(BeanName, "(getSessionContext)");
        return mySessionCtx;
    }

    /**
     * setSessionContext
     */
    @Override
    public void setSessionContext(SessionContext ctx) {
        printMsg(BeanName, "(setSessionContext)");
        mySessionCtx = ctx;
    }

    /**
     * ejbCreate
     */
    public void ejbCreate() throws CreateException {
        printMsg(BeanName, "(ejbCreate)");
    }

    /**
     * ejbActivate
     */
    @Override
    public void ejbActivate() {
        printMsg(BeanName, "(ejbActivate)");
    }

    /**
     * ejbPassivate
     */
    @Override
    public void ejbPassivate() {
        printMsg(BeanName, "(ejbPassivate)");
    }

    /**
     * ejbRemove
     */
    @Override
    public void ejbRemove() {
        printMsg(BeanName, "(ejbRemove)");
    }

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
    public byte[] method2(String arg1) {
        printMsg(BeanName, "----->method2 arg = " + arg1);
        return FATTransactionHelper.getTransactionId();
    }
}