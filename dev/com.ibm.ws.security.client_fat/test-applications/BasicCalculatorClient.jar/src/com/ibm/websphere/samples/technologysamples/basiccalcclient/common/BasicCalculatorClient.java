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

import com.ibm.websphere.samples.technologysamples.ejb.stateless.basiccalculatorejb.*;

public abstract class BasicCalculatorClient {

    public BasicCalculator bc = null;
    public BasicCalculatorHome bcHome = null;

    public class CalcException_InvalidOperation extends Exception {
    }

    public class CalcException_DivideByZero extends Exception {
    }

    public class CalcException_NotInitialized extends Exception {
    }
    
	/**
	 * Main service method for BasicCalculatorClient.  Encapsulates all EJB access.
	 * 
	 * @param operation Operation to perform: add, subtract, multiply, divide
	 * @param operand1 First operand of the operation.
	 * @param operand2 Second operand of the operation.
	 * @return CalcResult object
	 * @exception javax.naming.NamingException
     * @exception java.rmi.RemoteException
     * @exception CalcException_DivideByZero
     * @exception CalcException_InvalidOperation
     * @exception CalcException_NotInitialized
     * @exception javax.ejb.CreateException 
	 */
	public BasicCalculatorClientResultBean calculate(String operation, double operand1, double operand2) 
	       throws javax.naming.NamingException, java.rmi.RemoteException, 
	              CalcException_DivideByZero, CalcException_InvalidOperation,
	              CalcException_NotInitialized, javax.ejb.CreateException {
	                
	    String message = "";
		double result = 0; 
		
		if (bc == null) {
		    throw new CalcException_NotInitialized();
		}
		
		if (operation.equals("add")) {
			result = bc.makeSum(operand1, operand2);
		} else if (operation.equals("subtract")) {
			result = bc.makeDifference(operand1, operand2);
		} else if (operation.equals("multiply")) {
			result = bc.makeProduct(operand1, operand2);
		} else if (operation.equals("divide")) {
			if (operand2 == 0) {
			    throw new CalcException_DivideByZero();
			} else {
				result = bc.makeQuotient(operand1, operand2);
			}
		}
		else {
		    throw new CalcException_InvalidOperation();
		}

        // Encapsulate the results in a Result Bean and send it to the caller.
		BasicCalculatorClientResultBean calcResult = new BasicCalculatorClientResultBean();
		calcResult.setResult(result);
		calcResult.setOperation(operation);
		calcResult.setOperand1(operand1);
		calcResult.setOperand2(operand2);

        return calcResult;
	}
}