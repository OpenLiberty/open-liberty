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
package com.ibm.ws.wssecurity.caller;

import java.util.ResourceBundle;

public class CallerConstants {
    public static final String TR_GROUP = "WSSecurity";

    public static final String TR_RESOURCE_BUNDLE = "com.ibm.ws.wssecurity.resources.WSSecurityMessages";

    public static final ResourceBundle messages = ResourceBundle.getBundle(TR_RESOURCE_BUNDLE);

    public static final String USER_ID = "userIdentifier";
    public static final String GROUP_ID = "groupIdentifier";
    public static final String USER_UNIQUE_ID = "userUniqueIdentifier";
    public static final String REALM_ID = "realmIdentifier";
    public static final String INCLUDE_TOKEN = "includeTokenInSubject";
    public static final String MAP_TO_UR = "mapToUserRegistry";
    public static final String REALM_NAME = "realmName";
    public static final String ALLOW_CACHE_KEY = "allowCustomCacheKey";

}
