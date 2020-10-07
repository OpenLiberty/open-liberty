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
 * Evaluates to true if the value of the variable specified as the first 
 * argument contains the literal string specified as the second argument.
 */
public class Contains extends Operator
{
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(Contains.class);
      
    /**
     * Value searched for within the argument.  
     */
	private String m_value;
	
    
	/**
	 * Indicate whether the comparison is case sensative. 
	 */
	private boolean m_ignoreCase;

    /**
	 * Constructs a Contains condition for the given variable and value
	 * @param var Indicate the variable's type description.
	 * @param value Value to serach with the variable. 
	 */
	public Contains(String var, String value, boolean ignoreCase)
	{
		super(var);

		if(c_logger.isTraceEntryExitEnabled())
        {			
        	Object[] params = { var, value, new Boolean(ignoreCase) }; 
        	c_logger.traceEntry(this, "Contains", params); 
        }
		
		m_value = value; 
		m_ignoreCase = ignoreCase; 
	}
	
	/**
	 * @see com.ibm.ws.sip.container.rules.Operator#evaluate(java.lang.String)
	 */
	protected boolean evaluate(String value) {
       	String string1 = value;
       	String string2 = m_value;

	    if (m_ignoreCase) {
	    	string1 = string1.toLowerCase();
	    	string2 = string2.toLowerCase();
		}
		return string1.indexOf(string2) > -1;  
	}

	/**
	 * @see com.ibm.ws.sip.container.rules.Operator#evaluate(com.ibm.ws.sip.container.rules.PhoneComparison)
	 */
	protected boolean evaluate(PhoneComparison value) {
        return value.contains(m_value, m_ignoreCase);
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
    	StringBuffer buffer = new StringBuffer(16);
    	buffer.append(getVariable());
		buffer.append(" CONTAINS '");
		buffer.append(m_value);
		buffer.append("'"); 
		
		return buffer.toString();
	}
}
