/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.sip.stack.internalapi;

import java.util.List;

import com.ibm.wsspi.sip.channel.resolver.SIPUri;

public interface NaptrRequestListener {
	/**
	 * The listener will receive as a response on the NAPTR query
	 * list of the SipURLs.
	 * @param results
	 */
	public void handleResolve(List<SIPUri> results);
	
	/**
	 * Returns error if the resolve was failed.
	 * @param e
	 */
	public void error(Exception e);
}
