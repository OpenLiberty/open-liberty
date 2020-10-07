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
import jain.protocol.ip.sip.address.NameAddress;
import jain.protocol.ip.sip.address.URI;

import com.ibm.ws.sip.parser.Lexer;
import com.ibm.ws.sip.parser.Separators;
import com.ibm.ws.sip.parser.util.CharsBuffer;
import com.ibm.ws.sip.parser.util.CharsBuffersPool;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;

/**
 * Name adrress implementation.
 * 
 * @author Assaf Azaria, April 2003.
 */
public class NameAddressImpl implements NameAddress
{
	/** Serialization UID (do not change) */
	private static final long serialVersionUID = 4441607612063633851L;

	/** true if display names are always quoted, false if quoted only when necessary */
	private static final boolean s_forceDisplayNameQuoting =
		SIPTransactionStack.instance().getConfiguration().forceDisplayNameQuoting();

	/**
	 * true if the application is allowed to set a quoted display name.
	 * this flag serves misbehaving applications that insist on passing
	 * a quoted displayName string to the setDisplayName() method.
	 */
	private static final boolean s_allowDisplayNameQuotingByApp =
		SIPTransactionStack.instance().getConfiguration().allowDisplayNameQuotingByApp();

	/**
	 * true if Addresses are always serialized as name-addr (force <> around the URI)
	 */
	private static final boolean s_forceNameAddr =
		SIPTransactionStack.instance().getConfiguration().forceNameAddr();

	//
	// Members.
	//
	/**
	 * The actual address.
	 */
	URI m_address;

	/**
	 * The display name.
	 */	
	String m_displayName;

	//
	// Constructors.
	//
	/**
	 * Construct a new, empty NameAddress object.
	 */
	public NameAddressImpl()
	{
	}

	/**
	 * Construct a new NameAddress object from the given address.
	 * 
	 * @param address The address as a jain URI object. 
	 */
	NameAddressImpl(URI address) 
	{
		m_address = address;
	}

	/**
	 * Construct a new NameAddress object from the given address and
	 * display name.
	 * 
	 * @param displayName The display name.
	 * @param address The address as a jain URI object.
	 */
	NameAddressImpl(String displayName, URI address) 
	{
		m_displayName = displayName;
		m_address = address;
	}


	/**
	 * Gets display name of NameAddress
	 * (Returns null id display name does not exist)
	 * @return display name of NameAddress
	 */
	public String getDisplayName()
	{
		return m_displayName;
	}

	/**
	 * Gets boolean value to indicate if NameAddress
	 * has display name
	 * @return boolean value to indicate if NameAddress
	 * has display name
	 */
	public boolean hasDisplayName()
	{
		return m_displayName != null;
	}

	/**
	 * Removes display name from NameAddress (if it exists)
	 */
	public void removeDisplayName()
	{
		m_displayName = null;
	}

	/**
	 * Sets display name of Header
	 * @param <var>displayName</var> display name
	 * @throws IllegalArgumentException if displayName is null
	 * @throws SipParseException if displayName is not accepted by implementation
	 */
	public void setDisplayName(String displayName)
	throws IllegalArgumentException, SipParseException {
		m_displayName = displayName;
	}

	/**
	 * Gets address of NameAddress
	 * @return address of NameAddress
	 */
	public URI getAddress()
	{
		return m_address;
	}

	/**
	 * Sets address of NameAddress
	 * @param <var>address</var> address
	 * @throws IllegalArgumentException if address is null or not from same
	 * JAIN SIP implementation
	 */
	public void setAddress(URI addr)
	throws IllegalArgumentException
	{
		if (addr == null)
		{
			throw new IllegalArgumentException("NameAddress: null address"); 
		} 

		if(!(addr instanceof URIImpl)) 
		{
			throw new IllegalArgumentException("NameAddress: Address from another impl " + 
					addr.getClass().getName());
		}			

		m_address = addr;
	}

	/**
	 * Gets string representation of NameAddress
	 * @return string representation of NameAddress
	 */
	public String toString()
	{
		CharsBuffer buffer = CharsBuffersPool.getBuffer();
		writeToCharBuffer(buffer, true);

		String value = buffer.toString();
		CharsBuffersPool.putBufferBack(buffer);

		return value; 
	}

