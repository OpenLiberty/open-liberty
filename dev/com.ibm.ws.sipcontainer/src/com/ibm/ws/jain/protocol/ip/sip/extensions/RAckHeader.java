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
import jain.protocol.ip.sip.header.Header;

/**
 * The RAck header as defined in rfc 3262, section 7.2:
 * The RAck header is sent in a PRACK request to support reliability of
 * provisional responses.  It contains two numbers and a method tag.
 * The first number is the value from the RSeq header in the provisional
 * response that is being acknowledged.  The next number, and the
 * method, are copied from the CSeq in the response that is being
 * acknowledged.  The method name in the RAck header is case sensitive.
 * 
 * @see RSeqHeader
 * @see CSeqHeader
 * @author ran
 */
public interface RAckHeader extends Header
{
	/**
	 * Gets the response number of the RAck header.
	 * This is the first number in the header value, to be matched
	 * with the number in the RSeq header in the provisional response that
	 * is being acknowledged.
	 * @return the response number of the RAck header,
	 *  in the range of 1 to 2**32 - 1
	 */
	public long getResponseNumber();

	/**
	 * Sets the response number of the RAck header.
	 * @param <var>responseNumber</var> the response number of the RAck header,
	 *  in the range of 1 to 2**32 - 1
	 * @throws SipParseException if responseNumber is not in the valid range
	 */
	public void setResponseNumber(long responseNumber)
		throws SipParseException;

	/**
	 * Gets the sequence number of the RAck header.
	 * This is the second number in the RAck header value, to be matched
	 * with the number in the CSeq header in the provisional response that
	 * is being acknowledged.
	 * @return the sequence number of the RAck header,
	 *  in the range of 0 to 2**32 - 1
	 */
	public long getSequenceNumber();

	/**
	 * Sets the sequence number of the RAck header.
	 * @param <var>sequenceNumber</var> the sequence number of the RAck header,
	 *  in the range of 0 to 2**32 - 1
	 * @throws SipParseException if sequenceNumber is not in the valid range
	 */
	public void setSequenceNumber(long sequenceNumber)
		throws SipParseException;

	/**
	 * Gets the method of the RAck header.
	 * This is the same method from the CSeq header in the provisional response
	 * that is being acknowledged.
	 * @return the method of the RAck header
	 */
	public String getMethod();

	/**
	 * Sets the method of the RAck header.
	 * @param <var>method</var> the method of the RAck header
	 * @throws IllegalArgumentException if method is null
	 * @throws SipParseException if method is not accepted by implementation
	 */
	public void setMethod(String method)
		throws IllegalArgumentException, SipParseException;

	/**
	 * Checks if this RAck header matches the given RSeq and CSeq headers.
	 * @param rseq RSeq header from the provisional response
	 * @param cseq CSeq header from the provisional response
	 * @return true if match, otherwise false
	 */
	public boolean match(RSeqHeader rseq, CSeqHeader cseq);

	/** name of this header */
	public final static String name = "RAck";
}
