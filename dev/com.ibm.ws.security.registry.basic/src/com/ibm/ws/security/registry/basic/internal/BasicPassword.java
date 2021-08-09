/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.registry.basic.internal;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Simple container class for a passrod defined in the server.xml
 */
@Trivial
class BasicPassword {
    private final boolean isHashed;
    private final ProtectedString password;
    private final String hashedPassword;

    /**
     * BasicPassword holds either plain or hashed password
     * 
     * @param password password for user
     */
    BasicPassword(@Sensitive String password) {
        this (password, false);
    }

    /**
     * BasicPassword holds either plain or hashed password
     * 
     * @param password password for user
     * @param isHashed whether password string is hashed.
     */
    BasicPassword(@Sensitive String password, boolean isHashed) {
        this.isHashed = isHashed;
        if (isHashed) {
            this.password = null;
            this.hashedPassword = password;
        } else {
            ProtectedString ps = null;
            if (password != null && password.length() > 0) {
                ps = new ProtectedString(password.toCharArray());
            }
            this.password = ps;
            this.hashedPassword = null;
        }
    }

    /**
     * Return the user password
     * 
     * @return user password
     */
    boolean isHashed() {
        return isHashed;
    }

    /**
     * Return the user password
     * 
     * @return user password
     */
    ProtectedString getPassword() {
        return password;
    }

    /**
     * Return the user password
     * 
     * @return user password
     */
    String getHashedPassword() {
        return hashedPassword;
    }

    @Override
    public String toString() {
        return "****";
    }
}
