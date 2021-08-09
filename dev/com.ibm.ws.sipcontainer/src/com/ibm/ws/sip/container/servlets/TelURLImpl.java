/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.servlets;

import jain.protocol.ip.sip.SipParseException;

import java.util.Iterator;

import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.TelURL;
import javax.servlet.sip.URI;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.jain.protocol.ip.sip.address.TelephoneNumber;
import com.ibm.ws.jain.protocol.ip.sip.header.ParametersImpl;
import com.ibm.ws.sip.parser.util.CharsBuffer;
import com.ibm.ws.sip.parser.util.CharsBuffersPool;

/**
 * @author Amir Perlman, Dec 24, 2003
 *
 * Implementation of the Tel URL api. 
 * This is a simplified definition of rfc 2806.
 * scheme ":" ["+"] phone *(";" token ["=" token])
 * scheme = "tel" / "fax" / "modem"
 * phone = 1*("0" / "1" / "2" / "3" / "4" / "5" / "6" / "7" / "8" / "9" /
 *            "-" / "." / "(" / ")" /
 *            "*" / "#" / "A" / "B" / "C" / "D" /
 *            "p" / "w")
 * token = 1*(%x21 / %x23-27 / %x2A-2B / %x2D-2E / %x30-39
 *         / %x41-5A / %x5E-7A / %x7C / %x7E)
 */
public class TelURLImpl extends URIImpl implements TelURL, BaseURI
{
	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(TelURLImpl.class);


	/**
	 * Indicates whether the URI is a global URI.
	 * Phone numbers can be either "global" or "local". Global numbers are
	 * unambiguous everywhere. Local numbers are usable only within a
	 * certain area, which is called "context", see section 2.5.2.
	 */
	private boolean m_isGlobal;

	/**
	 * Phone number only stripped of the scheme,parameters and global indicator
	 */
	private String m_phoneNumber;

	/**
	 * The list of url parameters.
	 */
	private ParametersImpl m_params = new com.ibm.ws.jain.protocol.ip.sip.header.ParametersImpl();



	//
	//String constants for supported schemes
	//
	private final static String TEL = "tel";
	private final static String FAX = "fax";
	private final static String MODEM = "modem";


	public TelURLImpl(jain.protocol.ip.sip.address.URI jainURI) {
		super(jainURI);
		try {
			parse(jainURI.getSchemeData(), false);
		} catch (ServletParseException e) { }
	}


	public TelURLImpl(jain.protocol.ip.sip.address.URI jainURI, boolean mayThrow) throws ServletParseException {
		super(jainURI);
		parse(jainURI.getSchemeData(), mayThrow);
	}

	/**
	 * @param url the raw URL string
	 * @param mayThrow true if ok to throw an exception,
	 *  false if just print a log error
	 * @throws ServletParseException if url is invalid and mayThrow is true
	 */
	private void parse(String jainURI, boolean mayThrow) throws ServletParseException
	{
		try {
			parse(jainURI);
		}
		catch (ServletParseException e) {
			if (c_logger.isErrorEnabled())
			{
				Object[] args = { jainURI };
				c_logger.error(
						"error.invalid.tel.url",
						Situation.SITUATION_CREATE,
						args,
						e);
			}
			if (mayThrow) {
				throw e;
			}
		}
	}    



	/**
	 * parses the given string into a tel URL
	 * @param url the raw URL string
	 * @throws ServletParseException if url is invalid
	 */
	private void parse(String url) throws ServletParseException {
		if (null == url) {
			throw new ServletParseException("null URL");
		}

		int length = url.length();
		int position = parseIsGlobal(url);
		if (position >= length) {
			throw new ServletParseException("unexpected end of URL [" + url
					+ ']');
		}
		position = parsePhoneNumber(url, position, isGlobal());
		position = parseParam(url, position);
		if (position != length) {
			throw new ServletParseException("unexpected character in URL [" + url
					+ "] at position [" + position + ']');
		}
	}

	/**
	 * Parse out the parameters from the url
	 * *(";" token ["=" token])
	 * @param url the raw URL string
	 * @param position string position to begin matching
	 * @return string position after consuming the characters needed for this match
	 * @throws ServletParseException if url is invalid
	 */
	private int parseParam(String url, int position) throws ServletParseException
	{
		int length = url.length();
		int i = position;
		while (i < length) {
			char c = url.charAt(i);
			if (c != ';') {
				break;
			}

			// parse key
			String name = token(url, ++i);
			if (name == null) {
				break;
			}

			// parse value
			i += name.length();
			String value;
			if (i < length) {
				c = url.charAt(i);
				if (c == '=') {
					value = token(url, ++i);
					if (value == null) {
						break;
					}
					i += value.length();
				}
				else {
					value = "";
				}
			}
			else {
				value = "";
			}
			if (!m_params.hasParameter(name)) {
				setParameterInt(name, value);
			} else {
				throw new ServletParseException("duplicate parameter [" + name
						+ "] in URL [" + url + ']');        		
			}
		}
		return i;
	}

