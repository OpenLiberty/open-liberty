/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport.netty;

import java.net.InetSocketAddress;

import com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer;

public class SipMessageEvent {
	private final SipMessageByteBuffer sipMsg;
	private final InetSocketAddress remoteAddr;

	public SipMessageEvent(final SipMessageByteBuffer data, final InetSocketAddress addr) {
		this.sipMsg = data;
		this.remoteAddr = addr;
	}

	public InetSocketAddress getRemoteAddress() {
		return remoteAddr;
	}

	public SipMessageByteBuffer getSipMsg() {
		return sipMsg;
	}
}
