/*******************************************************************************
 * Copyright (c) 2010,2024 IBM Corporation and others.
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

package com.ibm.ws.config.admin.internal;

/**
 * Configuration administration service constants.
 *
 * See also {@link com.ibm.ws.config.xml.internal.XMLConfigConstants}.
 */
public interface ConfigAdminConstants {
    /** Trace group */
    String TR_GROUP = "config";
    /** NLS properties package name. */
    String NLS_PROPS = "com.ibm.ws.config.internal.resources.ConfigMessages";

    /** Configuration ID property name prefix. */
    String CFG_CONFIG_PREFIX = "config.";

    /** Configuration ID property name. */
    String CFG_CONFIG_INSTANCE_ID = CFG_CONFIG_PREFIX + "id";

    /** TBD */
    // TODO: This appears to be unused.
    String VAR_IN_USE = "WLP_VAR_IN_USE";

    /** Configuration resource file name. */
    String CONFIG_PERSISTENT = "configs.data";
}
