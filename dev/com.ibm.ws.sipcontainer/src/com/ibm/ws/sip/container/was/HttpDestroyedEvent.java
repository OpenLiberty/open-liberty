/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sip.container.was;

import java.util.EventObject;

import javax.servlet.http.HttpSession;

/**
 * This class is used for dispatching an http session destroy event to the sip container thread.
 *  
 * @author galina
 *
 */
public class HttpDestroyedEvent extends EventObject {

	public HttpDestroyedEvent(HttpSession source) {
		super(source);
	}

	/**
	 * @return related http session
	 */
	public HttpSession getHTTPSession() {
		return (HttpSession)getSource();
	}
}