	/**
	 * Dump this object to the specified char array
	 * @param buf destination buffer
	 * @param addrSpecFormatAllowed true if addr-spec is allowed,
	 *  false if name-addr is forced.
	 */
	public void writeToCharBuffer(CharsBuffer buf, boolean addrSpecFormatAllowed) {
		// write the display name, if present
		String displayName = m_displayName;

		if ((displayName != null && displayName.length() > 0)) {
			writeDisplayName(displayName, buf);
			buf.append(Separators.SP);
			addrSpecFormatAllowed = false;
		}else if (s_forceNameAddr){
			addrSpecFormatAllowed = false;
		}

		// write the address
		URIImpl uri = (URIImpl)m_address;
		if (uri == null) {
			return;
		}
		if (!addrSpecFormatAllowed) {
			buf.append(Separators.LESS_THAN);
		}
		int uriStart = buf.getCharCount();
		uri.writeToCharBuffer(buf);

		// follow the guidelines of RFC 3261 20.10 (repeated in 20.20 and 20.31)
		// to determine whether addr-spec is allowed
		if (addrSpecFormatAllowed) {
			// see if we used addr-spec inappropriately
			int uriEnd = buf.getCharCount();
			int uriLength = uriEnd - uriStart;
			char[] chars = buf.getCharArray();
			for (int i = 0; i < uriLength; i++) {
				char c = chars[uriStart+i];
				if (c == ',' || c == ';' || c == '?') {
					// change addr-spec to name-addr
					addrSpecFormatAllowed = false;
					break;
				}
			}
			// if we did use addr-spec inappropriately, shift-right the URI
			// one char to the right, and make room for the opening "<".
			if (!addrSpecFormatAllowed) {
				buf.append('x'); // ensure buffer is large enough for another char
				chars = buf.getCharArray();
				for (int i = uriLength; i > 0; i--) {
					chars[uriStart+i] = chars[uriStart+i-1];
				}
				buf.rewind(uriStart);
				buf.append(Separators.LESS_THAN);
				buf.rewind(uriEnd+1);
			}
		}
		if (!addrSpecFormatAllowed) {
			buf.append(Separators.GREATER_THAN);
		}
	}

	/**
	 * serializes the display name to the given buffer.
	 * quotes and escapes if needed.
	 * @param displayName the display name to write. at least 1 char long.
	 * @param buffer destination buffer
	 */
	private static void writeDisplayName(String displayName, CharsBuffer buffer) {
		// 1. check if quoting is done by the application
		if (s_allowDisplayNameQuotingByApp && 
				displayName.charAt(0) == Separators.DOUBLE_QUOTE) {
			buffer.append(displayName);
			return;
		}

		// 2. check if quoting is forced by configuration
		if (s_forceDisplayNameQuoting) {
			buffer.append(Separators.DOUBLE_QUOTE);
			Lexer.writeNoQuotes(displayName, buffer);
			buffer.append(Separators.DOUBLE_QUOTE);
			return;
		}

		int length = displayName.length();
		int start = buffer.getCharCount(); // in case we need to rewind later
		boolean quotes = false;

		// 3. start optimistic, assuming no need to quote
		for (int i = 0; i < length; i++) {
			char c = displayName.charAt(i);
			if (('a' <= c && c <= 'z') ||
					('A' <= c && c <= 'Z') ||
					('0' <= c && c <= '9'))
			{
				// valid token char
				buffer.append(c);
				continue;
			}
			switch (c) {
			case '-': case '.': case '!': case '%': case '*':
			case '_': case '+': case '`': case '\'': case '~':
				// valid token char 
			case ' ': case '\t':
				// valid LWS 	
				buffer.append(c);
				continue;
			}

			// not a token char. need quotes.
			quotes = true;
			break;
		}

		// 4. if quoted, do it again
		if (quotes) {
			buffer.rewind(start);
			buffer.append(Separators.DOUBLE_QUOTE);
			Lexer.writeNoQuotes(displayName, buffer);
			buffer.append(Separators.DOUBLE_QUOTE);
		}
	}

	/**
	 * Indicates whether some other Object is "equal to" this NameAddress
	 * (Note that obj must have the same Class as this NameAddress - this means 
	 * that it must be from the same JAIN SIP implementation)
	 * @param <var>obj</var> the Object with which to compare this NameAddress
	 * @return true if this NameAddress is "equal to" the obj
	 * argument; false otherwise
	 */
	public boolean equals(Object obj)
	{
		if(obj == null || !(obj instanceof NameAddressImpl)) 
		{
			return false;
		}

		NameAddressImpl other = (NameAddressImpl)obj;

		//		boolean ret = false;
		//		if (hasDisplayName())
		//		{
		//			if( other.hasDisplayName() )
		//			{
		//				ret = m_displayName.equals(other.m_displayName);
		//			}
		//		}
		//		else
		//		{
		//			if( !other.hasDisplayName() )
		//			{
		//				ret = true;
		//			}
		//		}

		return  m_address.equals(other.m_address);


	}

	/**
	 * Return the hash code for this object.
	 */
	public int hashCode()
	{
		return toString().hashCode();
	}

	/**
	 * Creates and returns a copy of NameAddress
	 * @return a copy of NameAddress
	 */
	public Object clone()
	{
		try
		{
			NameAddressImpl ret = (NameAddressImpl)super.clone();
			if (m_address != null)
			{
				ret.m_address = (URI)m_address.clone();
			} 
			return ret;
		}
		catch (CloneNotSupportedException e)
		{
			// No way.
			e.printStackTrace();
			// @PMD:REVIEWED:AvoidThrowingCertainExceptionTypesRule: by Amirk on 9/19/04 4:44 PM
			// @PMD:REVIEWED:DoNotUsePrintStackTrace: by Amirk on 9/19/04 4:44 PM
			throw new Error("Clone Error.");
			// @PMD:REVIEWED:DoNotUsePrintStackTrace: by Amirk on 9/19/04 4:46 PM
		}
	}
}
