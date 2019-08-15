/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb2x.base.pitt.ejb;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;

/**
 * Simple test EJB that expects to be deployed as a stateful session bean using
 * bean managed transactions. <p>
 * 
 * This bean is used for testing the container and deployment tools. <p>
 */
public interface BMTXSession extends EJBObject {
    /**
     * Return if the bean instance has been passivated or not.
     */
    public boolean isPassivated() throws RemoteException;

    /**
     * Initialize the instance fields before the passivation test.
     */
    public void beforePassivation(int i) throws RemoteException;

    /**
     * Clean up the environment after the passivation test.
     */
    public void afterPassivation(int i) throws RemoteException;

    /**
     * Test if passivation and activation correctly handle the instance fields.
     */
    public void testPassivationActivation() throws RemoteException;

    public void runNotSupportedTest() throws RemoteException;
}