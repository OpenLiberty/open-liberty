/**
L * Licensed to the Apache Software Foundation (ASF) under one
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
package org.apache.wss4j.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

public final class XMLUtils {

    public static final String XMLNS_NS = "http://www.w3.org/2000/xmlns/";
    public static final String XML_NS = "http://www.w3.org/XML/1998/namespace";
    public static final String WSU_NS =
        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(XMLUtils.class);

    private XMLUtils() {
        // complete
    }

    /**
     * Gets a direct child with specified localname and namespace. <p/>
     *
     * @param parentNode the node where to start the search
     * @param localName  local name of the child to get
     * @param namespace  the namespace of the child to get
     * @return the node or <code>null</code> if not such node found
     */
    public static Element getDirectChildElement(Node parentNode, String localName, String namespace) {
        if (parentNode == null) {
            return null;
        }
        for (Node currentChild = parentNode.getFirstChild();
            currentChild != null;
            currentChild = currentChild.getNextSibling()
        ) {
            if (Node.ELEMENT_NODE == currentChild.getNodeType()
                    && localName.equals(currentChild.getLocalName())
                    && namespace.equals(currentChild.getNamespaceURI())) {
                return (Element) currentChild;
            }
        }
        return null;
    }

    /**
     * Return the text content of an Element, or null if no such text content exists
     */
    public static String getElementText(Element e) {
        if (e != null) {
            Node node = e.getFirstChild();
            StringBuilder builder = new StringBuilder();
            boolean found = false;
            while (node != null) {
                if (Node.TEXT_NODE == node.getNodeType()) {
                    found = true;
                    builder.append(((Text)node).getData());
                } else if (Node.CDATA_SECTION_NODE == node.getNodeType()) {
                    found = true;
                    builder.append(((CDATASection)node).getData());
                }
                node = node.getNextSibling();
            }

            if (!found) {
                return null;
            }
            return builder.toString();
        }
        return null;
    }

    public static String getNamespace(String prefix, Node e) {
        while (e != null && e.getNodeType() == Node.ELEMENT_NODE) {
            Attr attr = null;
            if (prefix == null) {
                attr = ((Element) e).getAttributeNode("xmlns");
            } else {
                attr = ((Element) e).getAttributeNodeNS(XMLNS_NS, prefix);
            }
            if (attr != null) {
                return attr.getValue();
            }
            e = e.getParentNode();
        }
        return null;
    }

    public static String prettyDocumentToString(Document doc) throws IOException, TransformerException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            elementToStream(doc.getDocumentElement(), baos);
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    public static void elementToStream(Element element, OutputStream out)
        throws TransformerException {
        DOMSource source = new DOMSource(element);
        StreamResult result = new StreamResult(out);

        TransformerFactory transFactory = TransformerFactory.newInstance();
        transFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        try {
            transFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        } catch (IllegalArgumentException ex) { //NOPMD
            // ignore
        }

        Transformer transformer = transFactory.newTransformer();
        transformer.transform(source, result);
    }

    /**
     * Utility to get the bytes uri
     *
     * @param source the resource to get
     */
    public static InputSource sourceToInputSource(Source source) throws IOException, TransformerException {
        if (source instanceof SAXSource) {
            return ((SAXSource) source).getInputSource();
        } else if (source instanceof DOMSource) {
            Node node = ((DOMSource) source).getNode();
            if (node instanceof Document) {
                node = ((Document) node).getDocumentElement();
            }
            Element domElement = (Element) node;
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                elementToStream(domElement, baos);
                InputSource isource = new InputSource(source.getSystemId());
                isource.setByteStream(new ByteArrayInputStream(baos.toByteArray()));
                return isource;
            }
        } else if (source instanceof StreamSource) {
            StreamSource ss = (StreamSource) source;
            InputSource isource = new InputSource(ss.getSystemId());
            isource.setByteStream(ss.getInputStream());
            isource.setCharacterStream(ss.getReader());
            isource.setPublicId(ss.getPublicId());
            return isource;
        } else {
            return getInputSourceFromURI(source.getSystemId());
        }
    }

    /**
     * Utility to get the bytes uri.
     * Does NOT handle authenticated URLs,
     * use getInputSourceFromURI(uri, username, password)
     *
     * @param uri the resource to get
     */
    public static InputSource getInputSourceFromURI(String uri) {
        return new InputSource(uri);
    }

    /**
     * Set a namespace/prefix on an element if it is not set already. First off, it
     * searches for the element for the prefix associated with the specified
     * namespace. If the prefix isn't null, then this is returned. Otherwise, it
     * creates a new attribute using the namespace/prefix passed as parameters.
     *
     * @param element
     * @param namespace
     * @param prefix
     * @return the prefix associated with the set namespace
     */
    public static String setNamespace(Element element, String namespace, String prefix) {
        String pre = getPrefixNS(namespace, element);
        if (pre != null) {
            return pre;
        }
        element.setAttributeNS(XMLNS_NS, "xmlns:" + prefix, namespace);
        return prefix;
    }

    public static String getPrefixNS(String uri, Node e) {
        while (e != null && e.getNodeType() == Element.ELEMENT_NODE) {
            NamedNodeMap attrs = e.getAttributes();
            int length = attrs.getLength();
            for (int n = 0; n < length; n++) {
                Attr a = (Attr) attrs.item(n);
                String name = a.getName();
                if (name.startsWith("xmlns:") && a.getNodeValue().equals(uri)) {
                    return name.substring("xmlns:".length());
                }
            }
            e = e.getParentNode();
        }
        return null;
    }

    /**
     * Turn a reference (eg "#5") into an ID (eg "5").
     *
     * @param ref
     * @return ref trimmed and with the leading "#" removed, or null if not
     *         correctly formed
     */
    public static String getIDFromReference(String ref) {
        if (ref == null) {
            return null;
        }
        String id = ref.trim();
        if (id.length() == 0) {
            return null;
        }
        if (id.charAt(0) == '#') {
            id = id.substring(1);
        }
        return id;
    }

    /**
     * Returns the single element that contains an Id with value
     * <code>uri</code> and <code>namespace</code>. The Id can be either a wsu:Id or an Id
     * with no namespace. This is a replacement for a XPath Id lookup with the given namespace.
     * It's somewhat faster than XPath, and we do not deal with prefixes, just with the real
     * namespace URI
     *
     * If checkMultipleElements is true and there are multiple elements, we LOG.a
     * warning and return null as this can be used to get around the signature checking.
     *
     * @param startNode Where to start the search
     * @param value Value of the Id attribute
     * @param checkMultipleElements If true then go through the entire tree and return
     *        null if there are multiple elements with the same Id
     * @return The found element if there was exactly one match, or
     *         <code>null</code> otherwise
     */
    public static Element findElementById(
        Node startNode, String value, boolean checkMultipleElements
    ) {
        //
        // Replace the formerly recursive implementation with a depth-first-loop lookup
        //
        if (startNode == null) {
            return null;
        }
        Node startParent = startNode.getParentNode();
        Node processedNode = null;
        Element foundElement = null;
        String id = XMLUtils.getIDFromReference(value);

        while (startNode != null && id != null) {
            // start node processing at this point
            if (startNode.getNodeType() == Node.ELEMENT_NODE) {
                Element se = (Element) startNode;
                // Try the wsu:Id first
                String attributeNS = se.getAttributeNS(WSU_NS, "Id");
                if (attributeNS.length() == 0 || !id.equals(attributeNS)) {
                    attributeNS = se.getAttributeNS(null, "Id");
                }
                if (attributeNS.length() != 0 && id.equals(attributeNS)) {
                    if (!checkMultipleElements) {
                        return se;
                    } else if (foundElement == null) {
                        foundElement = se; // Continue searching to find duplicates
                    } else {
                        LOG.warn("Multiple elements with the same 'Id' attribute value!");
                        return null;
                    }
                }
            }

            processedNode = startNode;
            startNode = startNode.getFirstChild();

            // no child, this node is done.
            if (startNode == null) {
                // close node processing, get sibling
                startNode = processedNode.getNextSibling();
            }
            // no more siblings, get parent, all children
            // of parent are processed.
            while (startNode == null) {
                processedNode = processedNode.getParentNode();
                if (processedNode == startParent) {
                    return foundElement;
                }
                // close parent node processing (processed node now)
                startNode = processedNode.getNextSibling();
            }
        }
        return foundElement;
    }


    /**
     * Returns the first element that matches <code>name</code> and
     * <code>namespace</code>. <p/> This is a replacement for a XPath lookup
     * <code>//name</code> with the given namespace. It's somewhat faster than
     * XPath, and we do not deal with prefixes, just with the real namespace URI
     *
     * @param startNode Where to start the search
     * @param name Local name of the element
     * @param namespace Namespace URI of the element
     * @return The found element or <code>null</code>
     */
    public static Element findElement(Node startNode, String name, String namespace) {
        //
        // Replace the formerly recursive implementation with a depth-first-loop
        // lookup
        //
        if (startNode == null) {
            return null;
        }
        Node startParent = startNode.getParentNode();
        Node processedNode = null;

        while (startNode != null) {
            // start node processing at this point
            if (startNode.getNodeType() == Node.ELEMENT_NODE
                && startNode.getLocalName().equals(name)) {
                String ns = startNode.getNamespaceURI();
                if (ns != null && ns.equals(namespace)) {
                    return (Element)startNode;
                }

                if ((namespace == null || namespace.length() == 0)
                    && (ns == null || ns.length() == 0)) {
                    return (Element)startNode;
                }
            }
            processedNode = startNode;
            startNode = startNode.getFirstChild();

            // no child, this node is done.
            if (startNode == null) {
                // close node processing, get sibling
                startNode = processedNode.getNextSibling();
            }
            // no more siblings, get parent, all children
            // of parent are processed.
            while (startNode == null) {
                processedNode = processedNode.getParentNode();
                if (processedNode == startParent) {
                    return null;
                }
                // close parent node processing (processed node now)
                startNode = processedNode.getNextSibling();
            }
        }
        return null;
    }

    /**
     * Returns all elements that match <code>name</code> and <code>namespace</code>.
     * <p/> This is a replacement for a XPath lookup
     * <code>//name</code> with the given namespace. It's somewhat faster than
     * XPath, and we do not deal with prefixes, just with the real namespace URI
     *
     * @param startNode Where to start the search
     * @param name Local name of the element
     * @param namespace Namespace URI of the element
     * @return The found elements (or an empty list)
     */
    public static List<Element> findElements(Node startNode, String name, String namespace) {
        //
        // Replace the formerly recursive implementation with a depth-first-loop
        // lookup
        //
        if (startNode == null) {
            return null;
        }
        Node startParent = startNode.getParentNode();
        Node processedNode = null;

        List<Element> foundNodes = new ArrayList<>();
        while (startNode != null) {
            // start node processing at this point
            if (startNode.getNodeType() == Node.ELEMENT_NODE
                && startNode.getLocalName().equals(name)) {
                String ns = startNode.getNamespaceURI();
                if (ns != null && ns.equals(namespace)) {
                    foundNodes.add((Element)startNode);
                }

                if ((namespace == null || namespace.length() == 0)
                    && (ns == null || ns.length() == 0)) {
                    foundNodes.add((Element)startNode);
                }
            }
            processedNode = startNode;
            startNode = startNode.getFirstChild();

            // no child, this node is done.
            if (startNode == null) {
                // close node processing, get sibling
                startNode = processedNode.getNextSibling();
            }
            // no more siblings, get parent, all children
            // of parent are processed.
            while (startNode == null) {
                processedNode = processedNode.getParentNode();
                if (processedNode == startParent) {
                    return foundNodes;
                }
                // close parent node processing (processed node now)
                startNode = processedNode.getNextSibling();
            }
        }
        return foundNodes;
    }

    /**
     * Returns the single SAMLAssertion element that contains an AssertionID/ID that
     * matches the supplied parameter.
     *
     * @param startNode Where to start the search
     * @param value Value of the AssertionID/ID attribute
     * @return The found element if there was exactly one match, or
     *         <code>null</code> otherwise
     */
    public static Element findSAMLAssertionElementById(Node startNode, String value) {
        Element foundElement = null;

        //
        // Replace the formerly recursive implementation with a depth-first-loop
        // lookup
        //
        if (startNode == null || value == null) {
            return null;
        }
        Node startParent = startNode.getParentNode();
        Node processedNode = null;

        while (startNode != null) {
            // start node processing at this point
            if (startNode.getNodeType() == Node.ELEMENT_NODE) {
                Element se = (Element) startNode;
                if (se.hasAttributeNS(null, "ID") && value.equals(se.getAttributeNS(null, "ID"))
                    || se.hasAttributeNS(null, "AssertionID")
                        && value.equals(se.getAttributeNS(null, "AssertionID"))) {
                    if (foundElement == null) {
                        foundElement = se; // Continue searching to find duplicates
                    } else {
                        LOG.warn("Multiple elements with the same 'ID' attribute value!");
                        return null;
                    }
                }
            }

            processedNode = startNode;
            startNode = startNode.getFirstChild();

            // no child, this node is done.
            if (startNode == null) {
                // close node processing, get sibling
                startNode = processedNode.getNextSibling();
            }
            // no more siblings, get parent, all children
            // of parent are processed.
            while (startNode == null) {
                processedNode = processedNode.getParentNode();
                if (processedNode == startParent) {
                    return foundElement;
                }
                // close parent node processing (processed node now)
                startNode = processedNode.getNextSibling();
            }
        }
        return foundElement;
    }

}
