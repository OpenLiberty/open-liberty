/*******************************************************************************
 * Copyright (c) 2001, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.persistence;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.ejb.EJBObject;

public interface RemoteEnumerator extends Remote
{

    public EJBObject[] nextNElements(int n)
                    throws RemoteException, EnumeratorException;

    public EJBObject[] allRemainingElements()
                    throws RemoteException, EnumeratorException;

}
