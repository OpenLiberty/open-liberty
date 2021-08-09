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

import java.util.EventListener;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionListener;

/**
 * This interface is used for sharing the HttpSessionListener.
 * The listener is implemented in {@link com.ibm.ws.sip.container.servlets.WASXHttpSessionListener} and stored in {@link com.ibm.ws.sip.container.SipContainer}
 * 
 * When an application started the http listener is added to the servlet context and the interface's method is used to update the SAS in the event of a http session invalidation. 
 */
public interface WASHttpSessionListener extends HttpSessionListener, EventListener {
	
	public void handleHttpSessionDestoyEvent(HttpSession session);
}
