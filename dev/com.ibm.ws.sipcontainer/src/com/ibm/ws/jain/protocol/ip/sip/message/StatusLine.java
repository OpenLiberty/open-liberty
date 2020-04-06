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

import java.io.Serializable;
import java.util.Objects;

import com.ibm.ws.sip.parser.Separators;
import com.ibm.ws.sip.parser.util.CharsBuffer;
import com.ibm.ws.sip.parser.util.CharsBuffersPool;

/**
 * The sip status line (used in responses).
 * 
 * Serializable added for: SipServletResponse can be added as an attribute to
 * the session or appSession and will be replicated after the dialog will be 
 * estableshed
 * 
 * @author Assaf Azaria, April 2003.
 */
public class StatusLine implements Serializable
{
	//
	// Members.
	//

	/** 
     * The version.
     */
   	SipVersion m_version;

    /** 
     * The status code.
     */
    int m_code;

    /** 
     * The reason phrase.
     */
    String m_reasonPhrase;

	//
	// Constructors.
	//
	
    /** 
     * Construct a new status line object.
     */
    public StatusLine()
    {
        m_version =  SipVersionFactory.createVersion();
    }

    
    //
    // Accessors.
    //

    /** 
     * Get the Sip Version
     * @return The sip version
     */
    public SipVersion getSipVersion()
    {
        return m_version;
    }

    /** 
     * Get the Status Code
     */
    public int getStatusCode()
    {
        return m_code;
    }

    /** 
     * Get the ReasonPhrase
     */
    public String getReasonPhrase()
    {
    	if (m_reasonPhrase == null)
    	{
    		return SipResponseCodes.getResponseCodeText(m_code);
    	}
        return m_reasonPhrase;
    }

    /**
     * Set the sip version. 
     * @param version The sip version.
     */
    public void setSipVersion(SipVersion version)
    {
        m_version = version;
    }

    /**
     * Set the status code
     * @param code The status code.
     */
    public void setStatusCode(int code)
    {
        m_code = code;
    }

    /**
     * Set the reason phrase.
     * @param reasonPhrase The reason phrase.
     */
    public void setReasonPhrase(String reasonPhrase)
    {
        m_reasonPhrase = reasonPhrase;
    }

    /**
     * sets the reason phrase given the reason code.
     * heap is conserved if the reason phrase is the exact standard phrase
     * for the given code.
     * otherwise, a new String is allocated for the proprietary phrase.
     */
    public void setReasonPhrase(int code, char[] array, int start, int length) {
    	String standard = SipResponseCodes.getResponseCodeText(code);
    	
    	if (length != standard.length()) {
			String proprietary = String.valueOf(array, start, length);
	        m_reasonPhrase = proprietary;
			return;
    	}
    	
    	for (int i = 0; i < length; i++) {
    		if (array[start+i] != standard.charAt(i)) {
    			String proprietary = String.valueOf(array, start, length);
    	        m_reasonPhrase = proprietary;
    			return;
    		}
    	}

        m_reasonPhrase = standard;
    }

    /**
	 * Encode into a canonical form.
	 */
	public String toString()
	{
	    CharsBuffer buffer = CharsBuffersPool.getBuffer();
        writeToCharBuffer(buffer);
        
        String value = buffer.toString();
        CharsBuffersPool.putBufferBack(buffer);
        
        return value;
	}
	
	
	/**
	 * Dump the request line to the specified buffer
     * @param buffer
     */
    public void writeToCharBuffer(CharsBuffer ret) 
    {
		ret.append(m_version.toString());
		ret.append(Separators.SP);
		ret.append(m_code);
		
		ret.append(Separators.SP);
		ret.append(getReasonPhrase()); 
	
		ret.append(Separators.NEWLINE);
	}
	
	/**
	 * Equals implementation.
	 */
	public boolean equals(Object obj)
	{
		if (!(obj instanceof StatusLine))
		{
			return false;
		}
	
		StatusLine other = (StatusLine)obj;
	
		boolean ret = true;
		if (m_reasonPhrase != null)
		{
			ret = m_reasonPhrase.equals(other.m_reasonPhrase);
		}
		
		return ret && m_version.equals(other.m_version) &&
				m_code == other.m_code;
	}
	
	/**
	 *  hashCode implementation
	 */
	public int hashCode() {
		return Objects.hash(m_reasonPhrase, m_version, m_code);
	}
}
