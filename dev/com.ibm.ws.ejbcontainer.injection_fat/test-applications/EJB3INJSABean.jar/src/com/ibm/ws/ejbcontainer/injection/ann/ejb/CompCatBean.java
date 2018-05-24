/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.injection.ann.ejb;

import java.rmi.RemoteException;

import javax.annotation.Resource;
import javax.ejb.CreateException;
import javax.ejb.LocalHome;
import javax.ejb.Remove;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;

/**
 * Component/Compatibility Stateful Bean implementation for testing EJB injection.
 **/
@Stateful(name = "CompCat")
@LocalHome(CatEJBLocalHome.class)
public class CompCatBean extends Animal implements SessionBean {
    private static final long serialVersionUID = -2749018556167868922L;

    @Resource
    private SessionContext ivContext;

    @Override
    public String whatAmI() {
        return "I am a cat.";
    }

    @Override
    public String careInst() {
        return "Give me milk and tuna.";
    }

    @Override
    public String favToy() {
        return "Just a ball of string.";
    }

    public String catDef() // d452259
    {
        return "Cat: any of several carnivores of the family Felidae.";
    }

    /** Remove method **/
    @Remove
    public void finish() {
        // Intentionally blank
    }

    // Provided for compatibility with SLSB
    public void discardInstance() {
        finish();
    }

    public CompCatBean() {
        // Intentionally blank
    }

    public void ejbCreate() throws CreateException, RemoteException {
        // Intentionally blank
    }

    @Override
    public void ejbRemove() throws RemoteException {
        // Intentionally blank
    }

    @Override
    public void ejbActivate() throws RemoteException {
        // Intentionally blank
    }

    @Override
    public void ejbPassivate() throws RemoteException {
        // Intentionally blank
    }

    @Override
    public void setSessionContext(SessionContext sc) {
        ivContext = sc;
    }
}
