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
package com.ibm.ws.ejbcontainer.remote.fat.basic;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.Future;

public interface BusinessRMI extends Remote {
    void test() throws RemoteException;

    void testAppException() throws RemoteException, TestAppException;

    void testSystemException() throws RemoteException;

    void testTransactionException() throws RemoteException;

    List<?> testWriteValue(List<?> list) throws RemoteException;

    void setupAsyncVoid() throws RemoteException;

    void testAsyncVoid() throws RemoteException;

    long awaitAsyncVoidThreadId() throws RemoteException;

    void setupAsyncFuture(int asyncCount) throws RemoteException;

    Future<Long> testAsyncFuture() throws RemoteException;

    void awaitAsyncFuture() throws RemoteException;
}
