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
import jain.protocol.ip.sip.header.CSeqHeader;

import com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl;
import com.ibm.ws.sip.parser.SipParser;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
 * implementation of the RAck header
 * 
 * @author ran
 */
public class RAckHeaderImpl extends HeaderImpl implements RAckHeader
{
	/** the response number of the RAck header */
	private long m_responseNumber;
	
	/** the sequence number of the RAck header */
	private long m_sequenceNumber;
	
	/** the method of the RAck header */
	private String m_method;
	
	/**
	 * constructor
	 */
	public RAckHeaderImpl() {
		super();
	}
	
	/**
	 * Gets the response number of the RAck header.
	 * This is the first number in the header value, to be matched
	 * with the number in the RSeq header in the provisional response that
	 * is being acknowledged.
	 * @return the response number of the RAck header,
	 *  in the range of 1 to 2**32 - 1
	 * @see com.ibm.workplace.jain.protocol.ip.sip.extensions.RAckHeader#getResponseNumber()
	 */
	public long getResponseNumber() {
		return m_responseNumber;
	}

	/**
	 * Sets the response number of the RAck header.
	 * @param <var>responseNumber</var> the response number of the RAck header,
	 *  in the range of 1 to 2**32 - 1
	 * @throws SipParseException if responseNumber is not in the valid range
	 * @see com.ibm.workplace.jain.protocol.ip.sip.extensions.RAckHeader#setResponseNumber(long)
	 */
	public void setResponseNumber(long responseNumber) throws SipParseException {
		if (responseNumber < 1 || responseNumber > (1L << 32) - 1) {
			throw new SipParseException("Invalid response number set in RAck header");
		}
		m_responseNumber = responseNumber;
	}

	/**
	 * Gets the sequence number of the RAck header.
	 * This is the second number in the RAck header value, to be matched
	 * with the number in the CSeq header in the provisional response that
	 * is being acknowledged.
	 * @return the sequence number of the RAck header,
	 *  in the range of 0 to 2**32 - 1
	 * @see com.ibm.workplace.jain.protocol.ip.sip.extensions.RAckHeader#getSequenceNumber()
	 */
	public long getSequenceNumber() {
		return m_sequenceNumber;
	}

	/**
	 * Sets the sequence number of the RAck header.
	 * @param <var>sequenceNumber</var> the sequence number of the RAck header,
	 *  in the range of 0 to 2**32 - 1
	 * @throws SipParseException if sequenceNumber is not in the valid range
	 * @see com.ibm.workplace.jain.protocol.ip.sip.extensions.RAckHeader#setSequenceNumber(long)
	 */
	public void setSequenceNumber(long sequenceNumber) throws SipParseException {
		if (sequenceNumber < 0 || sequenceNumber > (1L << 32) - 1) {
			throw new SipParseException("Invalid sequence number set in RAck header");
		}
		m_sequenceNumber = sequenceNumber;
	}

	/**
	 * Gets the method of the RAck header.
	 * This is the same method from the CSeq header in the provisional response
	 * that is being acknowledged.
	 * @return the method of the RAck header
	 * @see com.ibm.workplace.jain.protocol.ip.sip.extensions.RAckHeader#getMethod()
	 */
	public String getMethod() {
		return m_method;
	}

	/**
	 * Sets the method of the RAck header.
	 * @param <var>method</var> the method of the RAck header
	 * @throws IllegalArgumentException if method is null
	 * @throws SipParseException if method is not accepted by implementation
	 * @see com.ibm.workplace.jain.protocol.ip.sip.extensions.RAckHeader#setMethod(java.lang.String)
	 */
	public void setMethod(String method) throws IllegalArgumentException, SipParseException {
		if (method == null) {
			throw new IllegalArgumentException("Null method set in RAck header");
		}
		if (method.length() == 0) {
			throw new SipParseException("Empty method set in RAck header");
		}
		m_method = method;
	}

	/**
	 * parses the value of the RAck header
	 * @param parser a parser pre-initialized with a buffer that's ready for parsing
	 * @see com.ibm.workplace.jain.protocol.ip.sip.header.HeaderImpl#parseValue(com.ibm.workplace.sip.parser.SipParser)
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		// RAck =  "RAck" HCOLON response-num LWS CSeq-num LWS Method
		long responseNumber = parser.longNumber();
		if (!Character.isSpaceChar(parser.LA())) {
			throw new SipParseException("whitespace expected after response-num in RAck header");
		}
		parser.lws();
		long sequenceNumber = parser.longNumber();
		if (!Character.isSpaceChar(parser.LA())) {
			throw new SipParseException("whitespace expected after CSeq-num in RAck header");
		}
		String method = parser.nextToken(ENDL);

		setResponseNumber(responseNumber);
		setSequenceNumber(sequenceNumber);
		setMethod(method);
	}

	/**
     * dumps the value of the RAck header into the specified buffer
	 * @see com.ibm.workplace.jain.protocol.ip.sip.header.HeaderImpl#encodeValue(com.ibm.workplace.sip.parser.util.CharsBuffer)
	 */
	protected void encodeValue(CharsBuffer ret) {
		ret.append(m_responseNumber)
			.append(SP)
			.append(m_sequenceNumber)
			.append(SP)
			.append(m_method);
	}
	
	/**
	 * compares two parsed RAck header values
	 * @param other the other header to compare with
	 * @return true if both header values are identical, otherwise false
	 * @see com.ibm.workplace.jain.protocol.ip.sip.header.HeaderImpl#valueEquals(com.ibm.workplace.jain.protocol.ip.sip.header.HeaderImpl)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!(other instanceof RAckHeaderImpl)) {
			return false;
		}
		RAckHeaderImpl o = (RAckHeaderImpl)other;
		
		return m_responseNumber == o.m_responseNumber
			&& m_sequenceNumber == o.m_sequenceNumber
			&& m_method == null
				? o.m_method == null
				: m_method.equals(o.m_method);
	}

	/**
	 * gets the name of this header 
	 * @return literally "RAck" 
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

	/**
	 * Checks if this RAck header matches the given RSeq and CSeq headers.
	 * @param rseq RSeq header from the provisional response
	 * @param cseq CSeq header from the provisional response
	 * @return true if match, otherwise false
	 * @see com.ibm.workplace.jain.protocol.ip.sip.extensions.RAckHeader#match(com.ibm.workplace.jain.protocol.ip.sip.extensions.RSeqHeader, jain.protocol.ip.sip.header.CSeqHeader)
	 */
	public boolean match(RSeqHeader rseq, CSeqHeader cseq) {
		return m_responseNumber == rseq.getResponseNumber()
			&& m_sequenceNumber == cseq.getSequenceNumber()
			&& m_method.equals(cseq.getMethod());
	}
}
