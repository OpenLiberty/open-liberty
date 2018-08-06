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

import javax.xml.bind.annotation.XmlAttribute;

import org.w3c.dom.NodeList;

public class Property {
    public static List<Property> fromNodeList(NodeList nl) {
        List<Property> props = new ArrayList<Property>();
        for (int i = 0; i < nl.getLength(); i++) { //Property Elements
            props.add(new Property(nl.item(i).getAttributes().getNamedItem("name").getNodeValue(), nl.item(i).getAttributes().getNamedItem("value").getNodeValue()));
        }
        return props;
    }

    @XmlAttribute
    private String value;
    @XmlAttribute
    private String name;

    public Property() {
        //blank constructor required by jaxb
    }

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
