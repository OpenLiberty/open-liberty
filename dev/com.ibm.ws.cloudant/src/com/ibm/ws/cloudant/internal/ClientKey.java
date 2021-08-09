/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cloudant.internal;

import java.util.Arrays;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Key for CloudantClient map.
 */
class ClientKey {
    private final String appClassLoaderId;
    private final int hashcode;
    private final String user;
    private final char[] passwordChars;

    /**
     * Construct a new key from the application classloader identifier and user/password.
     */
    ClientKey(String appClassLoaderId, String user, @Sensitive String password) {
        this.appClassLoaderId = appClassLoaderId;
        this.user = user;
        this.passwordChars = password == null ? null : password.toCharArray();
        hashcode = (appClassLoaderId == null ? 0 : appClassLoaderId.hashCode()) + (user == null ? 0 : user.hashCode());
    }

    @FFDCIgnore(ClassCastException.class)
    public boolean equals(Object o) {
        ClientKey otherKey;
        try {
            otherKey = (ClientKey) o;
        } catch (ClassCastException x) {
            return false;
        }

        return o != null
                && (appClassLoaderId == null ? otherKey.appClassLoaderId == null : appClassLoaderId.equals(otherKey.appClassLoaderId))
                && (user == null ? otherKey.user == null : user.equals(otherKey.user))
                && Arrays.equals(passwordChars, otherKey.passwordChars);
    }

    /**
     * Returns the classloader identifier for the application thread context classloader, if included in the key.
     * 
     * @return the classloader identifier for the application thread context classloader, if included in the key.
     */
    final String getApplicationClassLoaderIdentifier() {
        return appClassLoaderId;
    }

    public final int hashCode() {
        return hashcode;
    }

    @Trivial
    public String toString() {
        return new StringBuilder("ClientKey@").append(Integer.toHexString(hashcode)).append(' ')
                .append(user).append('|')
                .append(passwordChars == null ? null : '*').append('|')
                .append(appClassLoaderId).toString();
    }
}