/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins;

import com.ibm.ws.security.oauth20.util.OIDCConstants;

public class JWTConstants {

    /*
     * Constants for JWT Configuration -- Metatype defined
     */
    final static public String CONFIG_KEY_NAME = "name";
    final static public String CONFIG_KEY_GRANT_TYPE = "grantType"; // removed
    final static public String CONFIG_KEY_HANDLER_CLASS_NAME = "handlerClassName"; // removed

    /*
     * Constants for JWT Configuration -- free form
     */
    final static public String CONFIG_KEY_JWT_MAX_JTI_CACHE_SIZE = "jwtMaxJtiCacheSize"; // keep
    final static public String CONFIG_KEY_JWT_SKEW_SECONDS = "jwtSkew"; // keep in oauth
    final static public String CONFIG_KEY_JWT_MAX_LIFETIME_SECONDS_ALLOWED = "jwtMaxLifetimeAllowed"; // keep
    final static public String CONFIG_KEY_JWT_PREDEFINED_SCOPES = OIDCConstants.PREDEFINED_SCOPES; // remove new rules
    final static public String CONFIG_KEY_JWT_SIGNATURE_ALGORITHM = OIDCConstants.SIGNATURE_ALGORITHM; // remove "signatureAlgorithm";
    // public final static String ATTRTYPE_REQUEST_JWT = OIDCConstants.ATTRTYPE_REQUEST + ":jwt";
}
