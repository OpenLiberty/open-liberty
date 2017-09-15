/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.tai;

/**
 * Represents security configurable options for Trust Association.
 */
public interface TAIConfig {

    public static final String KEY_INVOKE_BEFORE_SSO = "invokeBeforeSSO";

    public static final String KEY_INVOKE_AFTER_SSO = "invokeAfterSSO";

    public static final String KEY_ADD_LTPA_TO_RESPONSE = "addLTPACookieToResponse";

    public boolean isFailOverToAppAuthType();

    public boolean isInvokeForUnprotectedURI();

    public boolean isInvokeForFormLogin();
}
