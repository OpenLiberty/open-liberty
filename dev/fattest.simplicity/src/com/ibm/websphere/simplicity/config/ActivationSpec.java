/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class ActivationSpec extends ConfigElement {
    // attributes
    private String authDataRef;

    // nested elements
    @XmlElement(name = "authData")
    private ConfigElementList<AuthData> authDatas;
    @XmlElement(name = "properties.dcra")
    private ConfigElementList<JCAGeneratedProperties> properties_dcra;
    @XmlElement(name = "properties.FAT1")
    private ConfigElementList<JCAGeneratedProperties> properties_FAT1;

    public ConfigElementList<AuthData> getAuthData() {
        return authDatas == null ? (authDatas = new ConfigElementList<AuthData>()) : authDatas;
    }

    public String getAuthDataRef() {
        return authDataRef;
    }

    public ConfigElementList<JCAGeneratedProperties> getProperties_dcra() {
        return properties_dcra == null ? (properties_dcra = new ConfigElementList<JCAGeneratedProperties>()) : properties_dcra;
    }

    public ConfigElementList<JCAGeneratedProperties> getProperties_FAT1() {
        return properties_FAT1 == null ? (properties_FAT1 = new ConfigElementList<JCAGeneratedProperties>()) : properties_FAT1;
    }

    @XmlAttribute
    public void setAuthDataRef(String authDataRef) {
        this.authDataRef = authDataRef;
    }

    @SuppressWarnings("unchecked")
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        buf.append("id=\"" + (getId() == null ? "" : getId()) + "\" ");
        if (authDataRef != null)
            buf.append("authDataRef=\"" + authDataRef + "\" ");

        List<?> nestedElementsList = Arrays.asList(
                                                   authDatas,
                                                   properties_dcra,
                                                   properties_FAT1
                        );
        for (ConfigElementList<?> nestedElements : (List<ConfigElementList<?>>) nestedElementsList)
            if (nestedElements != null && nestedElements.size() > 0)
                for (Object o : nestedElements)
                    buf.append(", " + o);
        buf.append("}");
        return buf.toString();
    }
}
