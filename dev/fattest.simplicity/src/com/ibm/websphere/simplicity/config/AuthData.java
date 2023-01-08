/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.common.apiservices.Bootstrap;
import componenttest.common.apiservices.BootstrapProperty;

/**
 * An authData element for holding usernames and passwords
 */
public class AuthData extends ConfigElement implements ModifiableConfigElement {

    private static final Class<AuthData> c = AuthData.class;

    private String user;
    private String password;

    @XmlAttribute
    public String krb5Principal;

    @XmlAttribute
    public String krb5TicketCache;

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
     * Modifies the element if the fat.modify="true" attribute was configured for this element.
     *
     * @param config The ServerConfiguration instance.
     */
    @Override
    public void modify(ServerConfiguration config) throws Exception {
        if (fatModify != null && fatModify.toLowerCase().equals("true")) {
            updateAuthDataFromBootStrapDBUser1();
        }
    }

    /**
     * Update an AuthData using database.user1 from Bootstrap
     */
    public void updateAuthDataFromBootStrapDBUser1() throws Exception {
        Log.entering(c, "updateAuthDataFromBootStrapDBUser1");
        Bootstrap bs = Bootstrap.getInstance();

        if (bs.getValue(BootstrapProperty.DB_USER1.getPropertyName()) == null) {
            Log.info(c, "updateAuthDataFromBootStrapDBUser1", "Database user1 property was not found, skip setting user id and password");
            return;
        }

        this.user = bs.getValue(BootstrapProperty.DB_USER1.getPropertyName());
        this.password = bs.getValue(BootstrapProperty.DB_PASSWORD1.getPropertyName());
        Log.exiting(c, "updateAuthDataFromBootStrapDBUser1");
    }

    /**
     * Update an AuthData using database.user2 from Bootstrap
     */
    public void updateAuthDataFromBootStrapDBUser2() throws Exception {
        Log.entering(c, "updateAuthDataFromBootStrapDBUser2");
        Bootstrap bs = Bootstrap.getInstance();

        if (bs.getValue(BootstrapProperty.DB_USER2.getPropertyName()) == null) {
            Log.info(c, "updateAuthDataFromBootStrapDBUser2", "Database user2 property was not found, skip setting user id and password");
            return;
        }

        this.user = bs.getValue(BootstrapProperty.DB_USER2.getPropertyName());
        this.password = bs.getValue(BootstrapProperty.DB_PASSWORD2.getPropertyName());
        Log.exiting(c, "updateAuthDataFromBootStrapDBUser2");
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
        if (krb5Principal != null)
            buf.append(", krb5Principal=\"" + krb5Principal + "\"");
        if (krb5TicketCache != null)
            buf.append(", krb5TokenCache=\"" + krb5TicketCache + "\"");
        buf.append("}");
        return buf.toString();
    }
}
