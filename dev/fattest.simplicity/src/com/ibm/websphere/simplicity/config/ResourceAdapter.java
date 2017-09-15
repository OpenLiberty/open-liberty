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
package com.ibm.websphere.simplicity.config;

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Represents both of the <resourceAdapter> elements in server.xml
 * 
 * One is for standalone resource adapters, configured at top level, for example,
 * <resourceAdapter location="C:/connectors/myAdapter.rar" autoStart="true">
 * _ <properties.myAdapter .../>
 * </resourceAdapter>
 * 
 * The other is for embedded resource adapters, nested under application, for example,
 * <application name="app1" ...
 * _ <resourceAdapter moduleName="myEmbeddedAdapter" autoStart="true">
 * ___ <properties.app1.myEmbeddedAdapter .../>
 * _ </resourceAdapter>
 * </application>
 */
public class ResourceAdapter extends ConfigElement {
    // attributes
    private String alias; // for RARs embedded in apps only
    private String autoStart;
    private String contextServiceRef;
    private String location; // for standalone RARs only

    // nested elements
    @XmlElement(name = "classloader")
    private ConfigElementList<ClassloaderElement> classloaders; // for standalone RARs only

    @XmlElement(name = "contextService")
    private ConfigElementList<ContextService> contextServices;

    @XmlElement(name = "customize")
    private ConfigElementList<Customize> customizations;

    @XmlElement(name = "properties.CalendarApp.CalendarRA")
    private ConfigElementList<JCAGeneratedProperties> properties_CalendarApp_CalendarRA;

    @XmlElement(name = "properties.CalendarRA")
    private ConfigElementList<JCAGeneratedProperties> properties_CalendarRA;

    @XmlElement(name = "properties.FAT1")
    private ConfigElementList<JCAGeneratedProperties> properties_FAT1s;

    public String getAlias() {
        return alias;
    }

    public String getAutoStart() {
        return autoStart;
    }

    public ConfigElementList<ClassloaderElement> getClassloaders() {
        return classloaders == null ? (classloaders = new ConfigElementList<ClassloaderElement>()) : classloaders;
    }

    public ConfigElementList<ContextService> getContextServices() {
        return contextServices == null ? (contextServices = new ConfigElementList<ContextService>()) : contextServices;
    }

    public String getContextServiceRef() {
        return contextServiceRef;
    }

    public ConfigElementList<Customize> getCustomizes() {
        return customizations == null ? (customizations = new ConfigElementList<Customize>()) : customizations;
    }

    public String getLocation() {
        return location;
    }

    public ConfigElementList<JCAGeneratedProperties> getProperties_CalendarApp_CalendarRA() {
        return properties_CalendarApp_CalendarRA == null ? (properties_CalendarApp_CalendarRA = new ConfigElementList<JCAGeneratedProperties>()) : properties_CalendarApp_CalendarRA;
    }

    public ConfigElementList<JCAGeneratedProperties> getProperties_CalendarRA() {
        return properties_CalendarRA == null ? (properties_CalendarRA = new ConfigElementList<JCAGeneratedProperties>()) : properties_CalendarRA;
    }

    public ConfigElementList<JCAGeneratedProperties> getProperties_FAT1s() {
        return properties_FAT1s == null ? (properties_FAT1s = new ConfigElementList<JCAGeneratedProperties>()) : properties_FAT1s;
    }

    @XmlAttribute
    public void setAlias(String alias) {
        this.alias = alias;
    }

    @XmlAttribute
    public void setAutoStart(String autoStart) {
        this.autoStart = autoStart;
    }

    @XmlAttribute
    public void setContextServiceRef(String contextServiceRef) {
        this.contextServiceRef = contextServiceRef;
    }

    @XmlAttribute
    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        if (getId() != null)
            buf.append("id=").append(getId()).append(' ');
        if (alias != null)
            buf.append("alias=").append(alias).append(' ');
        if (autoStart != null)
            buf.append("autoStart=").append(autoStart).append(' ');
        if (contextServiceRef != null)
            buf.append("contextServiceRef=").append(contextServiceRef).append(' ');
        if (location != null)
            buf.append("location=").append(location).append(' ');

        @SuppressWarnings("unchecked")
        List<ConfigElementList<?>> nestedElementsList = Arrays.asList(classloaders,
                                                                      contextServices,
                                                                      customizations,
                                                                      properties_CalendarApp_CalendarRA,
                                                                      properties_CalendarRA,
                                                                      properties_FAT1s
                        );
        for (ConfigElementList<?> nestedElements : nestedElementsList)
            if (nestedElements != null && nestedElements.size() > 0)
                for (Object o : nestedElements)
                    buf.append(o).append(' ');

        buf.append('}');
        return buf.toString();
    }
}
