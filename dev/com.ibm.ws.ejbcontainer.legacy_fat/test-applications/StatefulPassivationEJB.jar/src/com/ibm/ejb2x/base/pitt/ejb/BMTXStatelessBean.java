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

package com.ibm.ejb2x.base.pitt.ejb;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.transaction.UserTransaction;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

/**
 * This class implements a very simple stateless session bean for use
 * in testing support for bean managed transactions in the EJS runtime. <p>
 *
 * It expects to be deployed as a stateless session bean using bean
 * managed transactions. <p>
 */
@SuppressWarnings({ "serial" })
public class BMTXStatelessBean implements SessionBean {
    private SessionContext sessionContext;

    /**
     * Create a new instance of this session bean.
     */
    public BMTXStatelessBean() {
        sessionContext = null;
    }

    // -------------------------------------------------
    //
    // Methods defined by the BMTXStateless interface.
    //
    // -------------------------------------------------

    /**
     * Leave a bean managed transaction active when this method completes.
     * Container is supposed to rollback any such "left-over" transactions.
     * Provides regression test for CMVC defect 70616.
     */
    public void regressionMethod70616() {
        // -------------------------------
        // Start transaction and return.
        // -------------------------------

        try {
            UserTransaction userTx = sessionContext.getUserTransaction();
            userTx.begin();
        } catch (Exception ex) {
            throw new EJBException("transaction begin failed", ex);
        }
    }

    /**
     * Test lookup and use of UserTransaction object from java:comp namespace.
     */
    public void testAccessToUserTransaction() {
        UserTransaction ut = null;

        try {
            ut = FATHelper.lookupUserTransaction();
        } catch (NamingException ex) {
            throw new EJBException("lookup of UserTransaction failed", ex);
        }

        CMKey pkey = new CMKey("Life is Wonderful (yeah, right!)");
        CMEntity b = null;
        CMEntityHome cmHome = null;
        @SuppressWarnings("unused")
        int result = 0;

        try {
            ut.begin();
        } catch (Exception ex) {
            throw new EJBException("ut.begin failed", ex);
        }

        try {
            createInitialContext();
            Object tmpObj = ic.lookup("CMEntity");
            cmHome = (CMEntityHome) PortableRemoteObject.narrow(tmpObj, CMEntityHome.class); // d163474
            //cmHome = (CMEntityHome) PortableRemoteObject.narrow(new InitialContext().lookup("java:app/StatefulPassivationEJB/CMEntity"), CMEntityHome.class);
        } catch (NamingException ex) {
            throw new EJBException("lookup of CMEntity home failed", ex);
        }

        try {
            b = cmHome.create(pkey);
            result = b.increment();
        } catch (Exception ex) {
            throw new EJBException("create failed", ex);
        }

        try {
            ut.commit();
        } catch (Exception ex) {
            throw new EJBException("ut.begin failed", ex);
        }

        try {
            b.remove();
        } catch (Exception ex) {
            throw new EJBException("remove failed", ex);
        }

        try {
            ut.begin();
        } catch (Exception ex) {
            throw new EJBException("ut.begin failed", ex);
        }

        try {
            b = cmHome.create(pkey);
            result = b.increment();
        } catch (Exception ex) {
            throw new EJBException("create failed", ex);
        }

        try {
            ut.rollback();
        } catch (Exception ex) {
            throw new EJBException("ut.begin failed", ex);
        }
    }

    private transient InitialContext ic = null;

    private synchronized void createInitialContext() {
        if (ic == null) {
            try {
                ic = new InitialContext();
            } catch (Exception ex) {
                throw new EJBException("Failure creating initial context", ex);
            }
        }
    }

    // ----------------------------------------------
    //
    // Methods defined by the SessionBean interface.
    //
    // ----------------------------------------------

    public void ejbCreate() {}

    @Override
    public void ejbActivate() {}

    @Override
    public void ejbPassivate() {}

    @Override
    public void ejbRemove() {}

    /**
     * Set the session context for use by this bean.
     */
    @Override
    public void setSessionContext(SessionContext ctx) {
        sessionContext = ctx;
    }
}