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
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

public class AuthDataProperties extends ConfigElement {
    // attributes
    private String user;
    private String password;

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    @XmlAttribute(name = "user")
    public void setUser(String value) {
        user = value;
    }

    @XmlAttribute(name = "password")
    public void setPassword(String value) {
        password = value;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append("{");
        if (user != null)
            buf.append("user=\"" + user + "\" ");
        if (password != null)
            buf.append("password=\"" + password + "\" ");

        buf.append("}");
        return buf.toString();
    }
}