	/**
	 * parses a token out of the given url string
	 * token = 1*(%x21 / %x23-27 / %x2A-2B / %x2D-2E / %x30-39
	 *         / %x41-5A / %x5E-7A / %x7C / %x7E)
	 * @param url the given url string
	 * @param position start position
	 * @return the matched string, null if no match
	 */
	private static String token(String url, int position) {
		int length = url.length();
		int i;
		for (i = position; i < length; i++) {
			char c = url.charAt(i);
			if (c == '!' || // %x21
					('#' <= c && c <= '\'') || // %x23-27
					('*' <= c && c <= '+') || // %x2A-2B
					('-' <= c && c <= '.') || // %x2D-2E
					('0' <= c && c <= '9') || // %x30-39
					('A' <= c && c <= 'Z') || // %x41-5A
					('^' <= c && c <= 'z') || // %x5E-7A
					c == '|' || // %x7C
					c == '~') // %x7E
			{
				continue;
			}
			break;
		}
		if (i == position) {
			return null;
		}
		return url.substring(position, i);
	}

	/**
	 * Parse out the phone number from the url.
	 * 1*phone-digit
	 * phone-digit = "0" / "1" / "2" / "3" / "4" / "5" / "6" / "7" / "8" / "9" /
	 *               "-" / "." / "(" / ")" /
	 *               "*" / "#" / "A" / "B" / "C" / "D" /
	 *               "p" / "w"
	 * @param url the raw URL string
	 * @param position string position to begin matching
	 * @return string position after consuming the characters needed for this match
	 * @throws ServletParseException if url is invalid
	 */
	private int parsePhoneNumber(String url, int position, boolean isGlobal) throws ServletParseException
	{
		int length = url.length();
		int i;
		for (i = position; i < length; i++) {
			char c = url.charAt(i);
			switch (c) {
			case '0': case '1': case '2': case '3': case '4':
			case '5': case '6': case '7': case '8': case '9':
			case '-': case '.': case '(': case ')':
			case '*': case '#':
			case 'A': case 'a':
			case 'B': case 'b':
			case 'C': case 'c':
			case 'D': case 'd':
			case 'p': case 'P':
			case 'w': case 'W':
				continue;
			case 'e': case 'E': 
			case 'f': case 'F': {
				// changes according to RFC 3966, allowing hexa digits to local numbers only (don't allow this to global numbers)
				if (!isGlobal) {
					continue;
				} else {
					throw new ServletParseException("invalid number in tel URL [" + url + ']');
				}
			}

			}
			break;
		}
		if (i == position) {
			throw new ServletParseException("invalid number in tel URL [" + url + ']');
		}

		m_phoneNumber = url.substring(position, i);
		return i;
	}

	/**
	 * Parse out the is global indicator
	 * @param url the raw URL string
	 * @param position string position to begin matching
	 * @return string position after consuming the characters needed for this match
	 * @throws ServletParseException if url is invalid
	 */
	private int parseIsGlobal(String url) throws ServletParseException
	{
		m_isGlobal = url.charAt(0) == '+';
		if (m_isGlobal) {
			return 1;
		}
		else {
			return 0;
		}
	}

	/**
	 * @see javax.servlet.sip.TelURL#getPhoneNumber()
	 */
	public String getPhoneNumber()
	{
		return m_phoneNumber;
	}

	/**
	 * @see javax.servlet.sip.TelURL#isGlobal()
	 */
	public boolean isGlobal()
	{
		return m_isGlobal;
	}

	/**
	 * @see javax.servlet.sip.TelURL#getParameter(java.lang.String)
	 */
	public String getParameter(String key)
	{
		super.getParameter(key);
		return m_params.getParameter(key);
	}

	/**
	 * @see javax.servlet.sip.TelURL#getParameterNames()
	 */
	@SuppressWarnings("unchecked")
	public Iterator<String> getParameterNames()
	{
		return m_params.getParameters();
	}

	/**
	 * @see javax.servlet.sip.URI#getScheme()
	 */
	public String getScheme()
	{
		return getJainURI().getScheme();
	}

	/**
	 * @see javax.servlet.sip.URI#isSipURI()
	 */
	public boolean isSipURI()
	{
		return false;
	}


	/**
	 * @see javax.servlet.sip.URI#clone()
	 */
	@Override
	public URI clone()
	{
		TelURLImpl cloned = null;

		cloned = (TelURLImpl) super.clone();

		cloned.m_isGlobal = m_isGlobal;
		cloned.m_phoneNumber = m_phoneNumber;
		cloned.m_params = (ParametersImpl) m_params.clone();

		return cloned;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		return super.hashCode(); 
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return getJainURI().toString();
	}

