/*******************************************************************************
 * Copyright (c) 2002, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejb2x.base.cache.ejb;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.SessionSynchronization;

/**
 * Bean implementation for a Stateful Session bean that implements the
 * SessionSynchronization interface and may be configured with different
 * Activation policies (ONCE, TRANSACTION).
 **/
public class StatefulSynchBean extends StatefulBean implements SessionBean, SessionSynchronization {
    private static final long serialVersionUID = -5324340825906642707L;

    @Override
    public void ejbCreate() throws CreateException {
        super.ejbCreate();
    }

    @Override
    public void ejbCreate(String message) throws CreateException {
        super.ejbCreate(message);
    }

    @Override
    public void setMessage(String message) {
        super.setMessage(message);
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }

    public StatefulSynchBean() {}

    @Override
    public void ejbRemove() {}

    @Override
    public void ejbActivate() {}

    @Override
    public void ejbPassivate() {}

    @Override
    public void setSessionContext(SessionContext sc) {}

    // --------------------------------------------------------------------------
    // SessionSynchronization Interface methods
    // --------------------------------------------------------------------------
    @Override
    public void afterBegin() {
        System.out.println("afterBegin : " + ivMessage);
    }

    @Override
    public void beforeCompletion() {
        System.out.println("beforeCompletion : " + ivMessage);
    }

    @Override
    public void afterCompletion(boolean flag) {
        System.out.println("afterCompletion : " + ivMessage);
    }
}