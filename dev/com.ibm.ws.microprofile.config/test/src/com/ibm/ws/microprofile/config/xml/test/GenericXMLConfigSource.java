/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.xml.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

public class GenericXMLConfigSource extends HashMapConfigSource implements ConfigSource {

    public GenericXMLConfigSource(ConcurrentMap<String, String> properties, int ordinal, String id) {
        super(properties, ordinal, id);
    }

    public GenericXMLConfigSource(URL resource, int ordinal, String id) {
        this(loadProperties(resource), ordinal, id);
    }

    public GenericXMLConfigSource(String resourceName, ClassLoader classLoader, int ordinal) {
        this(classLoader.getResource(resourceName), ordinal, "XML File Config Source: " + resourceName);
    }

    public static ConcurrentMap<String, String> loadProperties(URL resource) {

        ConcurrentMap<String, String> props = new ConcurrentHashMap<>();

        if (resource != null) {
            InputStream inputStream = null;

            try {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                inputStream = resource.openStream();
                Document document = builder.parse(inputStream);
                DOMImplementationLS ls = (DOMImplementationLS) document.getImplementation().getFeature("LS", "3.0");
                LSSerializer serializer = ls.createLSSerializer();
                serializer.getDomConfig().setParameter("xml-declaration", false);

                Node root = document.getDocumentElement();
                NodeList nodes = root.getChildNodes();
                int len = nodes.getLength();
                int idx = 0;
                for (int i = 0; i < len; i++) {
                    Node node = nodes.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element ele = (Element) node;
                        String key = ele.getNodeName() + "_" + idx++;
                        String value = serializer.writeToString(ele);
                        props.put(key, value);
                    }
                }

            } catch (ParserConfigurationException | SAXException | IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        return props;
    }
}
