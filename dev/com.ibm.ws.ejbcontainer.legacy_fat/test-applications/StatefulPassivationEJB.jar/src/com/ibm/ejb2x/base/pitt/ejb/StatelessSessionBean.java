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

import javax.ejb.EJBObject;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

/**
 * This class implements a very simple session bean for use in testing
 * the deployment tool and session container. <p>
 * 
 * @author Chriss Stephens
 * @version $Id: StatelessSessionBean.java,v 1.9 1999/11/30 20:13:00 chriss Exp $
 */
@SuppressWarnings("serial")
public class StatelessSessionBean implements SessionBean {
    private SessionContext sessionContext;
    public int theValue;
    public String primaryKey;

    /**
     * Create a new instance of this session managed bean.
     */
    public StatelessSessionBean() {
        sessionContext = null;
        theValue = 0;
        printClassName("this", this);
    }

    // -------------------------------------------------
    // Methods defined by the StatelessSession interface.
    // -------------------------------------------------

    /**
     * Return EJBObject for this bean.
     */
    public EJBObject getEJBObject() {
        return sessionContext.getEJBObject();
    }

    // ----------------------------------------------
    // Methods defined by the SessionBean interface.
    // ----------------------------------------------

    public void ejbCreate() {
        System.out.println("ejbobject = " + sessionContext.getEJBObject());
    }

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

    // to test 93333
    private void printClassName(String name, Object o) {
        System.out.println(o.getClass().getName());
    }
}