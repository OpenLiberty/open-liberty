/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;

/**
 * EJB (remote) interface for basic Container Managed Transaction Stateless
 * Session bean.
 **/
public interface BasicCMTStatelessEJB extends EJBObject {
    public void tx_Default() throws RemoteException;

    public void tx_Required() throws RemoteException;

    public void tx_NotSupported() throws RemoteException;

    public void tx_RequiresNew() throws RemoteException;

    public void tx_Supports() throws RemoteException;

    public void tx_Never() throws RemoteException;

    public void tx_Mandatory() throws RemoteException;

    public void test_getBusinessObject(boolean businessInterface) throws RemoteException;
}