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
public class KeyEntry extends ConfigElement {

    public final static String XML_ATTRIBUTE_NAME_NAME = "name";
    private String name;

    public final static String XML_ATTRIBUTE_NAME_KEY_PASSWORD = "keyPassword";
    private String keyPassword;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the keyPassword
     */
    public String getKeyPassword() {
        return keyPassword;
    }

    /**
     * @param keyPassword the keyPassword to set
     */
    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("KeyEntry{");
        if (null != null)
            buf.append("name=\"" + name + "\" ");
        if (keyPassword != null)
            buf.append("keyPassword=\"" + keyPassword + "\" ");
        buf.append("}");
        return buf.toString();
    }

}
