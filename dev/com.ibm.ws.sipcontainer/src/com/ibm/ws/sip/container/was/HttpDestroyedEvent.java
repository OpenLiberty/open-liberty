/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
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
		// TODO Auto-generated constructor stub
	}

	/**
	 * @return related http session
	 */
	public HttpSession getHTTPSession() {
		return (HttpSession)getSource();
	}
}
