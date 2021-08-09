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
package com.ibm.ws.security.registry.basic.internal;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Simple container class for a user defined in the server.xml
 */
@Trivial
class BasicUser {
    private final String name;
    private final BasicPassword password;

    /**
     * BasicUser representing the user name and password.
     * 
     * @param name name of user
     * @param password password for user
     */
    BasicUser(String name, @Sensitive String password) {
        this.name = name;
        this.password = new BasicPassword(password);
    }

    BasicUser(String name, BasicPassword password) {
        this.name = name;
        this.password = password;
    }

    /**
     * Return the user securityName.
     * 
     * @return user securityName
     */
    String getName() {
        return name;
    }

    /**
     * Return the user password
     * 
     * @return user password
     */
    BasicPassword getPassword() {
        return password;
    }

    /**
     * {@inheritDoc} Equality of a BasicUser is based only
     * on the name of the user. The password is not relevant.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BasicUser)) {
            return false;
        } else {
            BasicUser that = (BasicUser) obj;
            return this.name.equals(that.name);
        }
    }

    /**
     * {@inheritDoc} Return the name.
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * {@inheritDoc} Returns a hash of the name.
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
