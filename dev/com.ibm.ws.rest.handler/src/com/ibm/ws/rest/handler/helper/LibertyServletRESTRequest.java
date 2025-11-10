/*******************************************************************************
 * Copyright (c) 2013, 2024 IBM Corporation and others.
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
package com.ibm.ws.rest.handler.helper;

import javax.servlet.http.HttpSession;

import com.ibm.wsspi.rest.handler.RESTRequest;

/**
 * Implementation of RESTRequest that uses an HttpServletRequest object.
 */
public interface LibertyServletRESTRequest extends RESTRequest {

    /**
     * Gets the session ID of the request
     *
     * @return String sessionID a String specifying the sessionID of the HTTP request
     */
    public HttpSession getSession();

    /**
     * Gets the session ID of the request
     *
     * @return String sessionID a String specifying the sessionID of the HTTP request
     */
    public String getSessionForAudit();

}
