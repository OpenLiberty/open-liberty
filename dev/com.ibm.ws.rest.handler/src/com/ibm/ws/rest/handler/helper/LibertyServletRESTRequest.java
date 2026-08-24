/*******************************************************************************
 * Copyright (c) 2013, 2025 IBM Corporation and others.
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
 * Liberty-internal extension of RESTRequest that exposes HttpSession access
 * methods needed by the audit subsystem.
 */
public interface LibertyServletRESTRequest extends RESTRequest {

    /**
     * Returns the current HttpSession, creating one if necessary.
     *
     * @return the current HttpSession
     */
    public HttpSession getSession();

    /**
     * Returns the session ID for audit purposes without creating a new session.
     * Returns {@code null} if no session exists.
     *
     * @return the session ID string, or {@code null} if no session exists
     */
    public String getSessionForAudit();

}
