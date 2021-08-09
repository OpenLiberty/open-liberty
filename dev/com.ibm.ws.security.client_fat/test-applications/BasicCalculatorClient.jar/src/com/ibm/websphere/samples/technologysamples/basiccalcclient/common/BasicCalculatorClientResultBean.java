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

package com.ibm.websphere.samples.technologysamples.basiccalcclient.common;


public class BasicCalculatorClientResultBean
{
	/**
	 * The value of the mathematical operation performed
	 */
	public String result = "";

	/**
	 * The mathematical operation to be performed
	 */
	public String operation = "";

	/**
	 * A number to perform this mathematical operation on
	 */
	public String operand1 = "";

	/**
	 * Another number to perform this mathematical operation on
	 */
	public String operand2 = "";

	/**
	 * Sets the value of result. Converts a double to a String so that it can be accessed and
	 * displayed by the JSP.
	 * 
	 * @param d the double to be converted to a String and set as the value of result
	 */
	public void setResult(double d){
		result = (new Double(d)).toString( );
	}

	/**
	 * Returns the value of result (from the mathematical operation performed) 
	 * 
	 * @return result the value of the mathematical operation 
	 */
	public String getResult( ){
		return result;
	}

	/**
	 * Sets the value of operation by converting the name of the operation to its mathematical symbol.
	 * 
	 * @param s the name of the operation
	 */
	public void setOperation(String s){
		if (s.equals("add")) {
			operation = "+";
		} else if (s.equals("subtract")) {
			operation = "-";
		} else if (s.equals("multiply")) {
			operation = "*";
		} else if (s.equals("divide")) {
			operation = "/";
		}
	}

	/**
	 * Returns the value of the operation as a symbol +, -, *, /
	 * 
	 * @return operation the symbol representing the operation add, subtract, multiply, or divide
	 */
	public String getOperation( ){
		return operation;
	}

	/**
	 * Sets the value of operand1. Converts a double to a String so that it can be accessed and
	 * displayed by the JSP.
	 * 
	 * @param d the double to be converted to a String and set as the value of operand1
	 */
	public void setOperand1(double d){
		operand1  = (new Double(d)).toString( );
	}

	/**
	 * Returns the value of the first operand
	 * 
	 * @return operand1 the value of the first operand
	 */
	public String getOperand1( ){
		return operand1;
	}

	/**
	 * Sets the value of operand2. Converts a double to a String so that it can be accessed and
	 * displayed by the JSP.
	 * 
	 * @param d the double to be converted to a String and set as the value of operand2
	 */
	public void setOperand2(double d){
		operand2 = (new Double(d)).toString( );
	}

	/**
	 * Returns the value of the second operand
	 * 
	 * @return operand1 the value of the second operand
	 */
	public String getOperand2( ){
		return operand2;
	}

}