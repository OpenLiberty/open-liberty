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

import javax.ejb.EJBObject;
import java.rmi.RemoteException;

/**
 * Remote interface for the BasicCalculator stateless session bean.
 */
public interface BasicCalculator extends EJBObject {

	/**
	 * Perform the addition operation on the operands and return the result
	 *
	 * @return The result of adding operand1 and operand2
	 * @param operand1 A number to perform this mathematical operation on
	 * @param operand2 Another number to perform this mathematical operation on
	 * @throws java.rmi.RemoteException If there is a remote communication failure
	 */
	public double makeSum (double operand1, double operand2) throws RemoteException;

	/**
	 * Perform the subtraction operation on the operands and return the result
	 *
	 * @return The result of subtracting operand1 and operand2
	 * @param operand1 A number to perform this mathematical operation on
	 * @param operand2 Another number to perform this mathematical operation on
	 * @throws java.rmi.RemoteException If there is a remote communication failure
	 */
	public double makeDifference (double operand1, double operand2)  throws RemoteException;

	/**
	 * Perform the multiplication operation on the operands and return the result
	 *
	 * @return The result of multiplying operand1 and operand2
	 * @param operand1 A number to perform this mathematical operation on
	 * @param operand2 Another number to perform this mathematical operation on
	 * @throws java.rmi.RemoteException If there is a remote communication failure
	 */
	public double makeProduct (double operand1, double operand2)  throws RemoteException;

	/**
	 * Perform the division operation on the operands and return the result
	 *
	 * @return The result of dividing operand1 and operand2
	 * @param operand1 A number to perform this mathematical operation on
	 * @param operand2 Another number to perform this mathematical operation on
	 * @throws java.rmi.RemoteException If there is a remote communication failure
	 */
	public double makeQuotient (double operand1, double operand2)  throws RemoteException;

}