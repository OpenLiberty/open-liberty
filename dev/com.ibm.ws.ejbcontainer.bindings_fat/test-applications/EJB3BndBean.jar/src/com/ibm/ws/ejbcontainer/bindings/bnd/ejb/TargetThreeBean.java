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

import javax.ejb.EJBException;
import javax.ejb.SessionContext;

public class TargetThreeBean implements javax.ejb.SessionBean, LocalTargetThreeBiz1, LocalTargetThreeBiz2, LocalTargetThreeBiz3 {

    private static final long serialVersionUID = 1181647011303722826L;
    private javax.ejb.SessionContext mySessionCtx;
    final static String BeanName = "TargetOneBean";

    @Override
    public void ejbActivate() throws EJBException, RemoteException {

    }

    @Override
    public void ejbPassivate() throws EJBException, RemoteException {}

    @Override
    public void ejbRemove() {

    }

    public void ejbCreate() throws javax.ejb.CreateException {}

    public javax.ejb.SessionContext getSessionContext() {
        return mySessionCtx;
    }

    @Override
    public void setSessionContext(SessionContext ctx) {
        mySessionCtx = ctx;
    }

    @Override
    public String ping1() {
        return "pong";
    }

    @Override
    public String ping2() {
        return "pong";
    }

    @Override
    public String ping3() {
        return "pong";
    }

}
