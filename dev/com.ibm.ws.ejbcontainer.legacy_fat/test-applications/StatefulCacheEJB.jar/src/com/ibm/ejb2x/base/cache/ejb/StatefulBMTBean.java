/*******************************************************************************
 * Copyright (c) 2002, 2021 IBM Corporation and others.
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
package com.ibm.ejb2x.base.cache.ejb;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

/**
 * Bean implementation for a BMT Stateful Session bean that may be
 * configured with different Activation policies (ONCE, TRANSACTION).
 **/
public class StatefulBMTBean extends StatefulBean implements SessionBean {
    private static final long serialVersionUID = -1214626235299907635L;

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

    public StatefulBMTBean() {}

    @Override
    public void ejbRemove() {}

    @Override
    public void ejbActivate() {}

    @Override
    public void ejbPassivate() {}

    @Override
    public void setSessionContext(SessionContext sc) {}
}