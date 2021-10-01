/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.crossapp.shared;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;

public interface CrossAppEJBObject2x extends EJBObject {
    String echo(String s) throws RemoteException;

    CrossAppEJBHome2x lookupTestEJBHome(String s) throws RemoteException;

    CrossAppEJBObject2x getSessionContextEJBObject() throws RemoteException;

    CrossAppEJBHome2x getSessionContextEJBHome() throws RemoteException;
}
