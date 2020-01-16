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
 * Base class for IBM-.. headers that allow the application to control
 * message-by-message timers.
 * 
 * All these headers take the form:
 * "IBM-..." HCOLON 1*DIGIT
 * 
 * The header state contains a flag that specifies whether this header
 * came in from the network, or was created by the application.
 * If the header was created by the application, its value is used for
 * controlling the transaction timer.
 * If it came from the network, its value is ignored.
 * 
 * @author ran
 */
public abstract class IbmTimerHeader extends HeaderImpl
{
	/** serialization version identifier */
	private static final long serialVersionUID = 5533351659534603364L;

	/** true if created by the application, false if came through the network */
	private final boolean m_application;

	/** the time value, in milliseconds, or -1 if not set */
	private int m_timeValue;

	/**
	 * constructor
	 * @param application true if created by the application, false if came through the network
	 */
	protected IbmTimerHeader(boolean application) {
		m_application = application;
		m_timeValue = -1;
	}

	/**
	 * @return true if created by the application, false if came through the network
	 */
	public boolean applicationCreated() {
		return m_application;
	}

	/**
	 * @param timeValue the time value, in milliseconds
	 */
	public void setTimeValue(int timeValue) {
		m_timeValue = timeValue;
	}

	/**
	 * @return the time value, in milliseconds
	 */
	public int getTimeValue() {
		return m_timeValue;
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#parseValue(com.ibm.ws.sip.parser.SipParser)
	 */
	protected void parseValue(SipParser parser) throws SipParseException {
		m_timeValue = parser.number();
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#encodeValue(com.ibm.ws.sip.parser.util.CharsBuffer)
	 */
	protected void encodeValue(CharsBuffer buf) {
		buf.append(m_timeValue);
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#valueEquals(com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl)
	 */
	protected boolean valueEquals(HeaderImpl other) {
		if (!(other instanceof IbmTimerHeader)) {
			return false;
		}
		IbmTimerHeader o = (IbmTimerHeader)other;
		return m_timeValue == o.m_timeValue;
	}

	/**
	 * @see com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl#isNested()
	 */
	public boolean isNested() {
		return false;
	}
}
