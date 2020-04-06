/*******************************************************************************
 * Copyright (c) 2003,2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jain.protocol.ip.sip.header;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.header.WarningHeader;

import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
* Warning header implementation.
* 
* @author Assaf Azaria, Mar 2003.
*/
public class WarningHeaderImpl extends HeaderImpl
    implements WarningHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = -6135987514484576661L;

	//
	// Members.
	//
	/**
	 * The code.
	 */
	private int m_code;
	
	/**
	 * The host address.
	 */	
	private String m_host;
	
	/**
	 * The port. 
	 */
	private int m_port = -1;
		
	/**
	 * The text. 
	 */
	private String m_text;
	
    /**
     * @throws SipParseException
     */
    public WarningHeaderImpl() {
        super();
    }
	
   /**
    * Gets code of WarningHeader
    * @return code of WarningHeader
    */
    public int getCode()
    {
        return m_code;
    }

    /**
    * Gets agent host of WarningHeader
    * @return agent host of WarningHeader
    */
    public String getHost()
    {
        return m_host;
    }

    /**
    * Gets agent port of WarningHeader
    * (Returns negative int if port does not exist)
    * @return agent port of WarningHeader
    */
    public int getPort()
    {
        return m_port;
    }

    /**
    * Returns boolean value indicating if WarningHeader has port
    * @return boolean value indicating if WarningHeader has port
    */
    public boolean hasPort()
    {
        return m_port != -1;
    }

    /**
    * Gets text of WarningHeader
    * @return text of WarningHeader
    */
    public String getText()
    {
    	return m_text;
    }

    /**
     * Sets the agent of a warning header.
     * @param host String to set
     * @throws SipParseException if host has a bad specification
     * @throws IllegalArgumentException if host is null
     */
    public void setAgent(String host)
        throws SipParseException, IllegalArgumentException
    {
        if (host == null)
        {
        	throw new IllegalArgumentException("Warning: null host"); 
        } 

        // TODO: Parse the agent string.
        m_host = host;
    }

    /**
     * Sets code of WarningHeader
     * @param code int to set
     * @throws SipParseException if code is not accepted by implementation
     */
    public void setCode(int code) throws SipParseException
    {
    	if (code < 100 || code > 999)
    	{
    		throw new SipParseException("Warning: Only 3 digit code", "" + code);
    	}
        
        m_code = code;
    }

    /**
     * Sets agent host of WarningHeader
     * @param host String to set
     * @throws IllegalArgumentException if host is null
     * @throws SipParseException if host is not accepted by implementation
     */
    public void setHost(String host)
        throws IllegalArgumentException
    {
        if (host == null)
        {    
        	throw new IllegalArgumentException("Warning: null host"); 
        } 
        
        m_host = host;
    }

    /**
     * Sets agent port of WarningHeader
     * @param port int to set
     * @throws SipParseException if agentPort is not accepted by implementation
     */
    public void setPort(int port) throws SipParseException
    {
		if(port < 0) 
		{
			throw new SipParseException("Warning: negative port");
		} 
    	
    	m_port = port;
    }

    /**
    * Removes port from WarningHeader (if it exists)
    */
    public void removePort()
    {
        m_port = -1;    
    }

    /**
     * Sets text of WarningHeader
     * @param text String to set
     * @throws IllegalArgumentException if text is null
     * @throws SipParseException if text is not accepted by implementation
     */
    public void setText(String text)
        throws IllegalArgumentException, SipParseException
    {
    	if (text == null)
		{            
			throw new IllegalArgumentException("Warning: null text"); 
		} 
		
		if(text.length() == 0) 
		{
			throw new SipParseException("Warning: Empty text");
		} 
        
        m_text = text;
    }

    /**
    * Gets agent of WarningHeader
    * @return agent of WarningHeader
    */
    public String getAgent()
    {
        // TODO: deal better (more accurately) with agent. 
        return m_host;
    }
    
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
        try {
			// code
			//String code = parser.nextToken(SP);
			//setCode(Integer.parseInt(code));
        	setCode(parser.number());
			
			parser.match(SP);
			
			// agent
			String agent = parser.nextToken(SP);
			setAgent(agent);
			
			parser.match(SP);
			
			// warning text
			setText(parser.quotedString()); 
	    }
	    catch(NumberFormatException e) {
	    	throw new SipParseException(e.getMessage(), "");
	    }
	}
	
	/**
	 * Get the encoded value of this header. 
	 * 
	 * @see HeaderImpl#getValue()
	 * @see HeaderImpl#toString()
	 */
    protected void encodeValue(CharsBuffer ret)
	{
		ret.append(m_code);
		ret.append(SP);
		ret.append(m_host);
		if (hasPort())
		{
			ret.append(m_port);
		}
		
		if (m_text != null)
		{
			ret.append(SP); 
			
			if (m_text.charAt(0) == DOUBLE_QUOTE)
			{
				ret.append(m_text);
			}
			else
			{
				ret.append(DOUBLE_QUOTE); 
				ret.append(m_text);
				ret.append(DOUBLE_QUOTE);
			}
		}
	}
	
	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!(other instanceof WarningHeaderImpl)) {
			return false;
		}
		WarningHeaderImpl o = (WarningHeaderImpl)other;
		
		if (m_code != o.m_code) {
			return false;
		}

		if (m_port != o.m_port) {
			return false;
		}

		if (m_host == null || m_host.length() == 0) {
			if (o.m_host != null && o.m_host.length() > 0) {
				return false;
			}
		}
		else {
			if (o.m_host == null || o.m_host.length() == 0) {
				return false;
			}
		}

		if (m_text == null || m_text.length() == 0) {
			if (o.m_text == null || o.m_text.length() == 0) {
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (o.m_text == null || o.m_text.length() == 0) {
				return false;
			}
			else {
				return m_text.equals(o.m_text);
			}
		}
	}
	
	/**
	 * @return the name of this header 
	 * @see jain.protocol.ip.sip.header.Header#getName()
	 */
	public String getName() {
		return name;
	}

	/**
	 * Creates and returns a copy of Header
	 * @returns a copy of Header
	 */
	public Object clone()
	{
		// This is required in case someone will inherit 
		// from this class.
		return super.clone(); 
	}
	
	/**
	 * determines whether or not this header can have nested values
	 */
	public boolean isNested() {
		return true;
	}
}
