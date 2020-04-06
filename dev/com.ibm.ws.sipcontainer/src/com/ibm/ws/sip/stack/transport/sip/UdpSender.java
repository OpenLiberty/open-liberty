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

package com.ibm.ws.sip.stack.transport.sip;

import java.io.IOException;

import com.ibm.ws.sip.stack.context.MessageContext;
import com.ibm.ws.sip.stack.transaction.transport.UseCompactHeaders;
import com.ibm.wsspi.channelfw.ConnectionLink;

/**
 * common interface for a connection link that can send out UDP messages.
 * this is a SipUdpConnLink on non-Z, and a ZosInboundConnLink on Z.
 * 
 * @author ran
 */
interface UdpSender extends ConnectionLink
{
	/** sends out a message 
	 * @param useCompactHeaders TODO*/
	void send(MessageContext messageSendingContext, UseCompactHeaders useCompactHeaders) throws IOException;

}
