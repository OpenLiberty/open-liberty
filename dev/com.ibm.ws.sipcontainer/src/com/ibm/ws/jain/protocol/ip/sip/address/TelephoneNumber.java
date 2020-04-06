/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jain.protocol.ip.sip.address;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ibm.ws.sip.parser.Separators;
import com.ibm.ws.sip.parser.util.CharsBuffer;
import com.ibm.ws.sip.parser.util.CharsBuffersPool;

/**
 * A telephone number "user".
 * 
 * @author Assaf Azaria
 */
public class TelephoneNumber
{
	//
	// Constants.
	//
	public final static String POSTDIAL 		= "postd";
	public final static String PHONE_CONTEXT_TAG = "phone-context";
	public final static String ISUB 			= "isub";
	public final static String PROVIDER_TAG   = "tsp";
	
	//
	// Members.
	//
	
	/** 
	 * is global.
     */
    boolean m_isGlobal;

    /** 
     * The number.
     */
    String m_phoneNumber;

    /**
	 * The list of parameters.
	 */
	Map m_params = new HashMap(16);
	
    /** 
     * Constructor. 
     */
    public TelephoneNumber()
    {
	}

    //
    // Operations.
    //
	/**
	 * Set a parameter.
	 */
	public void setParameter(String name, String value)
	{
		m_params.put(name, value);
	}

	/** 
     * delete the specified parameter.
     * @param name String to set
     */
    public void removeParameter(String name)
    {
        m_params.remove(name);
    }

	/** 
	 * Return true if this header has parameters.
	 */
	public boolean hasParameter(String pname)
	{
		return m_params.containsKey(pname);
	}

	/** 
     * Get the phone number.
     */
    public String getPhoneNumber()
    {
        return m_phoneNumber;
    }

    /** 
     * Get the PostDial.
     */
    public String getPostDial()
    {
        return (String)m_params.get(POSTDIAL);
    }

    /**
     * Get the isdn subaddress for this number.
     * @return String
     */
    public String getIsdnSubaddress()
    {
        return (String)m_params.get(ISUB);
    }

    /** 
     * Returns true if the PostDial parameter exists
     */
    public boolean hasPostDial()
    {
        return hasParameter(POSTDIAL);
    }

    /**
     * Returns true if the isdn subaddress exists.
     */
    public boolean hasIsdnSubaddress()
    {
        return hasParameter(ISUB);
    }

    /**
     * is a global telephone number.
     */
    public boolean isGlobal()
    {
        return m_isGlobal;
    }

    /** remove the PostDial field
     */
    public void removePostDial()
    {
        removeParameter(POSTDIAL);
    }

    /**
     * Remove the isdn subaddress (if it exists).
     */
    public void removeIsdnSubaddress()
    {
        removeParameter(ISUB);
    }

    /** 
     * Set the Global field
     */
    public void setGlobal(boolean g)
    {
        m_isGlobal = g;
    }

    /** 
     * Set the PostDial field
     */
    public void setPostDial(String p)
    {
        setParameter(POSTDIAL, p);
    }

    /**
     * Set the isdn subaddress for this structure.
     */
    public void setIsdnSubaddress(String isub)
    {
        setParameter(ISUB, isub);
    }

    /** 
     * set the PhoneNumber field
     */
    public void setPhoneNumber(String num)
    {
        m_phoneNumber = num;
    }

	/**
	 * Get a string representation of this number.
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
        if (m_isGlobal)
        {
        	ret.append("+"); 
        } 
        
        ret.append(m_phoneNumber);
        
        if (!m_params.isEmpty())
        {
            ret.append(Separators.SEMICOLON);
            encodeParams(ret, m_params);
        }
    }
	
	/**
	 * Encode the paramters in canonical form.
	 */
	private void encodeParams(CharsBuffer buffer, Map map)
	{
		for (Iterator i = map.entrySet().iterator(); i.hasNext();) 
		{
			Map.Entry entry = (Map.Entry)i.next();
			buffer.append(entry.getKey());
			buffer.append(Separators.EQUALS);
			buffer.append(entry.getValue());
	
			if (i.hasNext()) 
			{ 
				buffer.append(Separators.SEMICOLON);
			} 
		}
	}

}
