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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Property {
    public static List<Property> fromNodeList(NodeList nl) {
        List<Property> props = new ArrayList<Property>();
        for (int i = 0; i < nl.getLength(); i++) { //Property Elements
            Node n = nl.item(i);
            String name = n.getAttributes().getNamedItem("name") == null ? null : n.getAttributes().getNamedItem("name").getNodeValue();
            String value = n.getAttributes().getNamedItem("value") == null ? null : n.getAttributes().getNamedItem("value").getNodeValue();
            props.add(new Property(name, value));
        }
        return props;
    }

    private final String value;

    private final String name;

    public Property(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
