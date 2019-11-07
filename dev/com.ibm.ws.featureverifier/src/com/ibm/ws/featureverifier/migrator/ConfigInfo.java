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
package com.ibm.ws.featureverifier.migrator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class ConfigInfo {
    Set<String> features = new HashSet<String>();
    Map<String, String> singletonBindings = new HashMap<String, String>();

    static ConfigInfo fromFile(File xml) throws Exception {
        ConfigInfo retval = new ConfigInfo();
        Document serverDoc = null;
        FileInputStream serverStream = null;
        try {
            serverStream = new FileInputStream(xml);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            serverDoc = db.parse(serverStream);
        } finally {
            try {
                if (serverStream != null)
                    serverStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Node ignoreElt = serverDoc.getElementsByTagName("featureManager").item(0);
        NodeList nl = ignoreElt.getChildNodes();
        int index = 0;
        for (index = 0; index < nl.getLength(); index++) {
            Node n = nl.item(index);
            String content = n.getTextContent();
            if (content.contains(" bound to ")) {
                String key = content.substring(0, content.indexOf(" bound to ")).trim();
                String value = content.substring(content.indexOf(" bound to ") + " bound to ".length()).trim();
                if (value.startsWith("##")) {
                    value.substring(2);
                }
                retval.singletonBindings.put(key, value);
            }
            if (n.getNodeName().equals("feature")) {
                String feature = n.getTextContent();
                if (!feature.startsWith("protected.85") && !feature.equals("featureverifier-1.0")) {
                    retval.features.add(n.getTextContent());
                }
            }
            if (content.contains(" will be supplied by")) {
                String supplied = content.substring(0, content.indexOf(" will be supplied by")).trim();
                supplied = supplied.substring("protected feature ".length());
                retval.features.add(supplied);
            }
        }
        return retval;
    }
}