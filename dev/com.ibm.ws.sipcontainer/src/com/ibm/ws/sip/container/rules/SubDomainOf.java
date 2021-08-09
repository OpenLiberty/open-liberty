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

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;

/**
 * @author Amir Perlman, Jun 25, 2003
 * Given a variable denoting a domain name (SIP/SIPS URI host) or telephone
 * subscriber (tel property of SIP or Tel URLs), and a literal value, this 
 * operator returns true if Mapping Requests to Servlets the variable denotes a 
 * subdomain of the domain given by the literal value. Domain names are matched
 *  according to the DNS definition of what constitutes a subdomain; for example,
 * the domain names "example.com" and "research.example.com" are both 
 * subdomains of "example.com". IP addresses may be given as arguments to 
 * this operator; however, they only match exactly. In the case of the tel 
 * variables, the subdomain-of operator evaluates to true if the telephone 
 * number denoted by the first argument has a prefix that matches the literal
 * value given in the second argument; for example, the telephone number 
 * "1 212 555 1212" would be considered a subdomain of "1212555".
 * 
 */
public class SubDomainOf extends Operator
{
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SubDomainOf.class);
      			
	/**
	 * Value searched for within the argument.  
	 */
	private String m_value;

	/**
	 * Indicate whether the comparison is case sensative. 
	 */
	private boolean m_ignoreCase;

	/**
	 * Constructs a SubDomainOf condition for the given variable and value
	 * @param var
	 * @param value
	 */
	public SubDomainOf(String var, String value, boolean ignoreCase) {
		super(var);

		if (c_logger.isTraceEntryExitEnabled()) {			
        	Object[] params = { var, value, new Boolean(ignoreCase) }; 
        	c_logger.traceEntry(this, "SubDomainOf", params); 
        }

		m_value = value; 
		m_ignoreCase = ignoreCase; 
	}

	/**
	 * @see com.ibm.ws.sip.container.rules.Operator#evaluate(java.lang.String)
	 */
	protected boolean evaluate(String value) {
		if (m_value == null) {
			if(c_logger.isTraceDebugEnabled())
            {
            	c_logger.traceDebug(this, "evaluate" ,
				"Subdomain evaluated to false compared value is null");
            }
			
        	return false; 
        }
		
        boolean rc;
        if (isSubDomainName(m_value)) {
			rc = isMatchingSubDomainName(value, m_value); 
		}
		else if (isIp(m_value)) {
			if (c_logger.isTraceDebugEnabled()) {
            	c_logger.traceDebug(this, "evaluate" ,
									"SubDomain Of, Checking IP match");
            }
			
			//Check if strings are matching. Note that Ignore case does not 
			//have any effect since it is all numbers and dots. 
			rc = m_value.equals(value); 
		}
		else {
			rc = false;
		}
		
		// A name can be confused as a phone number
		if (!rc && isSubDomainPhone(m_value)) {
			rc = checkTelephonyPrefixMatching(value, m_value); 
		}
		
		if (c_logger.isTraceDebugEnabled()) {
        	String tmp = "Is " + value + " SubDomain Of ";
        	tmp += m_value + ", Result: " + rc;  
        	c_logger.traceDebug(this, "evaluate", tmp);
        }
        return rc; 
	}

	/**
	 * @see com.ibm.ws.sip.container.rules.Operator#evaluate(com.ibm.ws.sip.container.rules.PhoneComparison)
	 */
	protected boolean evaluate(PhoneComparison value) {
        return value.equals(m_value, m_ignoreCase);
	}
	
	/**
	 * Helper function - Deteremines if the subDomain param is a sub domain 
	 * of the Domain param. 
	 * Domain names are matched according to the DNS definition of what 
	 * constitutes a subdomain; for example, the domain names "example.com" and 
	 * "research.example.com" are both subdomains of "example.com".
	 * @param subDomainParam
     * @param domainParam
     * @return boolean 
     */
    private boolean isMatchingSubDomainName(String subDomainParam, 
    										  String domainParam)
    {
		String subDomain = subDomainParam;
		String domain = domainParam;
		
		if(c_logger.isTraceEntryExitEnabled())
        {			
        	Object[] params = { subDomain, domain }; 
        	c_logger.traceEntry(this, "isMatchingSubDomainName", params); 
        }
		
        boolean rc = false; 
        if(m_ignoreCase)
        {
        	subDomain = subDomain.toLowerCase(); 
        	domain = domain.toLowerCase(); 
        }
		
		if(subDomain.endsWith(domain))
		{
			rc = true; 	
		}
		
		if(c_logger.isTraceEntryExitEnabled())
        {
        	c_logger.traceExit(this, "isMatchingSubDomainName", new Boolean(rc)); 
        }
        
		return rc; 
    }
	
	
	/**
	 * Helper function - Determines if the given name is a sub domain name. 
	 * The name is checked for alphabetic chars, '-' or '.' to determine that
	 * it is a domain name and not ip address or telephone nubmer.
	 * It also must contain at least one letter. 
	 * hostname     =  *( domainlabel "." ) toplabel [ "." ]
	 * domainlabel  =  alphanum / alphanum *( alphanum / "-" ) alphanum
	 * toplabel     =  ALPHA / ALPHA *( alphanum / "-" ) alphanum 
	 * @param value
	 * @return True if the string contains at least one alphabetic char. 
	 */
	private boolean isSubDomainName(String value)
	{
		boolean rc = true;
		boolean letter = false;
		for(int i=0; i<value.length(); i++)
		{
			char c = value.charAt(i);
			if (Character.isLetter(c)) // We need at least one letter
			{
				letter = true;
			}
			else if(!(Character.isDigit(c) || (c == '.') || (c == '-')))
			{
				rc = false; 
				break; 
			}
		}

		return rc && letter;
	}

	/**
	 * Helper function - Determines if the given value is phone number. 
	 * The name is checked for 0-9 and a-d to determine that it is a 
	 * valid telephone nubmer.  
	 * @param value
	 * @return True if the string contains only one of 0123456789abcd+-().*#pw. 
	 */
	private boolean isSubDomainPhone(String value)
	{
		final String validPhoneNumberChars = "0123456789abcd+-().*#pw"; 
		boolean rc = true;
		char c;  
		for(int i=0; i<value.length(); i++)
		{
			c = Character.toLowerCase(value.charAt(i));
			if(validPhoneNumberChars.indexOf(c) < 0)
			{
				rc = false; 
				break; 
			}
		}
    
		return rc;
	}


    /**
     * Helper function - Checks if the given prefix matches the given 
     * telephone number. In the case of the tel variables, the subdomain-of 
     * operator evaluates to true if the telephone number denoted by the 
     * first argument has a prefix that matches the literal value given in 
     * the second argument; for example, the telephone number "1 212 555 1212" 
     * would be considered a subdomain of "1212555".
     * @param telParam Full telephone number
     * @param prefixParam
     * @return boolean
     */
    private boolean checkTelephonyPrefixMatching(String telParam, 
    											   String prefixParam)
    {
		String tel = telParam;
		String prefix = prefixParam;
		
		if(c_logger.isTraceEntryExitEnabled())
        {			
        	Object[] params = { tel, prefix }; 
        	c_logger.traceEntry(this, "checkTelephonyPrefixMatching", params); 
        }
        
        boolean rc = false; 
        
        //Remove all type of separators (- ( ) SPC )
        tel = removeSeparatorsFromTel(tel);
		prefix = removeSeparatorsFromTel(prefix);
		
		rc = tel.startsWith(prefix); 
		
        return rc; 
    }

    
    /**
     * Remove all separators from teleophone number.  
     * @param tel
     * @return Stripped telephone number. 
     */
    private String removeSeparatorsFromTel(String tel)
    {
        StringBuffer buffer = new StringBuffer(16); 
        for(int i=0; i<tel.length(); i++)
        {
        	char c = Character.toLowerCase(tel.charAt(i));  
        	if((c >= '0' && c <= '9') || 
        	   (c >= 'a' && c <= 'd') || (c == '*') || (c == '#'))
        	{
        		buffer.append(c); 
        	}
        }
        
        return buffer.toString();
    }

    
    /**
	 * Helper function - Determines if the given name is an ip address.  
	 * @param value The value to check.  
	 * @return True if the values is a valid ip, 
	 */
	private boolean isIp(String value)
	{
		boolean rc = false; 
		try
        {
            //We determine the validity of the IP by using the InetAddress
            //If might be more expensive but simplifies the task and ensures
            //compatabiltiy to IPv6 as well. 
            InetAddress.getByName(value);
            
            //If we got here then we have a valid ip address. 
            rc = true; 
            
        }
        catch (UnknownHostException e)
        {
            if(c_logger.isTraceDebugEnabled())
            {
				c_logger.traceDebug(this, "isIp" ,
							"SubDomain Of, Not an IP: " + m_value);
            }
        } 
		
		return rc;
	}
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer buffer = new StringBuffer(16);
        buffer.append(getVariable());
        buffer.append(" SUBDOMAIN-OF '");
        buffer.append(m_value);
        buffer.append("'");
        
        return buffer.toString(); 
    }

}
