/*******************************************************************************
 * Copyright (c) 2001, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.samples.technologysamples.ejb.stateless.basiccalculatorejb;

import javax.ejb.EJBHome;
import javax.ejb.CreateException;
import java.rmi.RemoteException;

/**
 * Home interface for BasicCalculator stateless session bean.
 */
public interface BasicCalculatorHome extends EJBHome {

	/**
	 * Creates an instance of the BasicCalculator EJB object
	 *
	 * @return BasicCalculator The BasicCalculator EJB object
	 * @throws java.rmi.RemoteException If there is a remote communication failure
	 * @throws javax.ejb.CreateException BasicCalculator EJB object was not created
	 */
	public BasicCalculator create ( ) throws CreateException, RemoteException;

}