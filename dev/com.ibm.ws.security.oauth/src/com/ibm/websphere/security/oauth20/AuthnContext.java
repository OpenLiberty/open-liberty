/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.oauth20;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The AuthnContext contains information relevant for the user with granted access.
 */
public interface AuthnContext {
    /**
     * This string represents the access token used for this context.
     * @return the access token
     */
    public String getAccessToken();

    /**
     * The scopes that were granted for this access context
     * @return the grantedScopes
     */
    public String[] getGrantedScopes();

    /**
     * The time stamp in milliseconds since the epoch when this token was created. 
     * This can be used along with the lifetime to calculate an expiration time.
     * @return the createdAt in milliseconds
     */
    public long getCreatedAt();

    /**
     * The lifetime of this authentication context in seconds.
     * @return the expiresIn in seconds
     */
    public long getExpiresIn();

    /**
     * The name of the user who authorized this token
     * @return the user name
     */
    public String getUserName();

    /**
     * The HTTPServletRequest from the endpoint invoked
     * @return the request
     */
    public HttpServletRequest getRequest();

    /**
     * The HTTPServletResponse from the endpoint invoked
     * @return the response
     */
    public HttpServletResponse getResponse();

    /**
     * The extension properties
     * @return the properties
     */
    public Map<String, String[]> getProperties();

}
