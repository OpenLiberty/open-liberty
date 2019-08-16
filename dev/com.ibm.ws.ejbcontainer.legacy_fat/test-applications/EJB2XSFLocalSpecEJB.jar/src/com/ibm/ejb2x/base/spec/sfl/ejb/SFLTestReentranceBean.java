/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb2x.base.spec.sfl.ejb;

import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.ejb.SessionContext;

/**
 * Bean implementation class for Enterprise Bean: SFLTestReentranceBean
 */
@SuppressWarnings("serial")
public class SFLTestReentranceBean implements javax.ejb.SessionBean {
    private final static String CLASS_NAME = SFLTestReentranceBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private SessionContext mySessionCtx;
    final static String BeanName = "SFLTestReentranceBean";

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
     * unsetSessionContext
     */
    public void unsetSessionContext() {
        printMsg(BeanName, "(unsetSessionContext)");
        mySessionCtx = null;
    }

    /**
     * ejbCreate
     */
    public void ejbCreate() throws javax.ejb.CreateException {
        printMsg(BeanName, "(ejbCreate)");
    }

    /**
     * ejbPostCreate
     */
    public void ejbPostCreate() throws javax.ejb.CreateException {
        printMsg(BeanName, "(ejbPostCreate)");
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
     * printMsg
     */
    public void printMsg(String beanName, String msg) {
        svLogger.info("       " + beanName + " : " + msg);
    }

    /**
     * Call self recursively to cause an exception
     */
    public int callNonRecursiveSelf(int level, SFLTestReentrance ejb1) throws SFLApplException {
        if (level == 1) {
            return 1;
        }
        try {
            svLogger.info(">>>>> " + ejb1.getClass().getName());

            return ejb1.callNonRecursiveSelf(--level, ejb1) + 1;
        } catch (EJBException e) {
            throw new SFLApplException(true, e, "Caught expectred " + e.getClass().getName());
        } catch (Throwable t) {
            throw new SFLApplException(false, t, "Caught unexception " + t.getClass().getName());
        }
    }

    /**
     * Call self recursively n times
     *
     * @return number of recursive call
     */
    public int callRecursiveSelf(int level, SFLTestReentrance ejb1) throws SFLApplException {
        if (level == 1) {
            return 1;
        }
        try {
            svLogger.info(">>>>> " + ejb1.getClass().getName());

            return ejb1.callRecursiveSelf(--level, ejb1) + 1;
        } catch (Throwable t) {
            throw new SFLApplException(false, t, "Caught unexception " + t.getClass().getName());
        }
    }
}
