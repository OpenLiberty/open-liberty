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
 * The RSeq header as defined in rfc 3262, section 7.1:
 * The RSeq header is used in provisional responses in order to transmit
 * them reliably.  It contains a single numeric value from 1 to 2**32 - 1
 * 
 * @see CSeqHeader
 * @see RAckHeader
 * @author ran
 */
public interface RSeqHeader extends Header
{
	/**
	 * Gets the response number of the RSeq header.
	 * @return the response number of the RSeq header,
	 *  in the range of 1 to 2**32 - 1
	 */
	public long getResponseNumber();

	/**
	 * Sets the response number of the RSeq header.
	 * @param <var>responseNumber</var> the response number of the RSeq header,
	 *  in the range of 1 to 2**32 - 1
	 * @throws SipParseException if responseNumber is not in the valid range
	 */
	public void setResponseNumber(long responseNumber)
		throws SipParseException;

	/** name of this header */
	public final static String name = "RSeq";
}
