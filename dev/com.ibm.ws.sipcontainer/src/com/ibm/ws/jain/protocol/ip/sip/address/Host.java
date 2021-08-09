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

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;

/**
 * A sip host.
 * 
 * @author Assaf Azaria
 */
public class Host
{
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(Host.class);
    
	//
	// Constants.
	//
	public static final  int HOST_NAME = 1;
    public static  final int IPV4ADDRESS = 2;
	
	//
	// Members.
	//
	
	/** 
     * The host name.
     */
    String m_hostname;

    /** 
     * The address type.
     */
	int m_addressType;

	/**
	 * The address.
	 */
    InetAddress m_inetAddress;

    //
    // Constructors.
    //
    
    /** 
     * constructor
     */
    public Host()
    {
        m_addressType = HOST_NAME;
    }
    
    /** 
     * Construct a new host object.
     * 
     * @param hostName The host name.
     */
    public Host(String hostName) throws IllegalArgumentException
    {
        if (hostName == null)
        {
        	throw new IllegalArgumentException("null host name"); 
        }
        	 
        m_hostname = hostName.trim().toLowerCase();
        m_addressType = IPV4ADDRESS;
    }

    /** 
     * Constructo a new host.
     * 
     * @param name The name
     * @param addrType The address type.
     */
    public Host(String name, int addrType)
    {
        m_addressType = addrType;
        m_hostname = name.trim().toLowerCase();
    }

    /**
     * Return the host name in encoded form.
     * @return String
     */
    public String encode()
    {
        return m_hostname;
    }

    /**
     * 
     */
    public int hashCode(){
    	int result = 1;
    	result = 37 * result + m_hostname.hashCode();
    	return result;
    }
    
    /**
     * Equals.
     */
    public boolean equals(Object obj)
    {
        if (!(obj instanceof Host))
        {
            return false;
        }
        
        Host other = (Host)obj;
        return other.m_hostname.equals(m_hostname);

    }

    /** 
     * Get the HostName field
     */
    public String getHostname()
    {
        return m_hostname;
    }

    /** 
     * Get the Address field
     * @return String
     */
    public String getAddress()
    {
        return m_hostname;
    }

	/**
     * Convenience function to get the raw IP destination address
     * of a SIP message as a String.
     */
    public String getIpAddress()
    {
        String rawIpAddress = null;
        if (m_hostname == null)
        {
        	return null; 
        } 
        if (m_addressType == HOST_NAME)
        {
            try
            {
                if (m_inetAddress == null)
                {
					m_inetAddress = InetAddress.getByName(m_hostname);
                }
                 
                rawIpAddress = m_inetAddress.getHostAddress();
            }
            catch (UnknownHostException ex)
            {
                if (c_logger.isErrorEnabled())
    			{
    	            c_logger.error(
    	                "error.unknown.host.exception",
    	                Situation.SITUATION_CONNECT,
    	                null,
    	                ex);
    	        }
            }
        }
        else
        {
            rawIpAddress = m_hostname;
        }
        
        return rawIpAddress;
    }

    /**
     * Set the hostname member. 
     * @param h String to set
     */
    public void setHostName(String name)
    {
        m_inetAddress = null;
        m_addressType = HOST_NAME;
        
        if (name != null)
        {
        	m_hostname = name.trim().toLowerCase();
        } 

    }

    /** 
     * Set the IP Address.
     *@param address is the address string to set.
     */
    public void setAddress(String address)
    {
        m_inetAddress = null;
        m_addressType = IPV4ADDRESS;
        if (address != null)
        {
        	m_hostname = address.trim();
        } 
    }

    /** 
     * Return true if the address is a DNS host name
     *  (and not an IPV4 address)
     *@return true if the hostname is a DNS name
     */
    public boolean isHostname()
    {
        return m_addressType == HOST_NAME;
    }

    
    /** 
     * Get the inet address from this host.
    * Caches the inet address returned from dns lookup to avoid
    * lookup delays.
    *
     *@throws UnkownHostexception when the host name cannot be resolved.
     */
    public InetAddress getInetAddress() throws UnknownHostException
    {
        if (m_hostname == null)
        {
        	return null; 
        } 
        if (m_inetAddress != null)
        {
        	return m_inetAddress; 
        } 
        
        m_inetAddress = InetAddress.getByName(m_hostname);
        return m_inetAddress;

    }
    
    /**
     * Get a string representation of this object.
     */
    public String toString()
    {
    	return encode();
    }
    
}
