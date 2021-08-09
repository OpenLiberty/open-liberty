/*******************************************************************************
 * Copyright (c) 2002, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb1x.base.spec.sfr.ejb;

import java.rmi.RemoteException;

/**
 * Remote interface for Enterprise Bean: SFRTestReentrance
 */
public interface SFRTestReentrance extends javax.ejb.EJBObject {
    /**
     * Call self recursively n times
     * 
     * @return number of recursive call
     */
    public int callRecursiveSelf(int level, SFRTestReentrance ejb1) throws RemoteException, SFRApplException;

    /**
     * Call self recursively to cause an exception
     */
    public int callNonRecursiveSelf(int level, SFRTestReentrance ejb1) throws RemoteException, SFRApplException;
}
