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
package com.ibm.wsspi.sip.channel.resolver;

import java.util.EventListener;

/*
 * The listener interface for interest in SipURILookup requests
 */
public interface SipURILookupCallback extends EventListener {
	
	/** 
	 * The callback used when the naming service has a successful resolution
	 * for a SipURILookup
	 * @param sl the SipURILookup
	 */
	public void complete(SipURILookup sl);
	
	/** 
	 * The callback used when the naming service generates an exception to a 
	 * SipURILookup
	 * for a SipURI
	 * @param sl the SipURILookup
	 */
	public void error(SipURILookup sl, SipURILookupException e);
	
}
