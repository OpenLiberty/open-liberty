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
package org.apache.xml.security.stax.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.ext.OutputProcessorChain;
import org.apache.xml.security.stax.ext.SecurePart;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.stax.ext.stax.XMLSecAttribute;
import org.apache.xml.security.stax.ext.stax.XMLSecEvent;
import org.apache.xml.security.stax.ext.stax.XMLSecEventFactory;
import org.apache.xml.security.stax.ext.stax.XMLSecNamespace;

/**
 * Custom XMLStreamWriter to map XMLStreamWriter method calls into XMLEvent's
 *
 */
public class XMLSecurityStreamWriter implements XMLStreamWriter, AutoCloseable {

    private final OutputProcessorChain outputProcessorChain;
    private Element elementStack;
    private Element openStartElement;
    private NSContext namespaceContext = new NSContext(null);
    private boolean endDocumentWritten = false;
    private boolean haveToWriteEndElement = false;
    private SecurePart signEntireRequestPart;
    private SecurePart encryptEntireRequestPart;

    public XMLSecurityStreamWriter(OutputProcessorChain outputProcessorChain) {
        this.outputProcessorChain = outputProcessorChain;
    }

    private void chainProcessEvent(XMLSecEvent xmlSecEvent) throws XMLStreamException {
        try {
            outputProcessorChain.reset();
            outputProcessorChain.processEvent(xmlSecEvent);
        } catch (XMLSecurityException e) {
            throw new XMLStreamException(e);
        } catch (XMLStreamException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("Trying to declare prefix xmlns (illegal as per NS 1.1 #4)")) {
                throw new XMLStreamException("If you hit this exception this most probably means" +
                        "you are using the javax.xml.transform.stax.StAXResult. Don't use " +
                        "it. It is buggy as hell.", e);
            }
            //NB1: net.java.dev.stax-utils also doesn work: [Fatal Error]
            // :4:425: Attribute "xmlns" was already specified for element ...
            //NB2: The spring version also doesn't work...
            //it seems it is not trivial to write a StAXResult because I couldn't find an implementation
            // which passes the testcases...hmm
            throw e;
        }
    }

    private void outputOpenStartElement() throws XMLStreamException {
        if (openStartElement != null) {
            chainProcessEvent(
                    XMLSecEventFactory.createXmlSecStartElement(
                            openStartElement.getQName(),
                            openStartElement.getAttributes(),
                            openStartElement.getNamespaces()));
            openStartElement = null;
        }
        if (haveToWriteEndElement) {
            haveToWriteEndElement = false;
            writeEndElement();
        }
    }

    private String getNamespacePrefix(String namespaceURI) {
        if (elementStack == null) {
            return namespaceContext.getPrefix(namespaceURI);
        } else {
            return elementStack.getNamespaceContext().getPrefix(namespaceURI);
        }
    }

    @Override
    public void writeStartElement(String localName) throws XMLStreamException {
        writeStartElement(XMLConstants.DEFAULT_NS_PREFIX, localName, XMLConstants.NULL_NS_URI);
    }

    @Override
    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        writeStartElement(getNamespacePrefix(namespaceURI), localName, namespaceURI);
    }

    @Override
    public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        outputOpenStartElement();

        Element element;
        if (elementStack == null) {
            element = new Element(elementStack, namespaceContext, namespaceURI, localName, prefix);
            if (signEntireRequestPart != null) {
                signEntireRequestPart.setName(new QName(namespaceURI, localName, prefix));
                outputProcessorChain.getSecurityContext().putAsMap(
                        XMLSecurityConstants.SIGNATURE_PARTS,
                        signEntireRequestPart.getName(),
                        signEntireRequestPart
                );
            }
            if (encryptEntireRequestPart != null) {
                encryptEntireRequestPart.setName(new QName(namespaceURI, localName, prefix));
                outputProcessorChain.getSecurityContext().putAsMap(
                        XMLSecurityConstants.ENCRYPTION_PARTS,
                        encryptEntireRequestPart.getName(),
                        encryptEntireRequestPart
                );
            }
        } else {
            element = new Element(elementStack, namespaceURI, localName, prefix);
        }

        elementStack = element;
        openStartElement = element;
    }

    @Override
    public void writeEmptyElement(String localName) throws XMLStreamException {
        writeEmptyElement(XMLConstants.DEFAULT_NS_PREFIX, localName, XMLConstants.NULL_NS_URI);
    }

    @Override
    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        writeEmptyElement(getNamespacePrefix(namespaceURI), localName, namespaceURI);
    }

    @Override
    public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        writeStartElement(prefix, localName, namespaceURI);
        openStartElement.setEmptyElement(true);
        haveToWriteEndElement = true;
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        outputOpenStartElement();
        Element element = this.elementStack;
        this.elementStack = this.elementStack.getParentElement();
        chainProcessEvent(XMLSecEventFactory.createXmlSecEndElement(element.getQName()));

    }

    @Override
    public void writeEndDocument() throws XMLStreamException {
        if (!endDocumentWritten) {
            outputOpenStartElement();
            while (this.elementStack != null) {
                Element element = this.elementStack;
                this.elementStack = element.getParentElement();
                chainProcessEvent(XMLSecEventFactory.createXmlSecEndElement(element.getQName()));
            }
            chainProcessEvent(XMLSecEventFactory.createXMLSecEndDocument());
            endDocumentWritten = true;
        }
    }

    @Override
    public void close() throws XMLStreamException {
        try {
            writeEndDocument();
            outputProcessorChain.reset();
            outputProcessorChain.doFinal();
        } catch (XMLSecurityException e) {
            throw new XMLStreamException(e);
        }
    }

    @Override
    public void flush() throws XMLStreamException {
    }

    @Override
    public void writeAttribute(String localName, String value) throws XMLStreamException {
        writeAttribute(XMLConstants.DEFAULT_NS_PREFIX, XMLConstants.NULL_NS_URI, localName, value);
    }

    @Override
    public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
        writeAttribute(getNamespacePrefix(namespaceURI), namespaceURI, localName, value);
    }

    @Override
    public void writeAttribute(String prefix, String namespaceURI, String localName, String value)
            throws XMLStreamException {
        if (openStartElement == null) {
            throw new XMLStreamException("No open start element.");
        }
        openStartElement.addAttribute(
                XMLSecEventFactory.createXMLSecAttribute(
                        new QName(namespaceURI, localName, prefix), value));
    }

    @Override
    public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
        if (openStartElement == null) {
            throw new XMLStreamException("No open start element.");
        }
        this.openStartElement.addNamespace(XMLSecEventFactory.createXMLSecNamespace(prefix, namespaceURI));
    }

    @Override
    public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
        if (openStartElement == null) {
            throw new XMLStreamException("No open start element.");
        }
        //workaround for sun's stax parser
        if (this.openStartElement.getElementPrefix().equals(XMLConstants.DEFAULT_NS_PREFIX)) {
            this.openStartElement.setElementNamespace(namespaceURI);
            this.openStartElement.setElementPrefix(XMLConstants.DEFAULT_NS_PREFIX);
        }
        this.openStartElement.addNamespace(
                XMLSecEventFactory.createXMLSecNamespace(XMLConstants.DEFAULT_NS_PREFIX, namespaceURI));
    }

    @Override
    public void writeComment(String data) throws XMLStreamException {
        outputOpenStartElement();
        chainProcessEvent(XMLSecEventFactory.createXMLSecComment(data));
    }

    @Override
    public void writeProcessingInstruction(String target) throws XMLStreamException {
        writeProcessingInstruction(target, XMLConstants.DEFAULT_NS_PREFIX);
    }

    @Override
    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
        outputOpenStartElement();
        chainProcessEvent(XMLSecEventFactory.createXMLSecProcessingInstruction(target, data));
    }

    @Override
    public void writeCData(String data) throws XMLStreamException {
        outputOpenStartElement();
        chainProcessEvent(XMLSecEventFactory.createXMLSecCData(data));
    }

    @Override
    public void writeDTD(String dtd) throws XMLStreamException {
        if (elementStack != null) {
            throw new XMLStreamException("Not in proLOG");
        }
        chainProcessEvent(XMLSecEventFactory.createXMLSecDTD(dtd));
    }

    @Override
    public void writeEntityRef(final String name) throws XMLStreamException {
        outputOpenStartElement();
        chainProcessEvent(
                XMLSecEventFactory.createXMLSecEntityReference(
                        name,
                        XMLSecEventFactory.createXmlSecEntityDeclaration(name)
                )
        );
    }

    @Override
    public void writeStartDocument() throws XMLStreamException {
        writeStartDocument(null, null);
    }

    @Override
    public void writeStartDocument(String version) throws XMLStreamException {
        writeStartDocument(null, version);
    }

    @Override
    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        chainProcessEvent(XMLSecEventFactory.createXmlSecStartDocument(null, encoding, null, version));
    }

    @Override
    public void writeCharacters(String text) throws XMLStreamException {
        outputOpenStartElement();
        chainProcessEvent(XMLSecEventFactory.createXmlSecCharacters(text));
    }

    @Override
    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        outputOpenStartElement();
        chainProcessEvent(XMLSecEventFactory.createXmlSecCharacters(text, start, len));
    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        return getNamespacePrefix(uri);
    }

    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        if (elementStack == null) {
            this.namespaceContext.add(prefix, uri);
        } else {
            this.elementStack.getNamespaceContext().add(prefix, uri);
        }
    }

    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {
        if (elementStack == null) {
            this.namespaceContext.add(XMLConstants.DEFAULT_NS_PREFIX, uri);
        } else {
            this.elementStack.getNamespaceContext().add(XMLConstants.DEFAULT_NS_PREFIX, uri);
        }
    }

    @Override
    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        if (context == null) {
            throw new NullPointerException("context must not be null");
        }
        this.namespaceContext = new NSContext(context);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        if (this.elementStack == null) {
            return namespaceContext;
        }
        return elementStack.getNamespaceContext();
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        throw new IllegalArgumentException("Properties not supported");
    }

    public SecurePart getSignEntireRequestPart() {
        return signEntireRequestPart;
    }

    public void setSignEntireRequestPart(SecurePart signEntireRequestPart) {
        this.signEntireRequestPart = signEntireRequestPart;
    }

    public SecurePart getEncryptEntireRequestPart() {
        return encryptEntireRequestPart;
    }

    public void setEncryptEntireRequestPart(SecurePart encryptEntireRequestPart) {
        this.encryptEntireRequestPart = encryptEntireRequestPart;
    }

    private static class Element {

        private Element parentElement;

        private QName qName;
        private String elementName;
        private String elementNamespace;
        private String elementPrefix;
        private boolean emptyElement;
        private List<XMLSecNamespace> namespaces = Collections.emptyList();
        private List<XMLSecAttribute> attributes = Collections.emptyList();

        private NSContext namespaceContext;

        public Element(Element parentElement,
                       String elementNamespace, String elementName, String elementPrefix) {
            this(parentElement, null, elementNamespace, elementName, elementPrefix);
        }

        public Element(Element parentElement, NSContext namespaceContext,
                       String elementNamespace, String elementName, String elementPrefix) {
            this.parentElement = parentElement;
            this.namespaceContext = namespaceContext;
            this.elementName = elementName;
            setElementNamespace(elementNamespace);
            setElementPrefix(elementPrefix);
        }

        private Element getParentElement() {
            return parentElement;
        }

        private void setEmptyElement(boolean emptyElement) {
            this.emptyElement = emptyElement;
        }

        private String getElementName() {
            return elementName;
        }

        private String getElementNamespace() {
            return elementNamespace;
        }

        private void setElementNamespace(String elementNamespace) {
            if (elementNamespace == null) {
                this.elementNamespace = XMLConstants.NULL_NS_URI;
            } else {
                this.elementNamespace = elementNamespace;
            }
            this.qName = null;
        }

        private String getElementPrefix() {
            return elementPrefix;
        }

        private void setElementPrefix(String elementPrefix) {
            if (elementPrefix == null) {
                this.elementPrefix = XMLConstants.DEFAULT_NS_PREFIX;
            } else {
                this.elementPrefix = elementPrefix;
            }
            this.qName = null;
        }

        private List<XMLSecNamespace> getNamespaces() {
            return namespaces;
        }

        private void addNamespace(XMLSecNamespace namespace) {
            if (this.namespaces == Collections.<XMLSecNamespace>emptyList()) {
                this.namespaces = new ArrayList<>(1);
            }
            this.namespaces.add(namespace);

            //also add namespace to namespace-context
            getNamespaceContext().add(namespace.getPrefix(), namespace.getNamespaceURI());
        }

        private List<XMLSecAttribute> getAttributes() {
            return attributes;
        }

        private void addAttribute(XMLSecAttribute attribute) {
            if (this.attributes == Collections.<XMLSecAttribute>emptyList()) {
                this.attributes = new ArrayList<>(1);
            }
            this.attributes.add(attribute);
        }

        private NSContext getNamespaceContext() {
            if (this.namespaceContext == null) {
                if (emptyElement && parentElement != null) {
                    this.namespaceContext = parentElement.getNamespaceContext();
                } else if (parentElement != null) {
                    this.namespaceContext = new NSContext(parentElement.getNamespaceContext());
                } else {
                    this.namespaceContext = new NSContext(null);
                }
            }
            return this.namespaceContext;
        }

        private QName getQName() {
            if (this.qName == null) {
                this.qName = new QName(this.getElementNamespace(), this.getElementName(), this.getElementPrefix());
            }
            return this.qName;
        }
    }

    private static class NSContext implements NamespaceContext {

        private NamespaceContext parentNamespaceContext;
        private List<String> prefixNsList = Collections.emptyList();

        NSContext(NamespaceContext parentNamespaceContext) {
            this.parentNamespaceContext = parentNamespaceContext;
        }

        @Override
        public String getNamespaceURI(String prefix) {
            for (int i = 0; i < prefixNsList.size(); i += 2) {
                String s = prefixNsList.get(i);
                if (s.equals(prefix)) {
                    return prefixNsList.get(i + 1);
                }
            }

            if (parentNamespaceContext != null) {
                return parentNamespaceContext.getNamespaceURI(prefix);
            }
            return null;
        }

        @Override
        public String getPrefix(String namespaceURI) {
            for (int i = 1; i < prefixNsList.size(); i += 2) {
                String s = prefixNsList.get(i);
                if (s.equals(namespaceURI)) {
                    return prefixNsList.get(i - 1);
                }
            }

            if (parentNamespaceContext != null) {
                return parentNamespaceContext.getPrefix(namespaceURI);
            }
            return null;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Iterator getPrefixes(String namespaceURI) {
            List<String> prefixes = new ArrayList<>(1);
            for (int i = 1; i < prefixNsList.size(); i += 2) {
                String s = prefixNsList.get(i);
                if (s.equals(namespaceURI)) {
                    prefixes.add(prefixNsList.get(i - 1));
                }
            }

            if (parentNamespaceContext != null) {
                @SuppressWarnings("unchecked")
                Iterator<String> parentPrefixes = parentNamespaceContext.getPrefixes(namespaceURI);
                while (parentPrefixes.hasNext()) {
                    prefixes.add(parentPrefixes.next());
                }
            }
            return prefixes.iterator();
        }

        private void add(String prefix, String namespace) {
            if (this.prefixNsList == Collections.<String>emptyList()) {
                this.prefixNsList = new ArrayList<>(1);
            }
            this.prefixNsList.add(prefix);
            this.prefixNsList.add(namespace);
        }
    }
}
