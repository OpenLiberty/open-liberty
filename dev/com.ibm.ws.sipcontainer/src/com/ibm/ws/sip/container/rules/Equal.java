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
 *
 *	Compares the value of a variable with a literal value and evaluates to true 
 *  if the variable is defined and its value equals that of the literal. 
 * 	Otherwise, the result is false.
 */
public class Equal extends Operator 
{
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(Equal.class);
      		
	/**
	 * Value searched for within the argument.  
	 */
	private String m_value;


	/**
	 * Indicate whether the comparison is case sensative. 
	 */
	private boolean m_ignoreCase;

	/**
	 * Constructs a Equal condition for the given variable and value
	 * @param var Indicate the variable type description.
	 * @param value Value to compare with. 
	 */
	public Equal(String var, String value, boolean ignoreCase)
	{
		super(var);

		if(c_logger.isTraceEntryExitEnabled())
        {			
        	Object[] params = { var, value, new Boolean(ignoreCase) }; 
        	c_logger.traceEntry(this, "Equal", params); 
        }
        
		m_value = value; 
		m_ignoreCase = ignoreCase; 
	}

	/**
	 * @see com.ibm.ws.sip.container.rules.Operator#evaluate(java.lang.String)
	 */
	protected boolean evaluate(String value) {
       	return m_ignoreCase
	        ? value.equalsIgnoreCase(m_value)
	    	: value.equals(m_value);
	}

	/**
	 * @see com.ibm.ws.sip.container.rules.Operator#evaluate(com.ibm.ws.sip.container.rules.PhoneComparison)
	 */
	protected boolean evaluate(PhoneComparison value) {
        return value.equals(m_value, m_ignoreCase);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
    	StringBuffer buffer = new StringBuffer(16);
    	buffer.append(getVariable());
    	buffer.append(" EQUAL '");
    	buffer.append(m_value);
    	buffer.append("'");
    	
    	return buffer.toString();
	}
}
