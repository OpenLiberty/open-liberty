/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package com.ibm.ws.javaee.dd.web.common;

/**
 *
 */
public interface LoginConfig {

    /**
     * @return &lt;auth-method>, or null if unspecified
     */
    String getAuthMethod();

    /**
     * @return &lt;realm-name>, or null if unspecified
     */
    String getRealmName();

    /**
     * @return &lt;form-login-config>, or null if unspecified
     */
    FormLoginConfig getFormLoginConfig();

}
