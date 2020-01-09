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

import jain.protocol.ip.sip.address.SipURL;

import com.ibm.ws.sip.parser.Separators;
import com.ibm.ws.sip.parser.util.CharsBuffer;
import com.ibm.ws.sip.parser.util.CharsBuffersPool;

/**
 * Represents a sip user (in a uri).
 * 
 * @author Assaf Azaria, April 2003.
 */
public class SipUserInfo
{
	//
	// Members.
	//
	
	/** 
     * The user name.
     */
    String m_userName;

    /** 
     * The password.
     */
    String m_password;

    /** 
     * The user type. 
     * @see SipURL#USER_TYPE_IP
     * @see SipURL#USER_TYPE_PHONE
     */
    String m_type;

    /** 
     * Default constructor
     */
    public SipUserInfo()
    {
    	m_type = SipURL.USER_TYPE_IP;
    }

	//
	// Api.
	//
	
    /**
     * Gets the user type (which can be set to PHONE or REGULAR)
     */
    public String getType()
    {
        return m_type;
    }

    /** 
     * Get the user name.
     */
    public String getName()
    {
        return m_userName;
    }

    /** 
     * Get the password.
     */
    public String getPassword()
    {
        return m_password;
    }

    /**
     * Set the user name
     * @param name he name of the user.
     */
    public void setName(String name)
    {
    	if (name == null) return;
    	
    	m_userName = name;
        
        // Set the user type.
        if (name.indexOf(Separators.POUND) >= 0 || 
        	name.indexOf(Separators.SEMICOLON) >= 0)
        {
            setUserType(SipURL.USER_TYPE_PHONE);
        }
    }

    /**
     * Set the password. 
     * @param password The password.
     */
    public void setPassword(String password)
    {
        m_password = password;
    }

    /**
	 * Sets user type.
	 * @param <var>userType</var> user type
	 * @throws IllegalArgumentException if userType is null
	 */
	public void setUserType(String userType)
		throws IllegalArgumentException
	{
		if (userType == null)
		{
			throw new IllegalArgumentException("SipUserInfo: null userType");
		}
        
		if (!userType.equals(SipURL.USER_TYPE_IP) &&
			!userType.equals(SipURL.USER_TYPE_PHONE))
		{
			throw new IllegalArgumentException("SipUserInfo: unknown user type");
		}
        
		m_type = userType;
	}
   /**
	* Compare for equality.
	* @param obj Object to set
	* @return true if the two headers are equals, false otherwise.
	*/
   public boolean equals(Object obj)
   {
	   if (!(obj instanceof SipUserInfo))
	   {
		   return false;
	   }
	   SipUserInfo other = (SipUserInfo)obj;
	   if (!m_type.equals(other.m_type))
	   {
		   return false;
	   }
	   if (!m_userName.equalsIgnoreCase(other.m_userName))
	   {
		   return false;
	   }
	   if (m_password != null && other.m_password == null)
		   return false;

	   if (other.m_password != null && m_password == null)
		   return false;

	   return (m_password.equals(other.m_password));
   }

   /**
	* Encode the user information as a string.
	* @return String
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
	   ret.append(m_userName);
    
	   if (m_password != null)
	   {
		   ret.append(Separators.COLON);
		   ret.append(m_password);
	   }
   }
   
   /**
    * Hash code overide.
    */
	public int hashCode()
	{
		return toString().hashCode();
	}
}
