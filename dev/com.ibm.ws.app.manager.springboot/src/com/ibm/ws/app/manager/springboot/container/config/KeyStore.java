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
import javax.xml.bind.annotation.XmlElement;

/**
 * KeyStore element is defined here:<br>
 * /com.ibm.ws.ssl/resources/OSGI-INF/metatype/metatype.xml
 */
public class KeyStore extends ConfigElement {

    private String password;
    private String location;
    private String type;
    
    @XmlElement(name = "keyEntry")
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
    @XmlAttribute(name = "password")
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
    @XmlAttribute(name = "location")
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
    @XmlAttribute(name = "type")
    public void setType(String type) {
        this.type = type;
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
        if (password != null)
            buf.append("password=\"" + password + "\" ");
        if (location != null)
            buf.append("location=\"" + location + "\" ");
        if (type != null)
            buf.append("type=\"" + type + "\" ");
        buf.append("}");
        return buf.toString();
    }

}
