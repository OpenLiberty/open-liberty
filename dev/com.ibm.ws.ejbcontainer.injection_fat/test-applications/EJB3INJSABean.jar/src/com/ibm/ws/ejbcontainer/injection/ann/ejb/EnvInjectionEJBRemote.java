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

import javax.ejb.EJBObject;

/**
 * Compatibility EJBObject interface with methods to verify Environment Injection.
 **/
public interface EnvInjectionEJBRemote extends EJBObject {
    /**
     * Verify Environment Injection (field or method) occurred properly.
     **/
    public String verifyEnvInjection(int testpoint) throws RemoteException;

    /**
     * Verify No Environment Injection (field or method) occurred when
     * an method is called using an instance from the pool (sl) or cache (sf).
     **/
    public String verifyNoEnvInjection(int testpoint) throws RemoteException;

    /**
     * Clean up the bean if it is a SFSB
     */
    public void finish() throws RemoteException;

    public void discardInstance() throws RemoteException;
}
