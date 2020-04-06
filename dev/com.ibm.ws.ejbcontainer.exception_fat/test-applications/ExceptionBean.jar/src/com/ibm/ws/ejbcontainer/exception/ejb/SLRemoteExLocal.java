/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.exception.ejb;

import java.io.IOException;
import java.rmi.RemoteException;

import javax.ejb.EJBLocalObject;

/**
 * Local interface for Enterprise Bean: SLRemoteExBean
 */
public interface SLRemoteExLocal extends EJBLocalObject {

    void testMethodwithNoEx(String exceptionToThrow);

    void testMethodwithException(String exceptionToThrow) throws Exception;

    void testMethodwithExceptionAndRemote(String exceptionToThrow) throws Exception, RemoteException;

    void testMethodwithExceptionAndRemoteSub(String exceptionToThrow) throws Exception, SLRemoteException;

    void testMethodwithExceptionAndRemoteAndRemoteSub(String exceptionToThrow) throws Exception, RemoteException, SLRemoteException;

    void testMethodwithIOException(String exceptionToThrow) throws IOException;

    void testMethodwithIOExceptionAndRemote(String exceptionToThrow) throws IOException, RemoteException;

    void testMethodwithIOExceptionAndRemoteSub(String exceptionToThrow) throws IOException, SLRemoteException;

    void testMethodwithIOExceptionAndRemoteAndRemoteSub(String exceptionToThrow) throws IOException, RemoteException, SLRemoteException;

    void testMethodwithRemoteEx(String exceptionToThrow) throws RemoteException;

    void testMethodwithRemoteExAndSub(String exceptionToThrow) throws RemoteException, SLRemoteException;

    void testMethodwithRemoteExSub(String exceptionToThrow) throws SLRemoteException;
}
