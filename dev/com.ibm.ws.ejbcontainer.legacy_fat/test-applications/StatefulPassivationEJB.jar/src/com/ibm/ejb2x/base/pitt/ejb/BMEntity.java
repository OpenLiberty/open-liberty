/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb2x.base.pitt.ejb;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;

/**
 * A simple bean managed entity bean to test container and
 * deployment tools. <p>
 * 
 * @author Chriss Stephens
 * @version $Id: BMEntity.java,v 1.6 1999/11/30 20:10:24 chriss Exp $
 */
public interface BMEntity extends EJBObject {
    /**
     * Increment the persistent counter associated with this entity bean
     * within the scope of a new transaction. <p>
     * 
     * @return an <code>int</code> containing the value of the persistent
     *         counter after the increment has completed.
     */
    public int txNewIncrement() throws RemoteException;

    public int getNonpersistent() throws RemoteException;
}
