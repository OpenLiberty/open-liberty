/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

package com.ibm.ejb3x.BindingName.ejb;

import java.util.logging.Logger;

@SuppressWarnings("serial")
public class BindingNameBean implements javax.ejb.SessionBean, BindingNameIntf, RemoteBindingNameIntf {
    private final static String CLASS_NAME = BindingNameBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);
    private static final String BeanName = "BindingName";

    public BindingNameBean() {}

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
        return "SimpleBindingNameBean.toString()";
    }

    @Override
    public String foo() {
        return "bar";
    }
}
