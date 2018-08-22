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

import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.NodeList;

public class FeatureManifests {
    private final Set<FeatureManifestFile> files;

    public static Set<FeatureManifestFile> fromNodeList(NodeList nl) {
        Set<FeatureManifestFile> manifests = new HashSet<FeatureManifestFile>();

        for (int i = 0; i < nl.getLength(); i++) //newFeatureManifests Elements
            for (int j = 0; j < nl.item(i).getChildNodes().getLength(); j++) //manifest Elements
                if (nl.item(i).getChildNodes().item(j).getNodeName().equals("manifest"))
                    manifests.add(FeatureManifestFile.fromNode(nl.item(i).getChildNodes().item(j)));
        return manifests;
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
