/*******************************************************************************
 * Copyright (c) 2017, 2026 IBM Corporation and others.
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

import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Represents an explicitly installed application
 *
 * @author Tim Burns
 *
 */
public class ClassloaderElement extends ConfigElement {

    @XmlAttribute(name = "commonLibraryRef")
    private Set<String> commonLibraryRefs;

    @XmlAttribute(name = "privateLibraryRef")
    private Set<String> privateLibraryRefs;

    private String apiTypeVisibility;

    private String delegation;

    public Set<String> getCommonLibraryRefs() {
        if (this.commonLibraryRefs == null) {
            this.commonLibraryRefs = new TreeSet<String>();
        }
        return this.commonLibraryRefs;
    }

    public Set<String> getPrivateLibraryRefs() {
        if (this.privateLibraryRefs == null) {
            this.privateLibraryRefs = new TreeSet<String>();
        }
        return this.privateLibraryRefs;
    }

    @Override
    public ClassloaderElement clone() throws CloneNotSupportedException {
        ClassloaderElement clone = (ClassloaderElement) super.clone();

        if (this.commonLibraryRefs != null) {
            clone.commonLibraryRefs = new TreeSet<String>();
            for (String ref : this.commonLibraryRefs)
                clone.commonLibraryRefs.add(ref);
        }

        if (this.privateLibraryRefs != null) {
            clone.privateLibraryRefs = new TreeSet<String>();
            for (String ref : this.privateLibraryRefs)
                clone.privateLibraryRefs.add(ref);
        }

        return clone;
    }

    @XmlAttribute(name = "apiTypeVisibility")
    public void setApiTypeVisibility(String apiTypes) {
        this.apiTypeVisibility = apiTypes;
    }

    public String getApiTypeVisibility() {
        return this.apiTypeVisibility;
    }

    @XmlAttribute(name = "delegation")
    public void setDelegation(String delegation) {
        this.delegation = delegation;
    }

    public String getDelegation() {
        return this.delegation;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("ClassLoaderElement{");
        if (commonLibraryRefs != null)
            for (String ref : this.commonLibraryRefs)
                buf.append("commonLibraryRefs=\"" + ref + "\" ");
        if (privateLibraryRefs != null)
            for (String ref : this.privateLibraryRefs)
                buf.append("libraryRefs=\"" + ref + "\" ");
        buf.append("apiTypeVisibility=\"" + (apiTypeVisibility == null ? "" : apiTypeVisibility) + "\" ");
        if (delegation != null)
            buf.append("delegation=\"" + delegation + "\" ");
        buf.append("}");

        return buf.toString();
    }
}
