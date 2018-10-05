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

/**
 * KeyStore element is defined here:<br>
 * /com.ibm.ws.ssl/resources/OSGI-INF/metatype/metatype.xml
 */
public class KeyStore extends ConfigElement {

    public static final String XML_ATTRIBUTE_NAME_PASSWORD = "password";
    private String password;

    public static final String XML_ATTRIBUTE_NAME_LOCATION = "location";
    private String location;

    public static final String XML_ATTRIBUTE_NAME_TYPE = "type";
    private String type;

    public static final String XML_ATTRIBUTE_NAME_PROVIDER = "provider";
    private String provider;

    public static final String XML_ELEMENT_NAME_KEY_ENTRY = "keyEntry";
    private ConfigElementList<KeyEntry> keyEntries;

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the provider
     */
    public String getProvider() {
        return provider;
    }

    /**
     * @param type the type to set
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * @return the keyEntries
     */
    public ConfigElementList<KeyEntry> getKeyEntries() {
        if (this.keyEntries == null) {
            this.keyEntries = new ConfigElementList<KeyEntry>();
        }
        return this.keyEntries;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("KeyStore{");
        buf.append("id=\"" + this.getId() + "\" ");
        if (password != null)
            buf.append("password=\"***\" ");
        if (location != null)
            buf.append("location=\"" + location + "\" ");
        if (type != null)
            buf.append("type=\"" + type + "\" ");
        if (provider != null)
            buf.append("provider=\"" + provider + "\" ");
        buf.append("}");
        return buf.toString();
    }

}
