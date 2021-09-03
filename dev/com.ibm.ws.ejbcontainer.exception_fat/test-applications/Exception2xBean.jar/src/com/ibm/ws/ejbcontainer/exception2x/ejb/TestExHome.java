/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.exception2x.ejb;

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;

/**
 * This is a Home interface for the Entity Bean
 */
public interface TestExHome extends EJBHome {
    public TestEx create(int value) throws CreateException, RemoteException;

    /**
     * This create method will throw a NullPointerException during ejbCreate
     */
    public TestEx create(int value, int dummy) throws CreateException, RemoteException;
}