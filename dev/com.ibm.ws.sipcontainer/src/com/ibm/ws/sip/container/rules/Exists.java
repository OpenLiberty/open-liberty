/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.rules;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;

/**
 * @author Amir Perlman, Jun 25, 2003
 * Takes a variable name and evaluates to true if the variable is defined, 
 * and false otherwise.
 */
public class Exists extends Operator
{
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(Exists.class);
       				
	/**
	 * Constructs an Existscondition for the given variable
	 * @param var
	 */
	public Exists(String var)
	{
		super(var);

		if(c_logger.isTraceEntryExitEnabled())
        {			
        	Object[] params = { var }; 
        	c_logger.traceEntry(this, "Exists", params); 
        }
	}

	/**
	 * @see com.ibm.ws.sip.container.rules.Operator#evaluate(java.lang.String)
	 */
	protected boolean evaluate(String value) {
		return value != null;
	}

	/**
	 * @see com.ibm.ws.sip.container.rules.Operator#evaluate(com.ibm.ws.sip.container.rules.PhoneComparison)
	 */
	protected boolean evaluate(PhoneComparison value) {
		return value != null;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return getVariable() + " EXISTS"; 
	}
}
