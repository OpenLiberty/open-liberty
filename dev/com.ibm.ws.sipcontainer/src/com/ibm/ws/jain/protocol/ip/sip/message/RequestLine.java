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
package com.ibm.ws.jain.protocol.ip.sip.message;

import jain.protocol.ip.sip.address.URI;

import java.io.Serializable;
import java.util.Objects;

import com.ibm.ws.jain.protocol.ip.sip.address.URIImpl;
import com.ibm.ws.sip.parser.Separators;
import com.ibm.ws.sip.parser.util.CharsBuffer;
import com.ibm.ws.sip.parser.util.CharsBuffersPool;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;

/**
* The sip request line.
* 
* Serializable added for: add SipServletMessage as an attribute to the 
* SipSession or SipApplicationSession - and should be replicated
*
* @author Assaf Azaria, April 2003.
*/
public class RequestLine implements Serializable
{
    //
    // Members.
    //

    /** 
     * The uri.
     */
    protected URI m_uri;

    /**
     * The method.
     */
    String m_method;

    /** 
     * The version.
     */
    protected SipVersion m_version;

    //
    // Constructors.
    //

    /** 
     * constructor
     */
    public RequestLine()
    {
    	m_version = SipVersionFactory.createVersion();
    }
    
	/** 
	 * Constructor given the request URI and the method.
	 */
	public RequestLine(URI requestURI, String method)
	{
		m_uri = requestURI;
		m_method = method;
		m_version = SipVersionFactory.createVersion();
	}

    //
    // Operations.
    //

    /** 
     * Get the Request-URI.
     *
     */
    public URI getURI()
    {
        return m_uri;
    }

    

    /**
     * Get the Method string.
     */
    public String getMethod()
    {
        return m_method;
    }

    /**
     * Get the SIP version.
     */
    public SipVersion getSipVersion()
    {
        return m_version;
    }

    /**
     * Set the uri member
     */
    public void setURI(URI uri)
    {
        m_uri = uri;
    }

    /**
     * Set the method member
     */
    public void setMethod(String method)
    {
        m_method = method;
    }

    /**
     * Set the sipVersion member
     */
    public void setSipVersion(SipVersion version)
    {
        m_version = version;
    }

    /** 
	 * Encode the request line as a String.
	 *
	 * @return requestLine encoded as a string.
	 */
	public String toString()
	{
	    CharsBuffer buffer = CharsBuffersPool.getBuffer();
        writeToCharBuffer(buffer, false);
        
        String value = buffer.toString();
        CharsBuffersPool.putBufferBack(buffer);
        
        return value;
	}
	
	/**
	 * Dump the request line to the specified buffer
     * @param buffer
     * @param network true if the message is going to the network, false if
     *  it's just going to the log
     */
    public void writeToCharBuffer(CharsBuffer ret, boolean network) 
    {
        if (m_method != null)
		{
			ret.append(m_method);
			ret.append(Separators.SP);
		}
		if (m_uri != null)
		{
			boolean hide;
			if (network) {
				hide = false;
			}
			else {
				hide = SIPTransactionStack.instance().getConfiguration().hideRequestUri();
			}
			if (hide) {
				ret.append("<hidden request URI>");
			}
			else {
				((URIImpl)m_uri).writeToCharBuffer(ret);
			}
			ret.append(Separators.SP);
		}
		ret.append(m_version.toString());
		ret.append(Separators.NEWLINE);
	}
	
	/**
	 * Equals implementation.
	 */
	public boolean equals(Object obj)
	{
		if (!(obj instanceof RequestLine))
		{
			return false;
		}

		RequestLine other = (RequestLine)obj;

		boolean ret = true;
		if (m_method != null)
		{
			ret = m_method.equals(other.m_method);
		}
		
		if (m_uri != null)
		{
		    ret = ret && m_uri.equals(other.m_uri);
	    }
		
		return ret && m_version.equals(other.m_version);
	}
	
	/**
	 * hashCode implementation
	 */
	public int hashCode(){
		return Objects.hash(m_method, m_uri, m_version);
	}
}
