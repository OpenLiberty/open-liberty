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

import javax.servlet.sip.SipServletRequest;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;

/**
 * @author Amir Perlman, Jun 25, 2003
 *
 * Negates the value of the contained condition.
 */
public class Not extends LogicalConnector
{
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(Not.class);
    
	/**
	 * Condition to operate on. 
	 */
	private Condition m_condition; 
   

    /**
     * @param condition
     */
    public Not(Condition condition)
    {
		if(c_logger.isTraceEntryExitEnabled())
        {			
        	c_logger.traceEntry(this, "Not"); 
        }
        
		m_condition = condition;  
    }

    /**
     * @see com.ibm.ws.sip.container.rules.Condition#evaluate(javax.servlet.sip.SipServletRequest)
     */
    public boolean evaluate(SipServletRequest request)
    {
        return !m_condition.evaluate(request); 
    }

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
    
		return "NOT (" + m_condition + ")";  
	}
}
