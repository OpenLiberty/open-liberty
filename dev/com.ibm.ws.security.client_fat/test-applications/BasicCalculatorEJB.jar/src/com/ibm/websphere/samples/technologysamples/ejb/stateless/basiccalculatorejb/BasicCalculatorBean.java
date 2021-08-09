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

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

/**
 * BasicCalculatorBean is the implementation class for the <code>BasicCalculator</code> 
 * stateless session EJB. BasicCalculatorBean implements each of the business methods defined in the <code>BasicCalculator</code> EJB remote interface and each
 * of the EJB lifecycle methods in the javax.ejb.SessionBean interface.
 * 
 * @see com.ibm.websphere.samples.technologysamples.ejb.stateless.basiccalculatorejb.BasicCalculator
 * @see com.ibm.websphere.samples.technologysamples.ejb.stateless.basiccalculatorejb.BasicCalculatorHome
 */
public class BasicCalculatorBean implements SessionBean {

	private static final long serialVersionUID = 200805121225L;
	private double sum;
	private double difference;
	private double product;
	private double quotient;
	
	/**
	 * ejbCreate Session EJB lifecycle callback method. This method is empty for Stateless Session EJB.
	 * This method corresponds to the create method in the home interface.
	 */
	public void ejbCreate ( ) { }

	/**
	 * ejbActivate Session EJB lifecycle callback method. This method is empty for Stateless Session EJB.
	 */
	public void ejbActivate ( ) { }

	/**
	 * ejbPassivate Session EJB lifecycle callback method. This method is empty for Stateless Session EJB.
	 * This method is called when the instance is passivated from its "active" state, and releases.
	 */
	public void ejbPassivate ( ) { }

	/**
	 * ejbRemove Session EJB lifecycle callback method. This method is empty for Stateless Session EJB.
	 * This method is called just before the end of life for this object.
	 */
	public void ejbRemove ( ) { }

	/**
	 * ejbCreate Session EJB lifecycle callback method to set the EJB sessionContext for this EJB.
	 *
	 * @param sCtx javax.ejb.SessionContext The context for this session EJB
	 */
	public void setSessionContext (SessionContext sCtx) { }

	/**
	 * Perform the addition operation on the operands and return the result
	 *
	 * @return sum The result of adding operand1 and operand2
	 * @param operand1 A number to perform this mathematical operation on
	 * @param operand2 Another number to perform this mathematical operation on
	 */
	public double makeSum (double operand1, double operand2) {
		sum = operand1 + operand2;
		return sum;
	}

	/**
	 * Perform the subtraction operation on the operands and return the result
	 *
	 * @return difference The result of subtracting operand1 and operand2
	 * @param operand1 A number to perform this mathematical operation on
	 * @param operand2 Another number to perform this mathematical operation on
	 */
	public double makeDifference (double operand1, double operand2) {
		difference = operand1 - operand2;
		return difference;
	}

	/**
	 * Perform the multiplication operation on the operands and return the result
	 *
	 * @return product The result of multiplying operand1 and operand2
	 * @param operand1 A number to perform this mathematical operation on
	 * @param operand2 Another number to perform this mathematical operation on
	 */
	public double makeProduct (double operand1, double operand2) {
		product = operand1 * operand2;
		return product;
	}

	/**
	 * Perform the division operation on the operands and return the result
	 *
	 * @return quotient The result of dividing operand1 and operand2
	 * @param operand1 A number to perform this mathematical operation on
	 * @param operand2 Another number to perform this mathematical operation on
	 */
	public double makeQuotient (double operand1, double operand2) {
		quotient = operand1 / operand2;
		return quotient;
	}
}