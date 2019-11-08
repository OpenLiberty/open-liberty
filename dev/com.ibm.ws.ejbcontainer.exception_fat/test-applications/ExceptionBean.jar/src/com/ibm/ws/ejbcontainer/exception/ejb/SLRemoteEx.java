/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

import javax.ejb.EJBObject;

/**
 * Remote interface for Enterprise Bean: SLRemoteEx
 */
public interface SLRemoteEx extends EJBObject {

    // EJBDeploy/RMIC will fail if RemoteException (or parent) is not thrown
    // void testMethodwithNoEx(String exceptionToThrow);

    void testMethodwithException(String exceptionToThrow) throws Exception;

    void testMethodwithIOException(String exceptionToThrow) throws IOException;

    void testMethodwithRemoteEx(String exceptionToThrow) throws RemoteException;

    void testMethodwithExceptionAndRemote(String exceptionToThrow) throws Exception, RemoteException;

    void testMethodwithRemoteExAndSub(String exceptionToThrow) throws RemoteException;
}
