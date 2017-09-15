/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.admin.internal;

/**
 *
 */
public interface ConfigAdminConstants {

    /**
     * Strings for trace and nls messages (for those classes w/in the bundle that
     * use Tr)
     */
    String TR_GROUP = "config";
    String NLS_PROPS = "com.ibm.ws.config.internal.resources.ConfigMessages";

    /** Property identifying prefix of configuration key */
    String CFG_CONFIG_PREFIX = "config.";

    /** Internal property identifying an instance id for factory-based configuration */
    String CFG_CONFIG_INSTANCE_ID = CFG_CONFIG_PREFIX + "id";

    String VAR_IN_USE = "WLP_VAR_IN_USE";

    /** a subdirectory name where the config files would be persisted */
    String CONFIG_PERSISTENT_SUBDIR = "configs";

}
