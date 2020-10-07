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
package com.ibm.ws.jain.protocol.ip.sip.message;

/**
 * This is the slower Sip version object. It consumes more memory
 * than other SipVersionXXImpl
 * @author Moti
 */
class SipVersionImpl implements SipVersion
{
	/**
	 * The version string.
	 * @deprecated Moti : use the new SipServer20Impl
	 */
	private String m_version = null;
	
	/**
	 * Construct a new SipVersion object.
	 * @param version The version string.
	 */
	public SipVersionImpl(String version)
	{
		m_version = version;
	}
	
	/**
	 * Construct a new SipVersion object.
	 * @param major The major version.
	 * @param minor The minor version.
	 */
	public SipVersionImpl(int major, int minor)
	{
		System.out.println("SipVersion(int major, int minor) should be depracted.");
		StringBuffer buf = new StringBuffer("SIP/");
		buf.append(major).append(".").append(minor);
		m_version = buf.toString();
	}
	
	public String getVersionMajor()
	{
		if (m_version == null)
		{    
			return null; 
		} 
    
		// Find the number between the slash to the dot.
		int begin = m_version.indexOf('/');
		int end = m_version.indexOf('.');
		return m_version.substring(begin+1, end-1);
	}

	/**
	 * Get the minor version number.
	 */
	public String getVersionMinor()
	{
		if (m_version == null)
		{
			return null; 
		} 
    
		// Find the number after the dot.
		int begin = m_version.indexOf('.');
		return m_version.substring(begin +1);	 
	}
	
	/**
	 * Get a string representation of this sip version.
	 */
	public String toString()
	{
		return m_version;
	}
	
	/**
	 * Equals implementation.
	 */
	public boolean equals(Object obj)
	{
		if (obj == null)
			return false;
		
		if (!(obj instanceof SipVersionImpl))
		{
			return false;
		}
		
		SipVersionImpl other = (SipVersionImpl)obj;
		
		return m_version.equals(other.m_version);
	}




	
}
