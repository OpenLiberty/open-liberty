/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.home2x.ejb;

import java.rmi.RemoteException;
import java.util.List;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

@SuppressWarnings("serial")
public class TestEJBHome2xBean implements SessionBean {
    private SessionContext context;

    public String echo(String s) {
        return s;
    }

    public TestEJBHome2x lookupTestEJBHome(String s) {
        return (TestEJBHome2x) context.lookup(s);
    }

    public TestEJBObject2x getSessionContextEJBObject() {
        return (TestEJBObject2x) context.getEJBObject();
    }

    public TestEJBHome2x getSessionContextEJBHome() {
        // The spec does not require this cast to work without a narrow, but
        // this is likely an oversight given it does require getEJBObject to be
        // directly castable.
        return (TestEJBHome2x) context.getEJBHome();
    }

    public List<?> testWriteValue(List<?> list) {
        return list;
    }

    public RMICCompatReturn testRecursiveRMIC(RMICCompatParam param) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSessionContext(SessionContext context) throws EJBException, RemoteException {
        this.context = context;
    }

    @Override
    public void ejbActivate() {
    }

    @Override
    public void ejbPassivate() {
    }

    @Override
    public void ejbRemove() {
    }
}
