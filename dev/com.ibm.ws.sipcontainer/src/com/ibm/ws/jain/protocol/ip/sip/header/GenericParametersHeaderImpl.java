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
package com.ibm.ws.jain.protocol.ip.sip.header;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.header.CSeqHeader;
import jain.protocol.ip.sip.header.CallIdHeader;

import java.util.Arrays;

import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
 * A generic SIP header that matches the definition:
 * field-value *(;parameter-name=parameter-value)
 * 
 * Instances of this class are never created by the stack for messages that come
 * in from the network. They are only created by the application using the factory.
 * 
 * Important: this class does not handle address headers - that is, headers
 * where the "field-value" matches a "name-addr". Attempting to parse a
 * string value such as <sip:alice@atlanta.com;transport=tcp>;tag=123 would
 * result in field-value="<sip:alice@atlanta.com" and a transport parameter
 * with a value of "tcp>".
 * 
 * @author ran
 */
public class GenericParametersHeaderImpl extends ParametersHeaderImpl
{
	/** unique serialization version identifier */
	private static final long serialVersionUID = 3856825838155515719L;

	/** the header name */
	private final String m_name;

	/** the header field value, not including parameters. may be null. */
	private String m_value;

	/**
	 * a list of all known header fields which are not parameterable
	 * by definition.
	 * attempting to create one of these as a parameterable causes the
	 * parse() method to throw an exception.
	 */
	private static final String[] NON_PARAMETERABLES = {
		CallIdHeader.name, CSeqHeader.name
	};
	static {
		// need to sort, to support calling binarySearch() later on
		Arrays.sort(NON_PARAMETERABLES);
	}

	/**
	 * constructor
	 * @param name the header name
	 */
	public GenericParametersHeaderImpl(String name) {
		this(name, null);
	}

	/**
	 * constructor
	 * @param name the header name
	 * @param value the header field value, not including parameters
	 */
	public GenericParametersHeaderImpl(String name, String value) {
		super();
		m_name = name;
		m_value = value;
	}

	/**
	 * checks if the given header field name is parameterable
	 * @param name the name of the header
	 * @return true if parameterable, false if non-parameterable
	 */
	private static boolean isParameterable(String name) {
		int p = Arrays.binarySearch(NON_PARAMETERABLES, name);
		return p < 0;
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#parseValue(com.ibm.ws.sip.parser.SipParser)
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		// verify this header field is parameterable by definition
		String name = getName();
		if (!isParameterable(name)) {
			throw new SipParseException("not parameterable [" + name + ']');
		}

		// parse the string left of the parameters
		m_value = parser.nextToken(SEMICOLON);

		// parse the parameters
		super.parseValue(parser);
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#encodeValue(com.ibm.ws.sip.parser.util.CharsBuffer)
	 */
	protected void encodeValue(CharsBuffer buffer) {
		// write the value
		String value = m_value;
		if (value != null) {
			buffer.append(value);
		}

		// write the parameters
		super.encodeValue(buffer);
	}

	/**
	 * copies parameters from one header to another.
	 * upon return from this call, both headers will have the exact
	 * same value and list of parameters.
	 * future modifications to parameters of one header
	 * will affect the other.
	 * 
	 * @param source header to read value and parameters from
	 */
	public void assign(GenericParametersHeaderImpl source) {
		m_value = source.m_value;
		super.assign(source);
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#getName()
	 */
	public String getName() {
		return m_name;
	}

	/**
	 * @param value the header field value, not including parameters
	 */
	public void setFieldValue(String value) {
		m_value = value;
	}

	/**
	 * @return the header field value, not including parameters
	 */
	public String getFieldValue() {
		return m_value;
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#getListSeparator()
	 */
	protected char getListSeparator() {
		return SEMICOLON;
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl#getParamSeparator()
	 */
	protected char getParamSeparator() {
		return SEMICOLON;
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#isNested()
	 */
	public boolean isNested() {
		return false;
	}
}
