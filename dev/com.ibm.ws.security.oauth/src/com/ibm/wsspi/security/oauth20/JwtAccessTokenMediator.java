/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.oauth20;

import java.util.Map;

/**
* Implement this API and use it within the SPI to customize the claims for the JWT access token.
* The JwtAccessTokenMediator has to be implemented as a single Liberty Service or User Feature in the Liberty Server
* and multiple JwtAccessTokenMediator services or features will result in unpredictable behavior.
*/

public interface JwtAccessTokenMediator {

    /**
     * This method should return the claims as a JSON format string.
     * If this method return null then default JWT will be created. 
     * 
     * @param tokenMap The access token context for this request, including client_id, user name, scopes, and other properties.
     * @return The string content with the claims to be included in JWT, which can be parsed into a JSONObject.
     */
    public String mediateToken(Map<String, String[]> tokenMap);

}
