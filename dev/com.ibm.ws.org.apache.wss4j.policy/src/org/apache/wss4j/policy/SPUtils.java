/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.wss4j.policy;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public final class SPUtils {

    private SPUtils() {
    }

    public static boolean hasChildElements(Element element) {
        Node firstChild = element.getFirstChild();
        while (firstChild != null) {
            if (firstChild.getNodeType() == Node.ELEMENT_NODE) {
                return true;
            }
            firstChild = firstChild.getNextSibling();
        }
        return false;
    }

    public static Element getFirstPolicyChildElement(Element element) {
        Element policy = getFirstChildElement(element, SPConstants.P_LOCALNAME);
        if (policy != null && org.apache.neethi.Constants.isPolicyNS(policy.getNamespaceURI())) {
            return policy;
        }
        return null;
    }

    public static boolean hasChildElementWithName(Element element, QName elementName) {
        Element child = SPUtils.getFirstChildElement(element, elementName);
        if (child != null) {
            return true;
        }
        return false;
    }

    public static Element getFirstChildElement(Node parent, String childNodeName) {
        Node node = parent.getFirstChild();
        while (node != null && (Node.ELEMENT_NODE != node.getNodeType()
                || !node.getLocalName().equals(childNodeName))) {
            node = node.getNextSibling();
        }
        return (Element) node;
    }

    public static Element getFirstChildElement(Node parent, QName childNodeName) {
        Node node = parent.getFirstChild();
        while (node != null && (Node.ELEMENT_NODE != node.getNodeType()
                || !isNodeEqualToQName(node, childNodeName))) {
            node = node.getNextSibling();
        }
        return (Element) node;
    }

    private static boolean isNodeEqualToQName(Node node, QName nodeName) {
        if ((node.getNamespaceURI() == null && nodeName.getNamespaceURI() == null
            || node.getNamespaceURI() != null
                && node.getNamespaceURI().equals(nodeName.getNamespaceURI()))
            && node.getLocalName().equals(nodeName.getLocalPart())) {
            return true;
        }
        return false;
    }

    public static String getFirstChildElementText(Node parent, QName childNodeName) {
        Element element = getFirstChildElement(parent, childNodeName);
        return element != null ? element.getTextContent() : null;
    }

    public static Element getFirstChildElement(Node parent) {
        Node node = parent.getFirstChild();
        while (node != null && Node.ELEMENT_NODE != node.getNodeType()) {
            node = node.getNextSibling();
        }
        return (Element) node;
    }

    public static Element getNextSiblingElement(Node node) {
        Node n = node.getNextSibling();
        while (n != null && Node.ELEMENT_NODE != n.getNodeType()) {
            n = n.getNextSibling();
        }
        return (Element) n;
    }

    public static boolean isOptional(Element element) {
        Attr attr = findOptionalAttribute(element);
        if (attr != null) {
            String v = attr.getValue();
            return "true".equalsIgnoreCase(v) || "1".equals(v);
        }
        return false;
    }

    public static Attr findOptionalAttribute(Element element) {
        NamedNodeMap attributes = element.getAttributes();
        for (int x = 0; x < attributes.getLength(); x++) {
            Attr attr = (Attr) attributes.item(x);
            QName qName = new QName(attr.getNamespaceURI(), attr.getLocalName());
            if (org.apache.neethi.Constants.isOptionalAttribute(qName)) {
                return attr;
            }
        }
        return null;
    }

    public static boolean isIgnorable(Element element) throws IllegalArgumentException {
        Attr attr = findIgnorableAttribute(element);
        if (attr != null) {
            String value = attr.getValue();
            if ("true".equalsIgnoreCase(value) || "1".equals(value)) {
                if (SP13Constants.SP_NS.equals(element.getNamespaceURI())) {
                    throw new IllegalArgumentException("Ignorable attribute not allowed.");
                }
                return true;
            }
        }
        return false;
    }

    public static Attr findIgnorableAttribute(Element element) {
        NamedNodeMap attributes = element.getAttributes();
        for (int x = 0; x < attributes.getLength(); x++) {
            Attr attr = (Attr) attributes.item(x);
            QName qName = new QName(attr.getNamespaceURI(), attr.getLocalName());
            if (org.apache.neethi.Constants.isIgnorableAttribute(qName)) {
                return attr;
            }
        }
        return null;
    }

    public static String getAttribute(Element element, QName attName) {
        Attr attr;
        if (attName.getNamespaceURI() == null || attName.getNamespaceURI().length() == 0) {
            attr = element.getAttributeNode(attName.getLocalPart());
        } else {
            attr = element.getAttributeNodeNS(attName.getNamespaceURI(), attName.getLocalPart());
        }
        return attr == null ? null : attr.getValue().trim();
    }

    public static QName getElementQName(Element element) {
        return new QName(element.getNamespaceURI(), element.getLocalName(), element.getPrefix());
    }

    public static void serialize(Node node, XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
        if (node.getNodeType() == Node.DOCUMENT_NODE) {
            Document document = (Document) node;
            serialize(document.getDocumentElement(), xmlStreamWriter);
        }
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            xmlStreamWriter.writeStartElement(element.getPrefix(), element.getLocalName(), element.getNamespaceURI());
            NamedNodeMap namedNodeMap = element.getAttributes();
            for (int i = 0; i < namedNodeMap.getLength(); i++) {
                Attr attr = (Attr) namedNodeMap.item(i);
                String prefix = attr.getPrefix();
                if (prefix != null && "xmlns".equals(prefix)) {
                    xmlStreamWriter.writeNamespace(attr.getLocalName(), attr.getValue());
                } else if (prefix == null && "xmlns".equals(attr.getLocalName())) {
                    xmlStreamWriter.writeDefaultNamespace(attr.getValue());
                } else {
                    xmlStreamWriter.writeAttribute(prefix,
                                                   attr.getNamespaceURI(),
                                                   attr.getLocalName(),
                                                   attr.getValue());
                }
            }
            //write ns after processing element namespaces to prevent redeclarations
            if (element.getPrefix() != null) {
                String ns = xmlStreamWriter.getNamespaceContext().getNamespaceURI(element.getPrefix());
                if (ns == null) {
                    xmlStreamWriter.writeNamespace(element.getPrefix(), element.getNamespaceURI());
                }
            }
            Node firstChild = element.getFirstChild();
            while (firstChild != null) {
                serialize(firstChild, xmlStreamWriter);
                firstChild = firstChild.getNextSibling();
            }
            xmlStreamWriter.writeEndElement();
        } else if (node.getNodeType() == Node.TEXT_NODE) {
            Text text = (Text) node;
            xmlStreamWriter.writeCharacters(text.getData());
        }
    }
}
