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
package com.ibm.ws.ejbcontainer.mdb.jms.xml.ejb;

import static javax.ejb.TransactionAttributeType.SUPPORTS;

import java.util.logging.Logger;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.SessionSynchronization;
import javax.ejb.TransactionAttribute;

public class SFBean implements SessionBean, SessionSynchronization {
    private final static String CLASSNAME = SFBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private static final long serialVersionUID = 6126860989973139868L;
    private SessionContext mySessionCtx;
    final static String BeanName = "SFBean";

    public int intValue;

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
     * Returns the intValue.
     *
     * @return int
     */
    public int getIntValue() {
        printMsg(BeanName, "----->getIntValue = " + intValue);
        return intValue;
    }

    /**
     * Sets the intValue.
     *
     * @param intValue The intValue to set
     */
    public void setIntValue(int intValue) {
        printMsg(BeanName, "----->setIntValue = " + intValue);
        this.intValue = intValue;
    }

    /**
     * Increments the intValue.
     */
    @TransactionAttribute(SUPPORTS)
    public void incrementInt() {
        this.intValue++;
        printMsg(BeanName, "----->incrementInt = " + this.intValue);
    }

    /**
     * ejbCreate
     */
    public void ejbCreate() throws CreateException {
        printMsg(BeanName, "(ejbCreate)");
    }

    /**
     * ejbCreate with long values
     */
    public void ejbCreate(int intValue) throws CreateException {
        printMsg(BeanName, " intValue     = " + intValue);

        // Set my instance variables
        this.setIntValue(intValue);
    }

    /**
     * ejbPostCreate with long values
     */
    public void ejbPostCreate(int intValue) throws CreateException {
        printMsg(BeanName, "(ejbPostCreate with long values)");
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
        svLogger.info("       " + beanName + " : " + msg);
    }

    /**
     * method1
     */
    public String method1(String arg1) {
        printMsg(BeanName, "-----> method1 arg = " + arg1);
        return arg1;
    }

    @Override
    public void afterBegin() throws EJBException {
    }

    @Override
    public void afterCompletion(boolean commit) throws EJBException {
        if (!commit) {
            this.intValue--;
            printMsg(BeanName, "----->rollback intValue = " + this.intValue);
        }
    }

    @Override
    public void beforeCompletion() throws EJBException {
    }
}