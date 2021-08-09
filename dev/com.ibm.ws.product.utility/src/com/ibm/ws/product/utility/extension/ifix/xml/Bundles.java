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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.NodeList;

public class Bundles {

    private final List<BundleFile> files;

    public static List<BundleFile> fromNodeList(NodeList nl) {
        List<BundleFile> bundles = new ArrayList<BundleFile>();

        for (int i = 0; i < nl.getLength(); i++) //bundles Elements
            for (int j = 0; j < nl.item(i).getChildNodes().getLength(); j++) //bundle Elements
                if (nl.item(i).getChildNodes().item(j).getNodeName().equals("bundle"))
                    bundles.add(BundleFile.fromNode(nl.item(i).getChildNodes().item(j)));
        return bundles;
    }

    public Bundles(List<BundleFile> files) {
        this.files = files;
    }

    public List<BundleFile> getBundleFiles() {
        return this.files;
    }
}
