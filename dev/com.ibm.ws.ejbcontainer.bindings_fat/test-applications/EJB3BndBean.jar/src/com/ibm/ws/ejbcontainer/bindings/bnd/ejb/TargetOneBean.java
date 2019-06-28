/*******************************************************************************
 * Copyright (c) 2007, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.bindings.bnd.ejb;

/**
 * Bean implementation class for Enterprise Bean: TargetOneBean
 */
public class TargetOneBean implements javax.ejb.SessionBean, LocalTargetOneBiz, RemoteTargetOneBiz {
    /**
    *
    */
    private static final long serialVersionUID = 1181647011303722826L;
    private javax.ejb.SessionContext mySessionCtx;
    final static String BeanName = "TargetOneBean";

    /**
     * getSessionContext
     */
    public javax.ejb.SessionContext getSessionContext() {
        printMsg(BeanName, "(getSessionContext)");
        return mySessionCtx;
    }

    /**
     * setSessionContext
     */
    @Override
    public void setSessionContext(javax.ejb.SessionContext ctx) {
        printMsg(BeanName, "(setSessionContext)");
        mySessionCtx = ctx;
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

    /**
     * Insert the method's description here.
     */
    public void printMsg(String beanName, String msg) {
        System.out.println("       " + beanName + " : " + msg);
    }

    /**
     * echo
     */
    @Override
    public String echo(String message) {
        printMsg(BeanName, "----->echo message = " + message);
        return message;
    }

}
