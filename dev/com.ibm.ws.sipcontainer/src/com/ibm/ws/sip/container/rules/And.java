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

import java.util.Iterator;
import java.util.List;

import javax.servlet.sip.SipServletRequest;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;

/**
 * @author Amir Perlman, Jun 25, 2003
 * Contains a number of conditions and evaluates to true if and only if all 
 * contained conditions evaluate to true. 
 *
 */
public class And extends LogicalConnector
{
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(And.class);
    
	/**
	 * List of conditions that are logicly connected. 
	 */
	private List m_conditions;
	
    /**
     * Constrct an AND condition that operate on the given list of conditions
     * @param conditions
     */
    public And(List conditions)
	{
		if(c_logger.isTraceDebugEnabled())
        {
        	c_logger.traceDebug(this, "And" ,
				"Construct New AND Condition, #args: " + conditions.size());
        }
		
		m_conditions = conditions; 
	}
    
    /**
     * @see com.ibm.ws.sip.container.rules.Condition#evaluate(javax.servlet.sip.SipServletRequest)
     */
    public boolean evaluate(SipServletRequest request)
    {
        if(m_conditions == null || m_conditions.size() == 0)
        {
			if(c_logger.isErrorEnabled())
			{
				Object[] args = { "AND" }; 
				c_logger.error("error.missing.sub.elements", 
							   Situation.SITUATION_CREATE, 
							   args);
			}
        	
			return false; 
        }
        
        boolean rc = true; 
        
        Iterator iter = m_conditions.iterator(); 
        while (iter.hasNext() && rc) 
        {
        	Condition condition = (Condition) iter.next();
        	rc = rc && condition.evaluate(request);  
        	
        }
        
        return rc;
    }

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		StringBuffer buffer = new StringBuffer(16);
		
		Iterator iter = m_conditions.iterator();
		buffer.append('('); 
		while (iter.hasNext()) 
		{
			buffer.append(iter.next());
			if(iter.hasNext())
			{
				buffer.append(" AND ");
			}
		}
		buffer.append(')');
		return buffer.toString();  
	}
}
