/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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

public class UpdatedFile {
    //needs to support both hash and MD5hash as attributes
    public static UpdatedFile fromNode(Node n) {

        String id = n.getAttributes().getNamedItem("id") == null ? null : n.getAttributes().getNamedItem("id").getNodeValue();
        long size = n.getAttributes().getNamedItem("size") == null ? null : Long.parseLong(n.getAttributes().getNamedItem("size").getNodeValue());
        String date = n.getAttributes().getNamedItem("date") == null ? null : n.getAttributes().getNamedItem("date").getNodeValue();
        String hash = n.getAttributes().getNamedItem("hash") == null ? n.getAttributes().getNamedItem("MD5hash") == null ? null : n.getAttributes().getNamedItem("MD5hash").getNodeValue() : n.getAttributes().getNamedItem("hash").getNodeValue();

        return new UpdatedFile(id, size, date, hash);
    }

    private final String id;

    private final long size;

    private final String hash;

    private final String date;

    public UpdatedFile(String id, long size, String date, String hash) {
        this.id = id;
        this.size = size;
        this.date = date;
        this.hash = hash;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the size
     */
    public long getSize() {
        return size;
    }

    /**
     * @return the hash
     */
    public String getHash() {
        return hash;
    }

    /**
     * @return the date
     */
    public String getDate() {
        return date;
    }

}
