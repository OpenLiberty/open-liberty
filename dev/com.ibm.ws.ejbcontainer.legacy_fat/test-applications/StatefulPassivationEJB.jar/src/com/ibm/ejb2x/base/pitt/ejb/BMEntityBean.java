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

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

/**
 * This class implements a simple bean managed entity bean used for
 * testing the container and deployment tools. <p>
 * 
 * @author Chriss Stephens
 * @version $Id: BMEntityBean.java,v 1.14 1999/11/30 20:10:25 chriss Exp $
 */
@SuppressWarnings({ "serial" })
public class BMEntityBean implements SessionBean {
    @SuppressWarnings("unused")
    private SessionContext sessionContext;
    private int theValue;
    @SuppressWarnings("unused")
    private String name;
    private int nonPersistent;

    /**
     * Create a new instance of a bean managed entity bean.
     */
    public BMEntityBean() {}

    // --------------------------------------
    // Methods defined by our EJB interface
    // --------------------------------------

    /**
     * Increment the value of the persistent counter. <p>
     * 
     * This method, as hinted at by its name, must be deployed with the
     * TX_REQUIRES_NEW transaction attribute. <p>
     * 
     * @return an <code>int</code> containing the value of the counter after the
     *         increment
     */
    public int txNewIncrement() {
        return ++theValue;
    }

    /**
     * Return value of nonpersistent which should always be the same.
     */
    public int getNonpersistent() {
        return nonPersistent;
    }

    // ------------------------------------------------------
    // Boilerplate methods required by EntityBean interface
    // ------------------------------------------------------
    public void ejbCreate(String name) throws CreateException {
        this.name = name;
    }

    @Override
    public void ejbActivate() {}

    @Override
    public void ejbPassivate() {}

    @Override
    public void setSessionContext(SessionContext ctx) {
        sessionContext = ctx;
    }

    /*
     * The container calls this method when the bean is removed from.
     */
    @Override
    public void ejbRemove() {}
}