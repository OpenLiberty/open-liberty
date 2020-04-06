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
package com.ibm.ws.jain.protocol.ip.sip.extensions;

import jain.protocol.ip.sip.SipParseException;

import com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl;
import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
 * The mime version header.
 * MIME-Version = "MIME-Version" HCOLON 1*DIGIT "." 1*DIGIT 
 *
 * @author Assaf Azaria
 */
public class MimeVersionHeaderImpl extends HeaderImpl 
	implements MimeVersionHeader
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = -8581656462086659946L;

	//
	// Members.
	//
	
	/**
	 * The major part of the version.
	 */
	private short m_major;
	
	/**
	 * The minor part of the version.
	 */
	private short m_minor;
	
	/** 
	 * Construct a new MIME_Version header.
	 */
	public MimeVersionHeaderImpl()
	{
		super();
	}

	//
	// Operations.
	//
	
	/** 
	 * Get the major part of the version.
	 */
	public short getMajorVersion()
	{
		return m_major;
	}

	/** 
	 * Get the minor part of the version.
	 */
	public short getMinorVersion()
	{
		return m_minor;
	}
    
	/** 
	 * Set the major version
	 * @param major the major version.
	 * @throws IllegalArgumentException if the version is not a digit.
	 */
	public void setMajorVersion(short major) 
		throws IllegalArgumentException
	{
		if (major < 0 || major > 9)
		{
			throw new IllegalArgumentException("Mime version - major must be digit");
		}
		
		m_major = major;
	}

	/** 
	 * Set the minor version.
	 * @param minor the minor version.
	 * @throws IllegalArgumentException if the version is not a digit.
	 */
	public void setMinorVersion(short minor) 
		throws IllegalArgumentException
	{
		if (minor < 0 || minor > 9)
		{
			throw new IllegalArgumentException("Mime version - minor must be digit");
		}
	
		m_minor = minor;
	}
	
	/**
	 * parses the value of this header.
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
        // major
        setMajorVersion(parser.shortNumber());
        
        parser.match(DOT);

        // minor
        setMinorVersion(parser.shortNumber());
	}

	/**
	 * Get the encoded value of this header. 
	 * 
	 * @see HeaderImpl#getValue()
	 * @see HeaderImpl#toString()
	 */
	protected void encodeValue(CharsBuffer ret)
	{
		ret.append(m_major);
		ret.append(DOT);
		ret.append(m_minor);
	}
	
	/**
	 * compares two parsed header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#equals(java.lang.Object)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!(other instanceof MimeVersionHeaderImpl)) {
			return false;
		}
		MimeVersionHeaderImpl o = (MimeVersionHeaderImpl)other;
		return m_major == o.m_major
			&& m_minor == o.m_minor;
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
