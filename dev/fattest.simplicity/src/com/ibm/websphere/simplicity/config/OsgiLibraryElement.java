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

import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Represents an shared library for OSGi applications
 * 
 */
public class OsgiLibraryElement extends ConfigElement {

    @XmlAttribute(name = "libraryRef")
    private Set<String> libraryRefs;

    @XmlElement(name = "package")
    private Set<String> packages;

    public Set<String> getLibraryRefs() {
        if (this.libraryRefs == null) {
            this.libraryRefs = new TreeSet<String>();
        }
        return this.libraryRefs;
    }

    public Set<String> getPackages() {
        if (this.packages == null) {
            this.packages = new TreeSet<String>();
        }
        return this.packages;
    }

    @Override
    public OsgiLibraryElement clone() throws CloneNotSupportedException {
        OsgiLibraryElement clone = (OsgiLibraryElement) super.clone();

        if (this.libraryRefs != null) {
            clone.libraryRefs = new TreeSet<String>();
            for (String ref : this.libraryRefs)
                clone.libraryRefs.add(ref);
        }

        return clone;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("OsgiLibraryElement{");
        if (libraryRefs != null)
            for (String ref : this.libraryRefs)
                buf.append("libraryRefs=\"" + ref + "\" ");
        if (packages != null)
            for (String pkg : this.packages)
                buf.append("package=\"" + pkg + "\" ");
        buf.append("}");

        return buf.toString();
    }
}