	/**
	 * Removed from javax - hide 289 API
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		boolean rc = false;
		if (obj instanceof TelURLImpl)
		{
			TelURLImpl tel = ((TelURLImpl) obj);

			if (getJainURI().getScheme().equals(tel.getJainURI().getScheme()) && 
					tel.m_isGlobal == m_isGlobal && 
					m_phoneNumber.equals(tel.m_phoneNumber) && 
					m_params.equals(tel.m_params)) 
			{
				rc = true;
			}
		}

		return rc;
	}

	/**
	 * Helper function to determine whether this scheme is supported by this
	 * class
	 */
	public static boolean isSchemeSupported(String scheme)
	{
		boolean rc = false;
		if (scheme.equalsIgnoreCase(TEL)
				|| scheme.equalsIgnoreCase(FAX)
				|| scheme.equalsIgnoreCase(MODEM))
		{
			rc = true;
		}

		return rc;
	}

	/**
	 * Set parameter, internal method to avoid multiple jain updates
	 * @param name
	 * @param value
	 * @throws IllegalArgumentException
	 */
	private void setParameterInt(String name, String value) throws IllegalArgumentException {
		try {
			m_params.setParameter(name, value);
		} catch (SipParseException e) {
			throw new IllegalStateException(e);
		}    	
	}

	/**
	 *  @see javax.servlet.sip.URI#setParameter(java.lang.String, java.lang.String)
	 */
	public void setParameter(String name, String value) throws IllegalArgumentException {
		super.setParameter(name, value);
		setParameterInt(name, value);
		writeChangesToJain();
	}

	/**
	 *  @see javax.servlet.sip.URI#removeParameter(java.lang.String)
	 */
	public void removeParameter(String name) {
		m_params.removeParameter(name);
		writeChangesToJain();
	}

	/**
	 *  Internal method that changes phone number
	 *  @see javax.servlet.sip.TelURL#setPhoneNumber(java.lang.String, boolean)
	 */
	private void setPhoneNumberInt(String number, boolean global) throws IllegalArgumentException {
		try {
			int position = parseIsGlobal(number);
			if (isGlobal() != global) {
				throw new IllegalArgumentException("invalid number in tel URL [" + number + ']');
			}
			position = parsePhoneNumber(number, position, global);
			m_isGlobal = global;
			if (position < number.length()) {
				throw new IllegalArgumentException("invalid number in tel URL [" + number + ']');
			}
		} catch (ServletParseException e) {
			throw new IllegalArgumentException("invalid number in tel URL [" + number + ']');
		}
	}

	/**
	 * @see javax.servlet.sip.TelURL#getPhoneContext()
	 */
	public String getPhoneContext() {
		return getParameter(TelephoneNumber.PHONE_CONTEXT_TAG);
	}

	/**
	 * @see javax.servlet.sip.TelURL#setPhoneNumber(String, String)
	 */
	public void setPhoneNumber(String number, String phoneContext) throws IllegalArgumentException {
		setPhoneNumberInt(number, false);
		setParameterInt(TelephoneNumber.PHONE_CONTEXT_TAG, phoneContext);
		writeChangesToJain();
	}

	/**
	 * @see javax.servlet.sip.TelURL#setPhoneNumber(String)
	 */
	public void setPhoneNumber(String number) throws IllegalArgumentException {
		setPhoneNumberInt(number, true);
		writeChangesToJain();
	}


	/**
	 * Update jain reference to correct TelURI status.
	 * Note: If this method is not called any changes to the state of this class
	 * would not be relevent.
	 * 
	 * @throws IllegalArgumentException
	 */
	private void writeChangesToJain() throws IllegalArgumentException {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "writeChangesToJain");
		}
		CharsBuffer buffer = CharsBuffersPool.getBuffer();

		try {
			if(m_phoneNumber != null) {
				if (isGlobal()) { // in case of Global Phone we should add '+' sign
					buffer.append('+');
				}
				
				buffer.append(m_phoneNumber);
				
				/*
				 * in case of params
				 * we should add separator sign after phone number
				 */
				if (m_params != null && m_params.size() > 0) { 

					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this,
								"writeChangesToJain",
								"adds ';' separator sign after phone number");
					}

					buffer.append(';');

				} else {
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "writeChangesToJain", "no params in list");
					}
				}
			}
			m_params.encode(buffer, ';', false);
			String str = buffer.toString();
			getJainURI().setSchemeData(str);

		} catch (SipParseException e) {
			throw new IllegalArgumentException(e);
		} finally {
			CharsBuffersPool.putBufferBack(buffer);
		}

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "writeChangesToJain", this);
		}
	}
}
