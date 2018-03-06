/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.springboot.container.config;

import javax.xml.bind.annotation.XmlAttribute;


/**
 * An authData element for holding usernames and passwords
 */
public class AuthData extends ConfigElement {

    private String user;
    private String password;
    private String fatModify;

    public String getUser() {
        return user;
    }

    @XmlAttribute
    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    @XmlAttribute
    public void setPassword(String password) {
        this.password = password;
    }

    @XmlAttribute(name = "fat.modify")
    public void setFatModify(String fatModify) {
        this.fatModify = fatModify;
    }

    public String getFatModify() {
        return fatModify;
    }

    /**
     * Returns a string containing a list of the properties and their values stored
     * for this AuthData object.
     * 
     * @return String representing the data
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("AuthData{");
        buf.append("id=\"" + (getId() == null ? "" : getId()) + "\" ");
        buf.append("user=\"" + (user == null ? "" : user) + "\" ");
        buf.append("password=\"" + (password == null ? "" : "*****") + "\"");
        buf.append("}");
        return buf.toString();
    }
}
