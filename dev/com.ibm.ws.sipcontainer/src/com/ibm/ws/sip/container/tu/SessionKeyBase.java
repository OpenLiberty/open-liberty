/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.tu;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;

public class SessionKeyBase {
	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(SessionKeyBase.class);
	
	
	/**
	 * session key based key
	 */
	private String _sessionKeyBase;
	
	/**
	 * sip app id
	 */
	private String _sipAppID;
	
	public SessionKeyBase() {
		
	}
	
	public SessionKeyBase(String sessionKeyBase, String sipAppID) {
		_sessionKeyBase = sessionKeyBase;
		_sipAppID = sipAppID;
	}
	
	public String getKey() {
		return _sessionKeyBase;
	}
	
	
	public String getSipApplicationSessionID() {
		return _sipAppID;
	}
}
