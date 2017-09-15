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
package com.ibm.ws.product.utility.extension.ifix.xml;

import java.util.Set;

import javax.xml.bind.annotation.XmlElement;

public class FeatureManifests {
    @XmlElement(name = "manifest")
    private Set<FeatureManifestFile> files;

    public FeatureManifests() {
        //no-op
        //default constructor required by jaxb
    }

    public FeatureManifests(Set<FeatureManifestFile> files) {
        this.files = files;
    }

    /**
     * @return the list of manifest files
     */
    public Set<FeatureManifestFile> getManifests() {
        return this.files;
    }

}
