/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.crossapp.home2x.ejb;

import java.rmi.RemoteException;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import com.ibm.ws.ejbcontainer.remote.fat.crossapp.shared.CrossAppEJBHome2x;
import com.ibm.ws.ejbcontainer.remote.fat.crossapp.shared.CrossAppEJBObject2x;

@SuppressWarnings("serial")
public class CrossAppEJBHome2xBean implements SessionBean {
    private SessionContext context;

    public String echo(String s) {
        return s;
    }

    public CrossAppEJBHome2x lookupTestEJBHome(String s) {
        return (CrossAppEJBHome2x) context.lookup(s);
    }

    public CrossAppEJBObject2x getSessionContextEJBObject() {
        return (CrossAppEJBObject2x) context.getEJBObject();
    }

    public CrossAppEJBHome2x getSessionContextEJBHome() {
        // The spec does not require this cast to work without a narrow, but
        // this is likely an oversight given it does require getEJBObject to be
        // directly castable.
        return (CrossAppEJBHome2x) context.getEJBHome();
    }

    @Override
    public void setSessionContext(SessionContext context) throws EJBException, RemoteException {
        this.context = context;
    }

    @Override
    public void ejbActivate() {}

    @Override
    public void ejbPassivate() {}

    @Override
    public void ejbRemove() {}
}
