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
package com.ibm.ws.jain.protocol.ip.sip.address;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.address.URI;

import com.ibm.ws.sip.parser.Coder;
import com.ibm.ws.sip.parser.Separators;
import com.ibm.ws.sip.parser.util.CharsBuffer;
import com.ibm.ws.sip.parser.util.CharsBuffersPool;

/**
* URI Implementation.
* 
* @author Assaf Azaria, April, 2003.
* 
* @see SipURL
*/
public class URIImpl implements URI
{
    /** Serialization UID (do not change) */
    private static final long serialVersionUID = -5197070550574020625L;

    //
    // Members.
    //
    
    /**
     * The uri scheme.
     */
    protected String m_scheme;
    
    /**
     * The scheme date. 
     */
    private String m_schemeData;

	//
	// Constructors.
	//
	
    /**
     */
    URIImpl()
    {}
    
	/**
	 * Construct a new URI impl with the give scheme. 
	 * 
	 * @param scheme The scheme.
	 */
	public URIImpl(String scheme)
	{
		m_scheme = scheme;
	}
    
	/**
	 * Construct a new URI impl with the give scheme and 
	 * scheme data.
	 * 
	 * @param scheme The scheme.
	 * @param schemeData The scheme data. 
	 */
	URIImpl(String scheme, String schemeData)
	{
		m_scheme = scheme;
		m_schemeData = schemeData;
	}

	//
	// Methods.
	//
	
	/**
	 * Gets scheme of URI
	 * @return scheme of URI
	 */
    public String getScheme()
    {
        return m_scheme;
    }
	
	/**
	 * Gets scheme data of URI
	 * @return scheme data of URI
	 */
    public String getSchemeData()
    {
        return m_schemeData;
    }

	/**
	 * Sets scheme of URI
	 * @param <var>scheme</var> scheme
	 * @throws IllegalArgumentException if scheme is null
	 * @throws SipParseException if scheme is not accepted by implementation
	 */
    public void setScheme(String scheme)
        throws IllegalArgumentException, SipParseException
    {
        if (scheme == null)
        {
            throw new IllegalArgumentException("URI: Null scheme");
        }
        if (scheme.length() == 0)
        {
            throw new SipParseException("URI: Empty scheme", "");
        }
        
        // convert well-known schemes to lowercase
        if (scheme.equalsIgnoreCase("sip")) {
        	m_scheme = "sip";
        }
        else if (scheme.equalsIgnoreCase("sips")) {
        	m_scheme = "sips";
        }
        else if (scheme.equalsIgnoreCase("tel")) {
        	m_scheme = "tel";
        }
        else {
        	m_scheme = scheme;
        }
    }

	/**
	 * Sets scheme data of URI
	 * @param <var>schemeData</var> scheme data
	 * @throws IllegalArgumentException if schemeData is null
	 * @throws SipParseException if schemeData is not accepted by implementation
	 */
    public void setSchemeData(String schemeData)
        throws IllegalArgumentException, SipParseException
    {
        if (schemeData == null)
        {
            throw new IllegalArgumentException("URI: null schemeData");
        }
        if (schemeData.length() == 0)
        {
            throw new SipParseException("URI: null scheme data");
        }
        
        m_schemeData = schemeData;
    }

	/**
	 * Creates and returns a copy of URI
	 * @returns a copy of URI
	 */
    public Object clone()
    {
    	try
    	{
			return super.clone();
		}
    	catch (CloneNotSupportedException e)
    	{
    		// Can't happen.
    		e.printStackTrace();
    		// @PMD:REVIEWED:AvoidThrowingCertainExceptionTypesRule: by Amirk on 9/19/04 4:46 PM
    		throw new Error("Clone error");
    	// @PMD:REVIEWED:DoNotUsePrintStackTrace: by Amirk on 9/19/04 4:45 PM
    	}
    }
    
	/**
	 * Indicates whether some other Object is "equal to" this URI
	 * (Note that obj must have the same Class as this URI - this means that it
	 * must be from the same JAIN SIP implementation)
	 * @param <var>obj</var> the Object with which to compare this URI
	 * @returns true if this URI is "equal to" the obj
	 * argument; false otherwise (equality of URI's is defined in RFC 2068) 
	 */
    public boolean equals(Object obj)
    {
    	if (this == obj)
    	{return true;}
    	
        if (obj == null || 
        	!(obj instanceof URIImpl))
        {return false;}
        
        URIImpl other = (URIImpl)obj;
        
        if (!m_scheme.equals(other.getScheme()))
        {return false;}

        if (m_schemeData == null)
        {
        	if (other.getSchemeData()!=null)
        	{return false;}
        }
        else
        {
        	if (!m_schemeData.equals(other.getSchemeData()))
        	{return false;}
        }

        return true;
}
    
    /**
     * Get the hash code for this object.
     */
    public int hashCode()
	{
		return toString().hashCode();
	}
    
    //
    // Encoding.
    //
    
	/**
	 * Gets string representation of URI
	 * @return string representation of URI
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
     * Dump this object to the specified char array
     * @param ret
     */
    public void writeToCharBuffer(CharsBuffer ret)
    {
        ret.append(m_scheme);
		
		if (m_schemeData != null)
		{
			ret.append(Separators.COLON);
			if (isTelURI()) {
				Coder.encodeTelURI(m_schemeData, ret);
			}
			else {
			Coder.encode(m_schemeData, ret);
			}
		}
	}
    
    /**
     * @return true is the URI is a tel-uri
     */
    private boolean isTelURI() {
    	if (m_scheme.equalsIgnoreCase("TEL") || m_scheme.equalsIgnoreCase("FAX") || m_scheme.equalsIgnoreCase("MODEM")) {
    		return true;
    	}
    	return false;
    }
}
