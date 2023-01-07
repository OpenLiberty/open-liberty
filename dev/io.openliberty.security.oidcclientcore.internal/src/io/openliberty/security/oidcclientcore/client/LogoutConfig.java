/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.security.oidcclientcore.client;

/**
 * Logout Configuration.
 */
public interface LogoutConfig {

    default boolean isNotifyProvider() {
        return false;
    }

    default String getRedirectURI() {
        return "";
    }

    default boolean isAccessTokenExpiry() {
        return false;
    }

    default boolean isIdentityTokenExpiry() {
        return false;
    }

}