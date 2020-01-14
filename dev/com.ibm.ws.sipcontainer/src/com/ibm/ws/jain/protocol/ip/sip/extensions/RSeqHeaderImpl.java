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
 * implementation of the RSeq header
 * 
 * @author ran
 */
public class RSeqHeaderImpl extends HeaderImpl implements RSeqHeader
{
	/** the response number of the RSeq header */
	private long m_responseNumber;
	
	/**
	 * constructor
	 */
	public RSeqHeaderImpl() {
		super();
	}
	
	/**
	 * Gets the response number of the RSeq header.
	 * @return the response number of the RSeq header,
	 *  in the range of 1 to 2**32 - 1
	 * @see com.ibm.workplace.jain.protocol.ip.sip.extensions.RSeqHeader#getResponseNumber()
	 */
	public long getResponseNumber() {
		return m_responseNumber;
	}

	/**
	 * Sets the response number of the RSeq header.
	 * delcared final because called from constructor.
	 * @param <var>responseNumber</var> the response number of the RSeq header,
	 *  in the range of 1 to 2**32 - 1
	 * @throws SipParseException if responseNumber is not in the valid range
	 * @see com.ibm.workplace.jain.protocol.ip.sip.extensions.RSeqHeader#setResponseNumber(long)
	 */
	public final void setResponseNumber(long responseNumber) throws SipParseException {
		if (responseNumber < 1 || responseNumber > (1L << 32) - 1) {
			throw new SipParseException("Invalid response number set in RSeq header");
		}
		m_responseNumber = responseNumber;
	}

	/**
	 * parses the value of the RSeq header
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 * @see com.ibm.workplace.jain.protocol.ip.sip.header.HeaderImpl#parseValue(com.ibm.workplace.sip.parser.SipParser)
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		// RSeq =  "RSeq" HCOLON response-num
		long responseNumber = parser.longNumber();
		setResponseNumber(responseNumber);
	}
	
	/**
     * dumps the value of the RSeq header into the specified buffer
	 * @see com.ibm.workplace.jain.protocol.ip.sip.header.HeaderImpl#encodeValue(com.ibm.workplace.sip.parser.util.CharsBuffer)
	 */
	protected void encodeValue(CharsBuffer ret) {
		ret.append(m_responseNumber);
	}
	
	/**
	 * compares two parsed RSeq header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.workplace.jain.protocol.ip.sip.header.HeaderImpl#valueEquals(com.ibm.workplace.jain.protocol.ip.sip.header.HeaderImpl)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!(other instanceof RSeqHeaderImpl)) {
			return false;
		}
		RSeqHeaderImpl o = (RSeqHeaderImpl)other;
		return m_responseNumber == o.m_responseNumber;
	}

	/**
	 * gets the name of this header 
	 * @return literally "RSeq" 
	 * @see com.ibm.workplace.jain.protocol.ip.sip.header.HeaderImpl#getName()
	 */
	public String getName() {
		return name;
	}

	/**
	 * determines whether or not this header can have nested values
	 * @return false
	 * @see com.ibm.workplace.jain.protocol.ip.sip.header.HeaderImpl#isNested()
	 */
	public boolean isNested() {
		return false;
	}
}
