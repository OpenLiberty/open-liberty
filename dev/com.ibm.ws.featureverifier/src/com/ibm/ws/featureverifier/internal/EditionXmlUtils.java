/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.featureverifier.internal;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class EditionXmlUtils {
    public static Set<String> obtainFeatureNamesFromXml(File xml) throws SAXException, IOException, ParserConfigurationException {
        Set<String> result = new HashSet<String>();

        //if file exists, it'd better parse ok, else we'll just exception out of here.
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document d = db.parse(xml);

        NodeList includes = d.getElementsByTagName("include");
        if (includes != null) {
            int index = 0;
            for (index = 0; index < includes.getLength(); index++) {
                Node include = includes.item(index);
                if (include != null) {
                    NamedNodeMap fattrs = include.getAttributes();
                    String location = fattrs.getNamedItem("location").getTextContent();

                    File includedFile;
                    // If the include location is a variable, then use a regex to
                    // map the expected path structure. The path structure is laid
                    // out in /build.featureverify/build-featureverify.xml targets.
                    if (location.startsWith("${editions")) {
                        location = location.replaceFirst("\\$\\{editions.(.*)\\}(.*)", "../../editions/$1$2");
                        includedFile = new File(xml.getParentFile(), location);
                    } else if (location.startsWith("${profiles")) {
                        location = location.replaceFirst("\\$\\{profiles.(.*)\\}(.*)", "../../profiles/$1$2");
                        includedFile = new File(xml.getParentFile(), location);
                    } else {
                        // Its not a variable, so let's assume its a real file
                        includedFile = new File(xml.getParentFile(), location);
                    }

                    Set<String> included = obtainFeatureNamesFromXml(includedFile);
                    System.out.println(" - include : " + fattrs.getNamedItem("location").getTextContent() + " added " + included);
                    result.addAll(included);
                }
            }
        }

        NodeList features = d.getElementsByTagName("feature");
        if (features != null) {
            int index = 0;
            Set<String> names = new HashSet<String>();
            for (index = 0; index < features.getLength(); index++) {
                Node feature = features.item(index);
                if (feature != null) {
                    String name = feature.getTextContent();
                    names.add(name);
                }
            }
            result.addAll(names);
            System.out.println(" - " + xml.getAbsolutePath() + " added " + names);
        }

        return result;
    }
}
