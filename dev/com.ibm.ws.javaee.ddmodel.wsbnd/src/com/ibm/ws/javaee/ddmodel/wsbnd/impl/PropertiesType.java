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
package com.ibm.ws.javaee.ddmodel.wsbnd.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.StringType;
import com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.wsbnd.Properties;

/*
 <xsd:complexType name="propertiesType">
 <xsd:anyAttribute namespace="##local" processContents="skip"/>
 </xsd:complexType>
 */
public class PropertiesType extends DDParser.ElementContentParsable implements Properties {
    Map<String, String> attributes;

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> map = null != attributes ? new HashMap<String, String>() : null;

        if (null != map) {
            map.putAll(attributes);
        }
        return map;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        return false;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {

        if (nsURI != null) {
            return false;
        }

        StringType valueType = parser.parseStringAttributeValue(index);
        if (null == attributes) {
            attributes = new HashMap<String, String>();
        }

        attributes.put(localName, valueType.getValue());

        return true;
    }

    @Override
    public void describe(Diagnostics diag) {
        if (null != attributes) {
            String prefix = "";
            for (Entry<String, String> entry : attributes.entrySet()) {
                diag.append(prefix);
                diag.append(entry.getKey());
                diag.append("=");
                diag.append(entry.getValue());

                prefix = ",";
            }
        }
    }
}
