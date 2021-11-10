/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.exception2x.ejb;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;

/**
 * This is an Enterprise Java Bean Remote Interface
 */
public interface TestEx extends EJBObject {
    public int addMore(int i) throws RemoteException;

    public void throwRuntimeException(String s) throws RemoteException;

    public void throwTransactionRequiredException(String s) throws RemoteException;

    public void throwTransactionRolledbackException(String s) throws RemoteException;

    public void throwInvalidTransactionException(String s) throws RemoteException;

    public void throwAccessException(String s) throws RemoteException;

    public void throwActivityRequiredException(String s) throws RemoteException;

    public void throwInvalidActivityException(String s) throws RemoteException;

    public void throwActivityCompletedException(String s) throws RemoteException;

    public void throwNoSuchObjectException(String s) throws RemoteException;
}