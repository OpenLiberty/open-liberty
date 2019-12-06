/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb3x.HomeBindingName.ejb;

import java.util.logging.Logger;

@SuppressWarnings("serial")
public class LocalHomeBindingNameBean implements javax.ejb.SessionBean {
    private final static String CLASS_NAME = LocalHomeBindingNameBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);
    private static final String BeanName = "LocalHomeBindingName";

    public LocalHomeBindingNameBean() {}

    /**
     * setSessionContext
     */
    @Override
    public void setSessionContext(javax.ejb.SessionContext ctx) {
        printMsg(BeanName, "(setSessionContext)");
    }

    /**
     * ejbCreate
     */
    public void ejbCreate() throws javax.ejb.CreateException {
        printMsg(BeanName, "(ejbCreate)");
    }

    /**
     * ejbActivate
     */
    @Override
    public void ejbActivate() {
        printMsg(BeanName, "(ejbActivate)");
        throw new IllegalStateException("Should never be called");
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

    @Override
    public String toString() {
        return "LocalHomeBindingNameBean.toString()";
    }
}
