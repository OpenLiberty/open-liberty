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

import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.XMLEvent;

import org.apache.xml.security.stax.ext.stax.XMLSecAttribute;
import org.apache.xml.security.stax.ext.stax.XMLSecCharacters;
import org.apache.xml.security.stax.ext.stax.XMLSecEvent;
import org.apache.xml.security.stax.ext.stax.XMLSecNamespace;
import org.apache.xml.security.stax.ext.stax.XMLSecStartElement;

/**
 */
public class XMLSecurityEventWriter implements XMLEventWriter, AutoCloseable {

    private final XMLStreamWriter xmlStreamWriter;

    public XMLSecurityEventWriter(XMLStreamWriter xmlStreamWriter) {
        this.xmlStreamWriter = xmlStreamWriter;
    }

    @Override
    public void add(XMLEvent event) throws XMLStreamException {
        if (!(event instanceof XMLSecEvent)) {
            throw new IllegalArgumentException("XMLEvent must be an instance of XMLSecEvent");
        }

        XMLSecEvent xmlSecEvent = (XMLSecEvent)event;
        switch (xmlSecEvent.getEventType()) {
            case XMLStreamConstants.START_ELEMENT:
                XMLSecStartElement xmlSecStartElement = xmlSecEvent.asStartElement();
                QName n = xmlSecStartElement.getName();
                this.xmlStreamWriter.writeStartElement(n.getPrefix(), n.getLocalPart(), n.getNamespaceURI());

                List<XMLSecNamespace> xmlSecNamespaces = xmlSecStartElement.getOnElementDeclaredNamespaces();
                for (XMLSecNamespace namespace : xmlSecNamespaces) {
                    add(namespace);
                }

                List<XMLSecAttribute> xmlSecAttributes = xmlSecStartElement.getOnElementDeclaredAttributes();
                for (XMLSecAttribute xmlSecAttribute : xmlSecAttributes) {
                    add(xmlSecAttribute);
                }
                break;

            case XMLStreamConstants.END_ELEMENT:
                this.xmlStreamWriter.writeEndElement();
                break;

            case XMLStreamConstants.PROCESSING_INSTRUCTION:
                ProcessingInstruction pi = (ProcessingInstruction) xmlSecEvent;
                this.xmlStreamWriter.writeProcessingInstruction(pi.getTarget(), pi.getData());
                break;

            case XMLStreamConstants.CHARACTERS:
                XMLSecCharacters characters = xmlSecEvent.asCharacters();
                if (!characters.isCData()) {
                    final char[] text = characters.getText();
                    this.xmlStreamWriter.writeCharacters(text, 0, text.length);
                } else {
                    this.xmlStreamWriter.writeCData(characters.getData());
                }
                break;

            case XMLStreamConstants.COMMENT:
                this.xmlStreamWriter.writeComment(((Comment) xmlSecEvent).getText());
                break;

            case XMLStreamConstants.START_DOCUMENT:
                StartDocument startDocument = (StartDocument) xmlSecEvent;
                if (!startDocument.encodingSet()) {
                    this.xmlStreamWriter.writeStartDocument(startDocument.getVersion());
                } else {
                    this.xmlStreamWriter.writeStartDocument(startDocument.getCharacterEncodingScheme(), startDocument.getVersion());
                }
                break;

            case XMLStreamConstants.END_DOCUMENT:
                this.xmlStreamWriter.writeEndDocument();
                break;

            case XMLStreamConstants.ENTITY_REFERENCE:
                this.xmlStreamWriter.writeEntityRef(((EntityReference) xmlSecEvent).getName());
                break;

            case XMLStreamConstants.ATTRIBUTE:
                Attribute attribute = (Attribute) xmlSecEvent;
                QName name = attribute.getName();
                this.xmlStreamWriter.writeAttribute(name.getPrefix(), name.getNamespaceURI(), name.getLocalPart(), attribute.getValue());
                break;

            case XMLStreamConstants.DTD:
                this.xmlStreamWriter.writeDTD(((DTD) xmlSecEvent).getDocumentTypeDeclaration());
                break;

            case XMLStreamConstants.CDATA:
                this.xmlStreamWriter.writeCData(xmlSecEvent.asCharacters().getData());
                break;

            case XMLStreamConstants.NAMESPACE:
                Namespace ns = (Namespace) xmlSecEvent;
                this.xmlStreamWriter.writeNamespace(ns.getPrefix(), ns.getNamespaceURI());
                break;

            case XMLStreamConstants.SPACE:
            case XMLStreamConstants.NOTATION_DECLARATION:
            case XMLStreamConstants.ENTITY_DECLARATION:
            default:
                throw new XMLStreamException("Illegal event");
        }
    }

    @Override
    public void add(XMLEventReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            add(reader.nextEvent());
        }
    }

    @Override
    public void close() throws XMLStreamException {
        this.xmlStreamWriter.close();
    }

    @Override
    public void flush() throws XMLStreamException {
        this.xmlStreamWriter.flush();
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return this.xmlStreamWriter.getNamespaceContext();
    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        return this.xmlStreamWriter.getPrefix(uri);
    }

    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {
        this.xmlStreamWriter.setDefaultNamespace(uri);
    }

    @Override
    public void setNamespaceContext(NamespaceContext namespaceContext) throws XMLStreamException {
        this.xmlStreamWriter.setNamespaceContext(namespaceContext);
    }

    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        this.xmlStreamWriter.setPrefix(prefix, uri);
    }
}
