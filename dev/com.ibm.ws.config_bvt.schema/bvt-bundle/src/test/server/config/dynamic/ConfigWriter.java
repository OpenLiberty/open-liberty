/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.config.dynamic;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class ConfigWriter {

    private static DocumentBuilderFactory DOM_FACTORY = DocumentBuilderFactory.newInstance();

    private final Document document;

    public ConfigWriter() throws Exception {
        DocumentBuilder builder = DOM_FACTORY.newDocumentBuilder();
        document = builder.newDocument();
        document.appendChild(document.createElement("server"));
    }

    public ConfigWriter(InputStream in) throws Exception {
        DocumentBuilder builder = DOM_FACTORY.newDocumentBuilder();
        document = builder.parse(in);
    }

    private static boolean matches(Node node, String nodeName, String id, String idAttribute) {
        if (node instanceof Element && nodeName.equals(node.getNodeName())) {
            if (id == null) {
                return true;
            } else {
                Element element = (Element) node;
                return id.equals(element.getAttribute(idAttribute));
            }
        }
        return false;
    }

    private Element getConfigNode(String nodeName, String id, String idAttribute) {
        Element root = document.getDocumentElement();
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (matches(child, nodeName, id, idAttribute)) {
                return (Element) child;
            }
        }
        return null;
    }

    private List<Element> getConfigNodes(String nodeName, String id, String idAttribute) {
        Element root = document.getDocumentElement();
        NodeList children = root.getChildNodes();
        List<Element> nodes = new ArrayList<Element>();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (matches(child, nodeName, id, idAttribute)) {
                nodes.add((Element) child);
            }
        }
        return nodes;
    }

    public boolean deleteConfig(String nodeName, String id, boolean allInstances) {
        return deleteConfig(nodeName, id, "id", allInstances);
    }

    public boolean deleteConfig(String nodeName, String id, String idAttribute, boolean allInstances) {
        boolean didSomething = false;
        Element root = document.getDocumentElement();
        if (allInstances) {
            List<Element> toRemove = getConfigNodes(nodeName, id, idAttribute);
            for (Node node : toRemove) {
                root.removeChild(node);
                didSomething = true;
            }
        } else {
            Node node = getConfigNode(nodeName, id, idAttribute);
            if (node != null) {
                root.removeChild(node);
                didSomething = true;
            }
        }
        return didSomething;
    }

    public void addConfig(String element) throws Exception {
        DocumentBuilder builder = DOM_FACTORY.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(element)));
        addConfig(doc.getDocumentElement());
    }

    public void addConfig(String pattern, Object... args) throws Exception {
        addConfig(MessageFormat.format(pattern, args));
    }

    public void addConfig(Element element) {
        Node node = document.importNode(element, true);
        Element root = document.getDocumentElement();
        root.appendChild(node);
    }

    public boolean setValue(String nodeName, String id, String propertyName, String value) {
        return setValue(nodeName, id, "id", propertyName, value);
    }

    public boolean setValue(String nodeName, String id, String idAttribute, String propertyName, String value) {
        Element element = getConfigNode(nodeName, id, idAttribute);
        if (element != null) {
            element.setAttribute(propertyName, value);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeProperty(String nodeName, String id, String propertyName) {
        return removeProperty(nodeName, id, "id", propertyName);
    }

    public boolean removeProperty(String nodeName, String id, String idAttribute, String propertyName) {
        Element element = getConfigNode(nodeName, id, idAttribute);
        if (element != null) {
            // TODO: check for subelement with this name
            return (element.getAttributes().removeNamedItem(propertyName) != null);
        } else {
            return false;
        }
    }

    public void write(File file) throws Exception {
        write(new StreamResult(file));
    }

    public void write(OutputStream out) throws Exception {
        write(new StreamResult(out));
    }

    public void write(Writer writer) throws Exception {
        write(new StreamResult(writer));
    }

    private void write(Result result) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        //transformer.setOutputProperty (OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        transformer.transform(new DOMSource(document), result);
    }
}
