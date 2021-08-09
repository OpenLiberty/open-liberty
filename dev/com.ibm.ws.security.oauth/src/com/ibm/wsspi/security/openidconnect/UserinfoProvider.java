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
package com.ibm.wsspi.security.openidconnect;

import com.ibm.websphere.security.oauth20.AuthnContext;

/**
 * Implement this API and use it within the SPI to customize the json response for the userinfo endpoint.
 */
public interface UserinfoProvider {

    /**
     * This method should return the userinfo as a JSON format string.
     * If this method return null then it will be ignored. No further handling will happen
     * 
     * @param authnContext The Authentication context for this invocation
     * @return The string content with the userinfo, which can be parsed back to an JSONObject.
     *         This will be provided to anyone invoking the userinfo endpoint
     */
    public String getUserInfo(AuthnContext authnContext);
}
