/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.ann.sf.ejb;

import javax.ejb.EJBObject;

/**
 * EJBObject interface for advanced Container Managed Transaction Stateful
 * Session bean.
 **/
public interface AdvCMTStatefulEJBRemote extends EJBObject {
    public void tx_Default() throws java.rmi.RemoteException;

    public void tx_Required() throws java.rmi.RemoteException;

    public void tx_NotSupported() throws java.rmi.RemoteException;

    public void tx_RequiresNew() throws java.rmi.RemoteException;

    public void tx_Supports() throws java.rmi.RemoteException;

    public void tx_Never() throws java.rmi.RemoteException;

    public void tx_Mandatory() throws java.rmi.RemoteException;

    public void test_getBusinessObject(boolean businessInterface) throws java.rmi.RemoteException;

    public void verifyEJBFieldInjection() throws java.rmi.RemoteException;

    public void verifyEJBMethodInjection() throws java.rmi.RemoteException;

    public void verifyNoEJBFieldInjection() throws java.rmi.RemoteException;

    public void verifyNoEJBMethodInjection() throws java.rmi.RemoteException;
}
