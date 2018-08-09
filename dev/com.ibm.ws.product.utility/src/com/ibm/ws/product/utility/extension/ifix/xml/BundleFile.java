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

import org.w3c.dom.Node;

public class BundleFile extends UpdatedFile {

    private final String symbolicName;

    private final String version;

    private final boolean isBaseBundle;

    public static BundleFile fromNode(Node n) {
        String id = n.getAttributes().getNamedItem("id") == null ? null : n.getAttributes().getNamedItem("id").getNodeValue();
        long size = n.getAttributes().getNamedItem("size") == null ? null : Long.parseLong(n.getAttributes().getNamedItem("size").getNodeValue());
        String date = n.getAttributes().getNamedItem("date") == null ? null : n.getAttributes().getNamedItem("date").getNodeValue();
        String hash = n.getAttributes().getNamedItem("hash") == null ? n.getAttributes().getNamedItem("MD5hash") == null ? null : n.getAttributes().getNamedItem("MD5hash").getNodeValue() : n.getAttributes().getNamedItem("hash").getNodeValue();

        return new BundleFile(id, size, date, hash, n.getAttributes().getNamedItem("symbolicName").getNodeValue(), n.getAttributes().getNamedItem("version").getNodeValue(), Boolean.parseBoolean(n.getAttributes().getNamedItem("isBaseBundle").getNodeValue()));
    }

    public BundleFile(String id, long size, String date, String hash, String symbolicName, String version, boolean isBaseBundle) {
        super(id, size, date, hash);
        this.symbolicName = symbolicName;
        this.version = version;
        this.isBaseBundle = isBaseBundle;
    }

    public String getSymbolicName() {
        return this.symbolicName;
    }

    public String getVersion() {
        return this.version;
    }

    public boolean isBaseBundle() {
        return this.isBaseBundle;
    }
}
