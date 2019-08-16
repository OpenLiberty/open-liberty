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
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * This class implements the simple entity bean used for testing the
 * container and deployment tools. <p>
 * 
 * @author Chriss Stephens
 * @version $Id: CMEntityBean.java,v 1.17 2001/02/09 19:27:42 amber Exp $
 */
@SuppressWarnings({ "unused", "serial" })
public class CMEntityBean implements SessionBean {
    public int theValue;
    public CMKey primaryKey;
    public String testId; // used to identify test and customize bean behavior
    public String value = null;

    private SessionContext sessionContext;

    /**
     * Create a new instance of a simple entity bean.
     */
    public CMEntityBean() {}

    // --------------------------------------
    // Methods defined by our EJB interface
    // --------------------------------------

    /**
     * Increment the value of the persistent counter. <p>
     * 
     * @return an <code>int</code> containing the value of the counter after the
     *         increment
     */
    public int increment() {
        theValue++;
        return theValue;
    }

    /**
     * Increment the value of the persistent counter. <p>
     * 
     * @return an <code>int</code> containing the value of the counter after the
     *         txMandatoryIncrement
     */
    public int txMandatoryIncrement() {
        theValue++;
        return theValue;
    }

    /**
     * Increment the value and if botch != 0 throw user defined exception.
     */
    public void incrementOrBotch(int botch) throws BeanException1, BeanException2 {
        if (botch == CMEntity.BOTCHED_BOTCH) {
            throw new RuntimeException();
        }

        if (botch == CMEntity.BOTCH_BEFORE_UPDATE) {
            throw new BeanException1();
        }

        theValue++;

        if (botch == CMEntity.BOTCH_AFTER_UPDATE) {
            throw new BeanException2();
        }
    }

    public int getValue() {
        return theValue;
    }

    /**
     * Regression test for CMVC defect 69492, verify that getRollbackOnly()
     * succeeds after setRollbackOnly().
     */

    public void testRollbackOnly() {
        SessionContext ec = sessionContext;
        sessionContext.setRollbackOnly();
    }

    /**
     * Set value of this bean.
     */
    public void setValue(int v) {
        theValue = v;
    }

    /**
     * Get value of this bean.
     */
    public CMKey getKey() {
        return primaryKey;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public void clearTestId() {
        testId = null;
    }

    /**
     * Test access to java:comp/env
     */
    public void testCompEnvAccess() {
        Context compEnvContext = null;

        try {
            compEnvContext = (Context) (new InitialContext().lookup("java:comp/env"));
        } catch (NamingException e) {
            throw new EJBException("lookup of comp/env failed", e);
        }

        String ppty1 = null;
        String ppty2 = null;

        // "Comp env context lookup successful. Looking up values ...");
        try {
            ppty1 = (String) compEnvContext.lookup("CMEntityProperty1");
            ppty2 = (String) compEnvContext.lookup("CMEntityProperty2");
        } catch (NamingException e) {
            throw new EJBException("comp env lookup failed", e);
        }

        // "Values lookup successful. Comparing to expected values ...");
        if (!ppty1.equals("cmEntityProperty1"))
            throw new EJBException("ppty1 does not equal expected value :" + ppty1);
        if (!ppty2.equals("cmEntityProperty2"))
            throw new EJBException("ppty2 does not equal expected value :" + ppty2);
    }

    /*
     * methods for testing fix for CMVC defect 100792
     */
    public String getValue_NotSupported()
    {
        return value;
    }

    public void setValue_NotSupported(String newValue)
    {
        this.value = newValue;
    }

    // ------------------------------------------------------
    // Boilerplate methods required by SessionBean interface
    // ------------------------------------------------------

    public void ejbCreate(CMKey pkey) {
        primaryKey = pkey;
    }

    @Override
    public void ejbActivate() {
        theValue = 0;
    }

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