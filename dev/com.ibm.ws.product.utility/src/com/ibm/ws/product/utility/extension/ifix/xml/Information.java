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
import org.w3c.dom.NodeList;

public class Information {
    public static Information fromNodeList(NodeList nl) {
        //Only return the first information tag we find
        if (nl.getLength() > 0) {
            Node n = nl.item(0);
            String name = n.getAttributes().getNamedItem("name") == null ? null : n.getAttributes().getNamedItem("name").getNodeValue();
            String version = n.getAttributes().getNamedItem("version") == null ? null : n.getAttributes().getNamedItem("version").getNodeValue();
            return new Information(name, version, n.getTextContent());
        }
        return null;
    }

    private String content;

    private String version;

    private String name;

    public Information() {
        //required blank constructor
    }

    public Information(String name, String version, String content) {
        this.name = name;
        this.version = version;
        this.content = content;
    }
}
