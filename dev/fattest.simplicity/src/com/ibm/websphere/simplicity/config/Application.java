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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Represents an explicitly installed application
 *
 * @author Tim Burns
 *
 */
public class Application extends ConfigElement {

    private String name;
    private String type;
    private String location;
    @XmlElement(name = "classloader")
    private ConfigElementList<ClassloaderElement> classloaders;
    @XmlElement(name = "application-bnd")
    private ApplicationBnd applicationBnd;
    @XmlElement(name = "resourceAdapter")
    private ConfigElementList<ResourceAdapter> resourceAdapters;

    //@XmlElement(name = "library")
    //private List<Library> libraries;

    /**
     * @return the name of the application (defines context root for wars)
     */
    public String getName() {
        return this.name;
    }

    /**
     * @param name the name of the application (defines context root for wars)
     */
    @XmlAttribute
    public void setName(String name) {
        this.name = ConfigElement.getValue(name);
    }

    /**
     * @return the type of the archive (ear/war/etc)
     */
    public String getType() {
        return this.type;
    }

    /**
     * @param type the type of the archive (ear/war/etc)
     */
    @XmlAttribute
    public void setType(String type) {
        this.type = ConfigElement.getValue(type);
    }

    /**
     * @return the location where the archive is located (may be remote from the local JVM)
     */
    public String getLocation() {
        return this.location;
    }

    /**
     * @param location the location where the archive is located (may be remote from the local JVM)
     */
    @XmlAttribute
    public void setLocation(String location) {
        this.location = ConfigElement.getValue(location);
    }

    /**
     * @deprecated do not use for new code. Use getClassloaders instead. This method exists only for legacy purposes. It does not follow proper conventions for simplicity config.
     * @return gets the first configured class loader if one exists, otherwise creates a new ClassloaderElement as the first configured classloader.
     */
    @Deprecated
    public ClassloaderElement getClassloader() {
        if (this.classloaders == null)
            this.classloaders = new ConfigElementList<ClassloaderElement>();
        if (this.classloaders.isEmpty())
            this.classloaders.add(new ClassloaderElement());
        return this.classloaders.get(0);
    }

    /**
     * @return gets all configured class loaders
     */
    public ConfigElementList<ClassloaderElement> getClassloaders() {
        if (classloaders == null) {
            classloaders = new ConfigElementList<ClassloaderElement>();
        }
        return classloaders;
    }

    /**
     * @return get application binding information
     */
    public ApplicationBnd getApplicationBnd() {
        if (this.applicationBnd == null) {
            this.applicationBnd = new ApplicationBnd();
        }
        return this.applicationBnd;
    }

    /**
     * @return configuration for resource adapters that are embedded in the application
     */
    public ConfigElementList<ResourceAdapter> getResourceAdapters() {
        if (resourceAdapters == null)
            resourceAdapters = new ConfigElementList<ResourceAdapter>();
        return resourceAdapters;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("Application{");
        if (name != null)
            buf.append("name=\"" + name + "\" ");
        if (type != null)
            buf.append("type=\"" + type + "\" ");
        if (location != null)
            buf.append("location=\"" + location + "\" ");
        if (classloaders != null)
            buf.append(classloaders);
        if (applicationBnd != null)
            buf.append(applicationBnd.toString());
        if (resourceAdapters != null)
            buf.append(resourceAdapters);
        buf.append("}");

        return buf.toString();
    }

    @Override
    public Application clone() throws CloneNotSupportedException {
        Application clone = (Application) super.clone();
        if (this.classloaders != null)
            clone.classloaders = this.classloaders.clone();
        if (this.applicationBnd != null)
            clone.applicationBnd = this.applicationBnd.clone();
        if (this.resourceAdapters != null)
            clone.resourceAdapters = this.resourceAdapters.clone();

        return clone;
    }

}
