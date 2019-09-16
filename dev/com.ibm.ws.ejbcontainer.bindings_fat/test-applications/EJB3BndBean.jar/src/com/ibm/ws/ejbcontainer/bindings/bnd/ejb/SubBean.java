/*******************************************************************************
 * Copyright (c) 2007, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.bindings.bnd.ejb;

import java.rmi.RemoteException;

import javax.annotation.Resource;
import javax.ejb.CreateException;
import javax.ejb.LocalHome;
import javax.ejb.RemoteHome;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;

@Stateful(name = "SubBean")
@LocalHome(SubLocalHome.class)
@RemoteHome(SubRemoteHome.class)
public class SubBean implements SessionBean {
    private static final long serialVersionUID = 7435573037724452299L;

    @Resource
    private SessionContext ivContext;

    public String ping() {
        return "pong";
    }

    public void ejbCreate() throws CreateException, java.rmi.RemoteException {}

    @Override
    public void ejbActivate() throws RemoteException {}

    @Override
    public void ejbPassivate() throws RemoteException {}

    @Override
    public void ejbRemove() throws RemoteException {}

    @Override
    public void setSessionContext(SessionContext sc) {
        ivContext = sc;
    }
}
