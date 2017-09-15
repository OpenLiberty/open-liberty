/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.util;

/**
 * Internal constants that we do not need on the public interface (SchemaConstants), such as IS_URBRIDGE_RESULT
 */
public interface SchemaConstantsInternal {

    static final String IS_URBRIDGE_RESULT = "isURBridgeResult";
    static final String PROP_DISPLAY_BRIDGE_PRINCIPAL_NAME = "displayBridgePrincipalName";
    static final String PROP_DISPLAY_BRIDGE_CN = "displayBridgeCN";
}
