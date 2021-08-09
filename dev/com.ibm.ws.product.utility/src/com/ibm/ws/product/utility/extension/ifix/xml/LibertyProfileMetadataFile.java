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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class LibertyProfileMetadataFile implements MetadataOutput {

    public static LibertyProfileMetadataFile fromDocument(Document doc) {
        if (doc == null)
            return null;
        Element e = doc.getDocumentElement();
        e.normalize();
        if (!"libertyFixMetadata".equals(e.getNodeName()))
            return null;
        return new LibertyProfileMetadataFile(Bundles.fromNodeList(e.getElementsByTagName("bundles")), FeatureManifests.fromNodeList(e.getElementsByTagName("newFeatureManifests")));
    }

    private final Bundles bundles;

    private final FeatureManifests manifests;

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
