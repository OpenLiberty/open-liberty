/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.adapter.urbridge.utils;

import com.ibm.wsspi.security.wim.SchemaConstants;

public interface URBridgeConstants {
    // Strings for the map and reverse map
    public static final String USER_DISPLAY_NAME_PROP = "userDisplayNameProperty";
    public static final String USER_SECURITY_NAME_PROP = "userSecurityNameProperty";
    public static final String UNIQUE_USER_ID_PROP = "uniqueUserIdProperty";
    public static final String GROUP_DISPLAY_NAME_PROP = "groupDisplayNameProperty";
    public static final String GROUP_SECURITY_NAME_PROP = "groupSecurityNameProperty";
    public static final String UNIQUE_GROUP_ID_PROP = "uniqueGroupIdProperty";

    public static final String USER_DISPLAY_NAME_DEFAULT_PROP = "displayName";
    public static final String USER_SECURITY_NAME_DEFAULT_PROP = SchemaConstants.PROP_UNIQUE_NAME;
    public static final String UNIQUE_USER_ID_DEFAULT_PROP = SchemaConstants.PROP_UNIQUE_ID;
    public static final String GROUP_DISPLAY_NAME_DEFAULT_PROP = "displayName";
    public static final String GROUP_SECURITY_NAME_DEFAULT_PROP = SchemaConstants.PROP_UNIQUE_NAME;
    public static final String UNIQUE_GROUP_ID_DEFAULT_PROP = SchemaConstants.PROP_UNIQUE_ID;

    public static final String DISPLAY_NAME = "displayName";

    public static final String CUSTOM_REGISTRY_IMPL_CLASS = "registryImplClass";

    /**
     * The name of the property in vmm configuration data graph which represents a name.
     */
    String CONFIG_PROP_NAME = "name";

    /**
     * The type name of the custom properties type.
     */
    String CONFIG_DO_CUSTOM_PROPERTIES = "CustomProperties";

    // LDAP Adapter related configuration
    String CONFIG_PROP_SSL_CONFIGURATION = "sslConfiguration";

    String CONFIG_PROP_VALUE = "value";

    String CONFIG_PROP_OBJECTCLASS = "objectClass";
}
