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

import java.rmi.RemoteException;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.SessionSynchronization;

/**
 * This class implements a very simple session bean for use in testing
 * the deployment tool and session container. <p>
 * 
 * It expects to be deployed as a stateful session bean. <p>
 * 
 * @author Chriss Stephens
 * @version $Id: StatefulSessionBean.java,v 1.21 1999/11/30 20:10:26 chriss Exp $
 */
public class StatefulSessionBean implements SessionBean, SessionSynchronization {
    private static final long serialVersionUID = 2431058061045995311L;
    private SessionContext sessionContext;

    /**
     * Should this bean throw an exception when remove is attempted?
     */
    private final boolean removeFailure = false;

    /**
     * Should this bean call setRollbackOnly() during beforeCompletion()?
     */
    private final boolean botchOnCompletion = false;
    public int theValue;
    public String primaryKey;

    /**
     * Create a new instance of this session managed bean.
     */
    public StatefulSessionBean() {
        sessionContext = null;
        theValue = 0;
    }

    // -------------------------------------------------
    // Methods defined by the StatefulSession interface.
    // -------------------------------------------------

    /**
     * Increment the persistent state associated with this stateful session bean.
     */
    public int increment() {
        return ++theValue;
    }

    /**
     * Test method to make sure that "local" tx does not flow to clients. Updates
     * to first bean must be persistent even though call to second bean fails.
     */
    public void txNotSupportedDelegate(CMEntity b1, CMEntity b2) throws BeanException1, BeanException2 {
        try {
            b1.increment();
            b2.incrementOrBotch(CMEntity.BOTCH_BEFORE_UPDATE);
        } catch (RemoteException ex) {
            throw new EJBException(" ", ex);
        }
    }

    /**
     * Mark current transaction for rollback.
     */
    public void rollbackOnly() {
        sessionContext.setRollbackOnly();
    }

    /**
     * Delegate getNonPersistent call to given entity bean.
     */
    public int getNonpersistent(BMEntity bean) {
        try {
            return bean.getNonpersistent();
        } catch (RemoteException ex) {
            throw new EJBException("", ex);
        }
    }

    // ----------------------------------------------
    // Methods defined by the SessionBean interface.
    // ----------------------------------------------

    public void ejbCreate() {}

    @Override
    public void ejbActivate() {}

    @Override
    public void ejbPassivate() {}

    @Override
    public void ejbRemove() {
        if (removeFailure) {
            throw new EJBException("cannot remove this session bean");
        }
    }

    /**
     * Set the session context for use by this bean.
     */
    @Override
    public void setSessionContext(SessionContext ctx) {
        sessionContext = ctx;
    }

    // //////////////////////////////////////////////////////////////////
    //
    // SessionSynchronization interface
    //

    @Override
    public void afterBegin() {}

    @Override
    public void beforeCompletion() {
        if (botchOnCompletion) {
            sessionContext.setRollbackOnly();
        }
    }

    @Override
    public void afterCompletion(boolean committed) {}
}