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
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;

/**
 * A 2.1 SFSB used in a EJB 3.0 module with exclusion of default interceptors
 * and use of a single class level interceptor. The rest of the bean is meant to
 * be just like it was when it was in a 2.1 module (simulating what a customer
 * might do).
 **/
public class CompStatefulLocalBean implements SessionBean {

    /**
     * Definitions for the logger
     */
    private final static String CLASSNAME = CompStatefulLocalBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private static final long serialVersionUID = 6312057079326361936L;

    /** Name of this class */
    private static final String CLASS_NAME = "CompStatefulLocalBean";

    private final static String SF_JNDI_NAME = "java:app/EJB3INTXBean/SFUnspecifiedContextBean!com.ibm.ejblite.interceptor.v32.xml.ejb.SFUnspecifiedLocal"; // TJB:
    // Task35

    private final static String SL_JNDI_NAME = "java:app/EJB3INTXBean/SLUnspecifiedContextBean!com.ibm.ejblite.interceptor.v32.xml.ejb.SLUnspecifiedLocal"; // TJB:
    // Task35

    @SuppressWarnings("unused")
    private SessionContext ivContext;

    /**
     * Default CTOR required by Serializable interface.
     */
    public CompStatefulLocalBean() {
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

    public boolean txRequiredLookup() {
        svLogger.info(CLASS_NAME + ".txRequiredLookup");
        try {
            InitialContext ictx = new InitialContext();
            final SFUnspecifiedLocal bean1 = (SFUnspecifiedLocal) ictx.lookup(SF_JNDI_NAME);
            bean1.doNothing();
            // bean1.remove();
            // bean1.destroy();
            final SLUnspecifiedLocal bean2 = (SLUnspecifiedLocal) ictx.lookup(SL_JNDI_NAME);
            bean2.doNothing(bean2);
            return FATTransactionHelper.isTransactionGlobal();
        } catch (NamingException e) {
            throw new EJBException("lookup failed: " + e, e);
        }
    }

    public boolean txNotSupportedLookup() {
        svLogger.info(CLASS_NAME + ".txNotSupportedLookup");
        try {
            InitialContext ictx = new InitialContext();
            final SFUnspecifiedLocal bean1 = (SFUnspecifiedLocal) ictx.lookup(SF_JNDI_NAME);
            bean1.doNothing();
            bean1.remove();
            // bean1.destroy();
            final SLUnspecifiedLocal bean2 = (SLUnspecifiedLocal) ictx.lookup(SL_JNDI_NAME);
            bean2.doNothing(bean2);
            return FATTransactionHelper.isTransactionLocal();
        } catch (NamingException e) {
            throw new EJBException("lookup failed: " + e, e);
        }
    }

}
