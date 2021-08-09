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
 * Simple test EJB that expects to be deployed as a stateless session bean using
 * bean managed transactions. <p>
 * 
 * This bean is used for testing the container and deployment tools. <p>
 */
public interface BMTXStateless extends EJBObject {
    /**
     * Method for reproducing CMVC defect 70616
     */
    public void regressionMethod70616() throws RemoteException;

    /**
     * Method for testing lookup and use of UserTransaction from java:comp
     * namespace
     */
    public void testAccessToUserTransaction() throws RemoteException;
}