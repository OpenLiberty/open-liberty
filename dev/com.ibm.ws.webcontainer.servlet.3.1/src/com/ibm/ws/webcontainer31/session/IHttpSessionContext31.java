/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer31.session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionIdListener;

import com.ibm.ws.webcontainer.session.IHttpSessionContext;

/**
 * IHttpSessionContext interface specific to Servlet 3.1
 */
public interface IHttpSessionContext31 extends IHttpSessionContext {

    void addHttpSessionIdListener(HttpSessionIdListener listener, String J2EEName);
    
    public HttpSession generateNewId(HttpServletRequest _request, HttpServletResponse _response, HttpSession existingSession);
}
