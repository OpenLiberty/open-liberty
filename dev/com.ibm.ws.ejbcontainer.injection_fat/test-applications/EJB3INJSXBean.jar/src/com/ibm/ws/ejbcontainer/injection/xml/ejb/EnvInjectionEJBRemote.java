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

package com.ibm.ws.ejbcontainer.injection.xml.ejb;

/**
 * Compatibility EJBRemote interface with methods to verify Environment Injection.
 **/
public interface EnvInjectionEJBRemote extends javax.ejb.EJBObject {
    /**
     * Verify Environment Injection (field or method) occurred properly.
     **/
    public String verifyEnvInjection(int testpoint) throws java.rmi.RemoteException;

    /**
     * Verify No Environment Injection (field or method) occurred when
     * an method is called using an instance from the pool (sl) or cache (sf).
     **/
    public String verifyNoEnvInjection(int testpoint) throws java.rmi.RemoteException;

    /**
     * Provides a means to destroy a SLSB. Should throw unchecked EJBException
     * 
     * @throws java.rmi.RemoteException
     */
    public void discardInstance() throws java.rmi.RemoteException;

}