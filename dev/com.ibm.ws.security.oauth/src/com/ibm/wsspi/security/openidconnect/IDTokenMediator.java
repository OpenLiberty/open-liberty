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
package com.ibm.wsspi.security.openidconnect;

import java.util.Map;

/**
* Implement this API and use it within the SPI to customize the claims for the ID token.
* The IDTokenMediator has to be implemented as a single Liberty Service or User Feature in the Liberty Server
* and multiple IDTokenMediator services or features will result in unpredictable behavior.
*/

public interface IDTokenMediator {

    /**
     * This method should return the claims as a JSON format string.
     * If this method returns null then default ID token will be created. 
     * 
     * @param tokenMap The ID token context for this request, including client_id, user name, scopes, and other properties.
     * @return The string content with the claims to be included in ID token, which can be parsed into a JSONObject.
     */
    public String mediateToken(Map<String, String[]> tokenMap);

}
