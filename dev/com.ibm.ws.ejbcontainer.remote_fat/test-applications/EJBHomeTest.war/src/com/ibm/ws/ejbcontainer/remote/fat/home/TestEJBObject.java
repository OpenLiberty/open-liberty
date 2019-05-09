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
package com.ibm.ws.ejbcontainer.remote.fat.home;

import java.rmi.RemoteException;
import java.util.List;

import javax.ejb.EJBObject;

public interface TestEJBObject extends EJBObject {
    String echo(String s) throws RemoteException;

    TestEJBHome lookupTestEJBHome(String s) throws RemoteException;

    TestEJBObject getSessionContextEJBObject() throws RemoteException;

    TestEJBHome getSessionContextEJBHome() throws RemoteException;

    List<?> testWriteValue(List<?> list) throws RemoteException;
}
