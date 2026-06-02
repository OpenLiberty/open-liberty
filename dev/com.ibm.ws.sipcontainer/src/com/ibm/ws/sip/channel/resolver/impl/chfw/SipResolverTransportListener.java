/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.channel.resolver.impl.chfw;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;

public interface SipResolverTransportListener {
	/*
	 * 
	 */
	public void responseReceived(WsByteBuffer byteBuffer);
	
	/*
	 * Indicates that an error occurred on the transport. This is typically not 
	 * catastrophic and should result in a retry.
	 */
	public void transportError(Exception	exception, SipResolverTransport transport);

	/*
	 * Indicates a catastrophic failure, basically that several retires have failed and
	 * that the service is not going to work. Should result in error responses for any
	 * outstanding requrest.
	 */
	public void transportFailed(Exception	exception, SipResolverTransport transport);
}
