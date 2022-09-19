/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.storage;

public class CookieStorageProperties extends StorageProperties {

    /**
     * Defined as a Boolean type so that we can determine if this property is specifically set by a caller. If the property isn't
     * specifically set, we can defer to any existing logic that determines whether the Secure flag should be set for the cookie.
     */
    private Boolean isSecure = null;

    /**
     * Defined as a Boolean type so that we can determine if this property is specifically set by a caller. If the property isn't
     * specifically set, we can defer to any existing logic that determines whether the HttpOnly flag should be set for the cookie.
     */
    private Boolean isHttpOnly = null;

    public void setSecure(boolean isSecure) {
        this.isSecure = isSecure;
    }

    public boolean isSecureSet() {
        return isSecure != null;
    }

    public boolean isSecure() {
        return isSecure;
    }

    public void setHttpOnly(boolean isHttpOnly) {
        this.isHttpOnly = isHttpOnly;
    }

    public boolean isHttpOnlySet() {
        return isHttpOnly != null;
    }

    public boolean isHttpOnly() {
        return isHttpOnly;
    }

    @Override
    public String toString() {
        String result = "CookieStorageProperties:{";
        result += "storageLifetimeSeconds=" + storageLifetimeSeconds + ", ";
        result += "isSecure=" + isSecure + ", ";
        result += "isHttpOnly=" + isHttpOnly;
        result += "}";
        return result;
    }

}
