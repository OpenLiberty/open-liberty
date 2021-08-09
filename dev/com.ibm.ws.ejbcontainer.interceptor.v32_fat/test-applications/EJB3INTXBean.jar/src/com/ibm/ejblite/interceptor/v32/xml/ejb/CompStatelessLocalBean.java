/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejblite.interceptor.v32.xml.ejb;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;

/**
 * A 2.1 SLSB used in a EJB 3.0 module with exclusion of default interceptors
 * and use of a single class level interceptor. The rest of the bean is meant to
 * be just like it was when it was in a 2.1 module (simulating what a customer
 * might do).
 **/
public class CompStatelessLocalBean implements SessionBean {

    /**
     * Definitions for the logger
     */
    private final static String CLASSNAME = CompStatelessLocalBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private static final long serialVersionUID = 299264620002245507L;

    /** Name of this class */
    private static final String CLASS_NAME = "CompStatelessLocalBean";

    @SuppressWarnings("unused")
    private SessionContext ivContext;

    /**
     * Default CTOR required by Serializable interface.
     */
    public CompStatelessLocalBean() {
        // intentionally left blank.
    }

    /*
    *
    */
    public void ejbCreate() throws CreateException {
        try {
            ResultsLocal results = ResultsLocalBean.getSFBean();
            boolean unspecifiedTX = FATTransactionHelper.isUnspecifiedTransactionContext();
            results.addTransactionContext(CLASS_NAME, "postConstruct", unspecifiedTX);
        } catch (Exception e) {
            throw new EJBException("unexpected Throwable", e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ejb.SessionBean#ejbActivate()
     */
    @Override
    public void ejbActivate() {
        try {
            ResultsLocal results = ResultsLocalBean.getSFBean();
            boolean unspecifiedTX = FATTransactionHelper.isUnspecifiedTransactionContext();
            results.addTransactionContext(CLASS_NAME, "postActivate", unspecifiedTX);
        } catch (Exception e) {
            throw new EJBException("unexpected Throwable", e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ejb.SessionBean#ejbPassivate()
     */
    @Override
    public void ejbPassivate() {
        try {
            ResultsLocal results = ResultsLocalBean.getSFBean();
            boolean unspecifiedTX = FATTransactionHelper.isUnspecifiedTransactionContext();
            results.addTransactionContext(CLASS_NAME, "prePassivate", unspecifiedTX);
        } catch (Exception e) {
            throw new EJBException("unexpected Throwable", e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ejb.SessionBean#ejbRemove()
     */
    @Override
    public void ejbRemove() {
        try {
            ResultsLocal results = ResultsLocalBean.getSFBean();
            boolean unspecifiedTX = FATTransactionHelper.isUnspecifiedTransactionContext();
            results.addTransactionContext(CLASS_NAME, "preDestroy", unspecifiedTX);
        } catch (Exception e) {
            throw new EJBException("unexpected Throwable", e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ejb.SessionBean#setSessionContext(javax.ejb.SessionContext)
     */
    @Override
    public void setSessionContext(SessionContext sc) throws EJBException, RemoteException {
        ivContext = sc;
    }

    public boolean doNothing() {
        svLogger.info(CLASS_NAME + ".doNothing");
        return FATTransactionHelper.isTransactionGlobal();
    }

    public boolean doNothing(CompStatelessLocal bean) {
        svLogger.info(CLASS_NAME + ".doNothing(bean)");
        boolean global = bean.doNothing();
        return (global && FATTransactionHelper.isTransactionGlobal());
    }

    public void discard() {
        throw new RuntimeException(CLASS_NAME
                                   + ".discard throwing RuntimeException to force discard of bean");
    }

}
