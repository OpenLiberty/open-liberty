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

import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
 * A keepalive "pong" sent in response to a received "ping",
 * according to RFC 5626 5.4:
 * 
 * "
 * When a server receives a double CRLF sequence between SIP messages on
 * a connection-oriented transport such as TCP or SCTP, it MUST
 * immediately respond with a single CRLF over the same connection.
 * "
 * 
 * @author ran
 */
public class KeepalivePong extends MessageImpl
{
	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.message.MessageImpl#getStartLine()
	 */
	public String getStartLine() {
		return null;
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.message.MessageImpl#isRequest()
	 */
	public boolean isRequest() {
		return false; // and it's not a response either
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.message.MessageImpl#writeStartLineToBuffer(com.ibm.ws.sip.parser.util.CharsBuffer, boolean)
	 */
	public void writeStartLineToBuffer(CharsBuffer buffer, boolean network) {
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.message.MessageImpl#writeHeadersToBuffer(com.ibm.ws.sip.parser.util.CharsBuffer, com.ibm.ws.jain.protocol.ip.sip.message.MessageImpl.HeaderForm, boolean)
	 */
    public void writeHeadersToBuffer(CharsBuffer buffer, HeaderForm headerForm,
       	boolean network)
    {
		// override super implementation, and just write CR+LF
		buffer.append('\r').append('\n');
	}
}
