/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.registry;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.Service;

/**
 * Constants and definitions for the WIMUserRegistry.
 *
 */
@Trivial
public interface WIMUserRegistryDefines {

    /**
     * Backslash character.
     */
    char BACKSLASH = '\\';

    /**
     * UserRegistry group level property
     */
    // d115192
    String GROUP_LEVEL = "com.ibm.ws.wim.registry.grouplevel";

    String RETURN_REALM_QUALIFIED_ID = "com.ibm.ws.wim.registry.returnRealmQualifiedId";

    /**
     * Key for the UserRegistry UniqueUserID.
     */
    String UNIQUE_USER_ID_DEFAULT = Service.PROP_UNIQUE_NAME;

    /**
     * Key for the UserRegistry UserSecurityName.
     */
    // New:: Change to Input/Output property
    // String USER_SECURITY_NAME_DEFAULT = Service.PROP_PRINCIPAL_NAME;
    String OUTPUT_USER_SECURITY_NAME_DEFAULT = Service.PROP_UNIQUE_NAME;

    /**
     * Key for the UserRegistry UserSecurityName.
     */
    String USER_SECURITY_NAME_DEFAULT = Service.PROP_PRINCIPAL_NAME;

    // New:: Change to Input/Output property
    /**
     * Key for the UserRegistry UserSecurityName.
     */
    String INPUT_USER_SECURITY_NAME_DEFAULT = Service.PROP_PRINCIPAL_NAME;

    /**
     * Key for the UserRegistry UserDisplayName.
     */
    String USER_DISPLAY_NAME_DEFAULT = Service.PROP_PRINCIPAL_NAME;

    /**
     * Key for the UserRegistry UniqueGroupID.
     */
    // New:: Change to Input/Output property
    // String UNIQUE_GROUP_ID_DEFAULT = Service.PROP_UNIQUE_NAME;
    String OUTPUT_UNIQUE_GROUP_ID_DEFAULT = Service.PROP_UNIQUE_NAME;

    // New:: Change to Input/Output property
    /**
     * Key for the UserRegistry UniqueGroupID.
     */
    String INPUT_UNIQUE_GROUP_ID_DEFAULT = "cn";

    /**
     * Key for the UserRegistry GroupSecurityName.
     */
    // New:: Change to Input/Output property
    // String GROUP_SECURITY_NAME_DEFAULT = "cn";
    String INPUT_GROUP_SECURITY_NAME_DEFAULT = "cn";

    // New:: Change to Input/Output property
    /**
     * Key for the UserRegistry GroupSecurityName.
     */
    String OUTPUT_GROUP_SECURITY_NAME_DEFAULT = Service.PROP_UNIQUE_NAME;

    /**
     * Key for the UserRegistry GroupDisplayName.
     */
    String GROUP_DISPLAY_NAME_DEFAULT = "cn";
}
