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

public class AdminObject extends ConfigElement {
    // attributes
    private String jndiName;

    // nested elements
    @XmlElement(name = "properties.CalendarApp.CalendarRA")
    private ConfigElementList<JCAGeneratedProperties> properties_CalendarApp_CalendarRA;

    @XmlElement(name = "properties.CalendarRA")
    private ConfigElementList<JCAGeneratedProperties> properties_CalendarRA;

    @XmlElement(name = "properties.dcra")
    private ConfigElementList<JCAGeneratedProperties> properties_dcra;

    @XmlElement(name = "properties.dcra.Date")
    private ConfigElementList<JCAGeneratedProperties> properties_dcra_Date;

    @XmlElement(name = "properties.dcra.LinkedList")
    private ConfigElementList<JCAGeneratedProperties> properties_dcra_LinkedList;

    @XmlElement(name = "properties.dcra.List")
    private ConfigElementList<JCAGeneratedProperties> properties_dcra_List;

    @XmlElement(name = "properties.DynamicConfigRA.List")
    private ConfigElementList<JCAGeneratedProperties> properties_DynamicConfigRA_List;

    @XmlElement(name = "properties.FAT1")
    private ConfigElementList<JCAGeneratedProperties> properties_FAT1;

    public String getJndiName() {
        return this.jndiName;
    }

    public ConfigElementList<JCAGeneratedProperties> getProperties_CalendarApp_CalendarRA() {
        return properties_CalendarApp_CalendarRA == null ? (properties_CalendarApp_CalendarRA = new ConfigElementList<JCAGeneratedProperties>()) : properties_CalendarApp_CalendarRA;
    }

    public ConfigElementList<JCAGeneratedProperties> getProperties_CalendarRA() {
        return properties_CalendarRA == null ? (properties_CalendarRA = new ConfigElementList<JCAGeneratedProperties>()) : properties_CalendarRA;
    }

    public ConfigElementList<JCAGeneratedProperties> getProperties_dcra() {
        return properties_dcra == null ? (properties_dcra = new ConfigElementList<JCAGeneratedProperties>()) : properties_dcra;
    }

    public ConfigElementList<JCAGeneratedProperties> getProperties_dcra_Date() {
        return properties_dcra_Date == null ? (properties_dcra_Date = new ConfigElementList<JCAGeneratedProperties>()) : properties_dcra_Date;
    }

    public ConfigElementList<JCAGeneratedProperties> getProperties_dcra_LinkedList() {
        return properties_dcra_LinkedList == null ? (properties_dcra_LinkedList = new ConfigElementList<JCAGeneratedProperties>()) : properties_dcra_LinkedList;
    }

    public ConfigElementList<JCAGeneratedProperties> getProperties_dcra_List() {
        return properties_dcra_List == null ? (properties_dcra_List = new ConfigElementList<JCAGeneratedProperties>()) : properties_dcra_List;
    }

    public ConfigElementList<JCAGeneratedProperties> getProperties_DynamicConfigRA_List() {
        return properties_DynamicConfigRA_List == null ? (properties_DynamicConfigRA_List = new ConfigElementList<JCAGeneratedProperties>()) : properties_DynamicConfigRA_List;
    }

    public ConfigElementList<JCAGeneratedProperties> getProperties_FAT1() {
        return properties_FAT1 == null ? (properties_FAT1 = new ConfigElementList<JCAGeneratedProperties>()) : properties_FAT1;
    }

    @XmlAttribute
    public void setJndiName(String jndiName) {
        this.jndiName = ConfigElement.getValue(jndiName);
    }

    @SuppressWarnings("unchecked")
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        buf.append("id=\"" + (getId() == null ? "" : getId()) + "\" ");
        if (jndiName != null)
            buf.append("jndiName=\"" + jndiName + "\" ");

        List<?> nestedElementsList = Arrays.asList(
                                                   properties_CalendarApp_CalendarRA,
                                                   properties_CalendarRA,
                                                   properties_dcra,
                                                   properties_dcra_Date,
                                                   properties_dcra_LinkedList,
                                                   properties_dcra_List,
                                                   properties_DynamicConfigRA_List,
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
