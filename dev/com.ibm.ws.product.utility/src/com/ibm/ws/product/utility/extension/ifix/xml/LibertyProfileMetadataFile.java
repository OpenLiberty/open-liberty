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

import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "libertyFixMetadata")
public class LibertyProfileMetadataFile implements MetadataOutput {

    @XmlElement(name = "bundles")
    private Bundles bundles;

    @XmlElement(name = "newFeatureManifests")
    private FeatureManifests manifests;

    public LibertyProfileMetadataFile() {
        //no-op
        //default constructor required by jaxb
    }

    public LibertyProfileMetadataFile(List<BundleFile> bundleFiles, Set<FeatureManifestFile> manifestFiles) {
        this.bundles = new Bundles(bundleFiles);
        this.manifests = new FeatureManifests(manifestFiles);
    }

    public Bundles getBundles() {
        return this.bundles;
    }

    public FeatureManifests getManifests() {
        return this.manifests;
    }
}
