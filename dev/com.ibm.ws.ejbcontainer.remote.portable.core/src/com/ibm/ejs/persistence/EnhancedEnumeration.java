/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.persistence;

import java.rmi.RemoteException;
import java.util.Enumeration;

public interface EnhancedEnumeration extends Enumeration
{

    public boolean hasMoreElementsR()
                    throws RemoteException, EnumeratorException;

    public Object nextElementR()
                    throws RemoteException, EnumeratorException;

    public Object[] nextNElements(int n)
                    throws RemoteException, EnumeratorException;

    public Object[] allRemainingElements()
                    throws RemoteException, EnumeratorException;

}
