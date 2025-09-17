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

import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartDocument;

import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.ext.InputProcessorChain;
import org.apache.xml.security.stax.ext.XMLSecurityProperties;
import org.apache.xml.security.stax.ext.stax.XMLSecEvent;
import org.apache.xml.security.stax.ext.stax.XMLSecStartElement;

/**
 * A custom implementation of a XMLStreamReader to get back from the XMLEventReader world
 * to XMLStreamReader
 *
 */
public class XMLSecurityStreamReader implements XMLStreamReader {

    private final InputProcessorChain inputProcessorChain;
    private XMLSecEvent currentXMLSecEvent;
    private final boolean skipDocumentEvents;
    private String version;
    private boolean standalone;
    private boolean standaloneSet;
    private String characterEncodingScheme;

    private static final String ERR_STATE_NOT_ELEM = "Current state not START_ELEMENT or END_ELEMENT";
    private static final String ERR_STATE_NOT_STELEM = "Current state not START_ELEMENT";
    private static final String ERR_STATE_NOT_PI = "Current state not PROCESSING_INSTRUCTION";

    public XMLSecurityStreamReader(InputProcessorChain inputProcessorChain, XMLSecurityProperties securityProperties) {
        this.inputProcessorChain = inputProcessorChain;
        this.skipDocumentEvents = securityProperties.isSkipDocumentEvents();
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        if (XMLInputFactory.IS_NAMESPACE_AWARE.equals(name)) {
            return true;
        }
        return null;
    }

    @Override
    public int next() throws XMLStreamException {
        int eventType;
        try {
            inputProcessorChain.reset();
            currentXMLSecEvent = inputProcessorChain.processEvent();
            eventType = currentXMLSecEvent.getEventType();
            if (eventType == START_DOCUMENT) {
                // Even when skipDocumentEvents is true, we still want to get the information out of the event.
                // We only skip the event itself.
                StartDocument startDocument = (StartDocument) currentXMLSecEvent;
                version = startDocument.getVersion();
                if (startDocument.encodingSet()) {
                    characterEncodingScheme = startDocument.getCharacterEncodingScheme();
                }
                standalone = startDocument.isStandalone();
                standaloneSet = startDocument.standaloneSet();
                if (skipDocumentEvents) {
                    currentXMLSecEvent = inputProcessorChain.processEvent();
                    eventType = currentXMLSecEvent.getEventType();
                }
            }
        } catch (XMLSecurityException e) {
            throw new XMLStreamException(e);
        }
        return eventType;
    }

    private XMLSecEvent getCurrentEvent() {
        return currentXMLSecEvent;
    }

    @Override
    public void require(int type, String namespaceURI, String localName) throws XMLStreamException {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        if (xmlSecEvent.getEventType() != type) {
            throw new XMLStreamException("Event type mismatch");
        }

        if (localName != null) {
            if (xmlSecEvent.getEventType() != START_ELEMENT && xmlSecEvent.getEventType() != END_ELEMENT
                    && xmlSecEvent.getEventType() != ENTITY_REFERENCE) {
                throw new XMLStreamException("Expected non-null local name, but current token not a START_ELEMENT, END_ELEMENT or ENTITY_REFERENCE (was " + xmlSecEvent.getEventType() + ")");
            }
            String n = getLocalName();
            if (!n.equals(localName)) {
                throw new XMLStreamException("Expected local name '" + localName + "'; current local name '" + n + "'.");
            }
        }
        if (namespaceURI != null) {
            if (xmlSecEvent.getEventType() != START_ELEMENT && xmlSecEvent.getEventType() != END_ELEMENT) {
                throw new XMLStreamException("Expected non-null NS URI, but current token not a START_ELEMENT or END_ELEMENT (was " + xmlSecEvent.getEventType() + ")");
            }
            String uri = getNamespaceURI();
            // No namespace?
            if (namespaceURI.length() == 0) {
                if (uri != null && uri.length() > 0) {
                    throw new XMLStreamException("Expected empty namespace, instead have '" + uri + "'.");
                }
            } else {
                if (!namespaceURI.equals(uri)) {
                    throw new XMLStreamException("Expected namespace '" + namespaceURI + "'; have '"
                            + uri + "'.");
                }
            }
        }
    }

    @Override
    public String getElementText() throws XMLStreamException {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        if (xmlSecEvent.getEventType() != START_ELEMENT) {
            throw new XMLStreamException("Not positioned on a start element");
        }
        StringBuilder stringBuilder = new StringBuilder();

        /**
         * Need to loop to get rid of PIs, comments
         */
        loop:
        while (true) {
            int type = next();
            switch (type) {
                case END_ELEMENT:
                    break loop;
                case COMMENT:
                case PROCESSING_INSTRUCTION:
                    continue loop;
                case ENTITY_REFERENCE:
                case SPACE:
                case CDATA:
                case CHARACTERS:
                    stringBuilder.append(getText());
                    break;
                default:
                    throw new XMLStreamException("Expected a text token, got " + type + ".");
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public int nextTag() throws XMLStreamException {
        while (true) {
            int next = next();

            switch (next) {
                case SPACE:
                case COMMENT:
                case PROCESSING_INSTRUCTION:
                    continue;
                case CDATA:
                case CHARACTERS:
                    if (isWhiteSpace()) {
                        continue;
                    }
                    throw new XMLStreamException("Received non-all-whitespace CHARACTERS or CDATA event in nextTag().");
                case START_ELEMENT:
                case END_ELEMENT:
                    return next;
            }
            throw new XMLStreamException("Received event " + next
                    + ", instead of START_ELEMENT or END_ELEMENT.");
        }
    }

    @Override
    public boolean hasNext() throws XMLStreamException {
        return currentXMLSecEvent == null || currentXMLSecEvent.getEventType() != END_DOCUMENT;
    }

    @Override
    public void close() throws XMLStreamException {
        try {
            inputProcessorChain.reset();
            inputProcessorChain.doFinal();
        } catch (XMLSecurityException e) {
            throw new XMLStreamException(e);
        }
    }

    @Override
    public String getNamespaceURI(String prefix) {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        switch (xmlSecEvent.getEventType()) {
            case START_ELEMENT:
                return xmlSecEvent.asStartElement().getNamespaceURI(prefix);
            case END_ELEMENT:
                //the documentation states that getNamespaces on an end element should return ns'es that
                //go out of scope (currently not unsupported)
                //so that means we have to query the parent element
                XMLSecStartElement xmlSecStartElement = xmlSecEvent.asEndElement().getParentXMLSecStartElement();
                if (xmlSecStartElement != null) {
                    return xmlSecStartElement.getNamespaceURI(prefix);
                }
                return null;
            default:
                throw new IllegalStateException(ERR_STATE_NOT_ELEM);
        }
    }

    @Override
    public boolean isStartElement() {
        return getCurrentEvent().isStartElement();
    }

    @Override
    public boolean isEndElement() {
        return getCurrentEvent().isEndElement();
    }

    @Override
    public boolean isCharacters() {
        return getCurrentEvent().isCharacters();
    }

    @Override
    public boolean isWhiteSpace() {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        return xmlSecEvent.isCharacters() && xmlSecEvent.asCharacters().isWhiteSpace();
    }

    @Override
    public String getAttributeValue(String namespaceURI, String localName) {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        if (xmlSecEvent.getEventType() != START_ELEMENT) {
            throw new IllegalStateException(ERR_STATE_NOT_STELEM);
        }
        Attribute attribute = xmlSecEvent.asStartElement().getAttributeByName(new QName(namespaceURI, localName));
        if (attribute != null) {
            return attribute.getValue();
        }
        return null;
    }

    @Override
    public int getAttributeCount() {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        if (xmlSecEvent.getEventType() != START_ELEMENT) {
            throw new IllegalStateException(ERR_STATE_NOT_STELEM);
        }
        return xmlSecEvent.asStartElement().getOnElementDeclaredAttributes().size();
    }

    @Override
    public QName getAttributeName(int index) {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        if (xmlSecEvent.getEventType() != START_ELEMENT) {
            throw new IllegalStateException(ERR_STATE_NOT_STELEM);
        }
        return xmlSecEvent.asStartElement().getOnElementDeclaredAttributes().get(index).getName();
    }

    @Override
    public String getAttributeNamespace(int index) {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        if (xmlSecEvent.getEventType() != START_ELEMENT) {
            throw new IllegalStateException(ERR_STATE_NOT_STELEM);
        }
        return xmlSecEvent.asStartElement().getOnElementDeclaredAttributes().get(index).getAttributeNamespace().getNamespaceURI();
    }

    @Override
    public String getAttributeLocalName(int index) {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        if (xmlSecEvent.getEventType() != START_ELEMENT) {
            throw new IllegalStateException(ERR_STATE_NOT_STELEM);
        }
        return xmlSecEvent.asStartElement().getOnElementDeclaredAttributes().get(index).getName().getLocalPart();
    }

    @Override
    public String getAttributePrefix(int index) {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        if (xmlSecEvent.getEventType() != START_ELEMENT) {
            throw new IllegalStateException(ERR_STATE_NOT_STELEM);
        }
        return xmlSecEvent.asStartElement().getOnElementDeclaredAttributes().get(index).getName().getPrefix();
    }

    @Override
    public String getAttributeType(int index) {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        if (xmlSecEvent.getEventType() != START_ELEMENT) {
            throw new IllegalStateException(ERR_STATE_NOT_STELEM);
        }
        return xmlSecEvent.asStartElement().getOnElementDeclaredAttributes().get(index).getDTDType();
    }

    @Override
    public String getAttributeValue(int index) {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        if (xmlSecEvent.getEventType() != START_ELEMENT) {
            throw new IllegalStateException(ERR_STATE_NOT_STELEM);
        }
        return xmlSecEvent.asStartElement().getOnElementDeclaredAttributes().get(index).getValue();
    }

    @Override
    public boolean isAttributeSpecified(int index) {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        if (xmlSecEvent.getEventType() != START_ELEMENT) {
            throw new IllegalStateException(ERR_STATE_NOT_STELEM);
        }
        return xmlSecEvent.asStartElement().getOnElementDeclaredAttributes().get(index).isSpecified();
    }

    @SuppressWarnings("unchecked")
    @Override
    public int getNamespaceCount() {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        switch (xmlSecEvent.getEventType()) {
            case START_ELEMENT:
                return xmlSecEvent.asStartElement().getOnElementDeclaredNamespaces().size();
            case END_ELEMENT:
                int count = 0;
                Iterator<Namespace> namespaceIterator = xmlSecEvent.asEndElement().getNamespaces();
                while (namespaceIterator.hasNext()) {
                    namespaceIterator.next();
                    count++;
                }
                return count;
            default:
                throw new IllegalStateException(ERR_STATE_NOT_ELEM);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public String getNamespacePrefix(int index) {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        switch (xmlSecEvent.getEventType()) {
            case START_ELEMENT:
                return xmlSecEvent.asStartElement().getOnElementDeclaredNamespaces().get(index).getPrefix();
            case END_ELEMENT:
                int count = 0;
                Iterator<Namespace> namespaceIterator = xmlSecEvent.asEndElement().getNamespaces();
                while (namespaceIterator.hasNext()) {
                    Namespace namespace = namespaceIterator.next();
                    if (count == index) {
                        return namespace.getPrefix();
                    }
                    count++;
                }
                throw new ArrayIndexOutOfBoundsException(index);
            default:
                throw new IllegalStateException(ERR_STATE_NOT_ELEM);
        }
    }

    @Override
    public String getNamespaceURI(int index) {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        if (xmlSecEvent.getEventType() != START_ELEMENT) {
            throw new IllegalStateException(ERR_STATE_NOT_STELEM);
        }
        return xmlSecEvent.asStartElement().getOnElementDeclaredNamespaces().get(index).getNamespaceURI();
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        if (xmlSecEvent.getEventType() != START_ELEMENT) {
            throw new IllegalStateException(ERR_STATE_NOT_STELEM);
        }
        return xmlSecEvent.asStartElement().getNamespaceContext();
    }

    @Override
    public int getEventType() {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        if (xmlSecEvent == null) {
            try {
                return next();
            } catch (XMLStreamException e) {
                throw new IllegalStateException(e);
            }
        }
        if (xmlSecEvent.isCharacters() && xmlSecEvent.asCharacters().isIgnorableWhiteSpace()) {
            return XMLStreamConstants.SPACE;
        }
        return xmlSecEvent.getEventType();
    }

    @Override
    public String getText() {
        XMLSecEvent xmlSecEvent = getCurrentEvent();

        switch (xmlSecEvent.getEventType()) {
            case ENTITY_REFERENCE:
                return ((EntityReference) xmlSecEvent).getDeclaration().getReplacementText();
            case DTD:
                return ((DTD) xmlSecEvent).getDocumentTypeDeclaration();
            case COMMENT:
                return ((Comment) xmlSecEvent).getText();
            case CDATA:
            case SPACE:
            case CHARACTERS:
                return xmlSecEvent.asCharacters().getData();
            default:
                throw new IllegalStateException("Current state not TEXT");
        }
    }

    @Override
    public char[] getTextCharacters() {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        switch (xmlSecEvent.getEventType()) {
            case ENTITY_REFERENCE:
                return ((EntityReference) xmlSecEvent).getDeclaration().getReplacementText().toCharArray();
            case DTD:
                return ((DTD) xmlSecEvent).getDocumentTypeDeclaration().toCharArray();
            case COMMENT:
                return ((Comment) xmlSecEvent).getText().toCharArray();
            case CDATA:
            case SPACE:
            case CHARACTERS:
                return xmlSecEvent.asCharacters().getText();
            default:
                throw new IllegalStateException("Current state not TEXT");
        }
    }

    @Override
    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        switch (xmlSecEvent.getEventType()) {
            case ENTITY_REFERENCE:
                ((EntityReference) xmlSecEvent).getDeclaration().getReplacementText().getChars(sourceStart, sourceStart + length, target, targetStart);
                return length;
            case DTD:
                ((DTD) xmlSecEvent).getDocumentTypeDeclaration().getChars(sourceStart, sourceStart + length, target, targetStart);
                return length;
            case COMMENT:
                ((Comment) xmlSecEvent).getText().getChars(sourceStart, sourceStart + length, target, targetStart);
                return length;
            case CDATA:
            case SPACE:
            case CHARACTERS:
                xmlSecEvent.asCharacters().getData().getChars(sourceStart, sourceStart + length, target, targetStart);
                return length;
            default:
                throw new IllegalStateException("Current state not TEXT");
        }
    }

    @Override
    public int getTextStart() {
        return 0;
    }

    @Override
    public int getTextLength() {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        switch (xmlSecEvent.getEventType()) {
            case ENTITY_REFERENCE:
                return ((EntityReference) xmlSecEvent).getDeclaration().getReplacementText().length();
            case DTD:
                return ((DTD) xmlSecEvent).getDocumentTypeDeclaration().length();
            case COMMENT:
                return ((Comment) xmlSecEvent).getText().length();
            case CDATA:
            case SPACE:
            case CHARACTERS:
                return xmlSecEvent.asCharacters().getData().length();
            default:
                throw new IllegalStateException("Current state not TEXT");
        }
    }

    @Override
    public String getEncoding() {
        return inputProcessorChain.getDocumentContext().getEncoding();
    }

    private static final int MASK_GET_TEXT =
            (1 << CHARACTERS) | (1 << CDATA) | (1 << SPACE)
                    | (1 << COMMENT) | (1 << DTD) | (1 << ENTITY_REFERENCE);

    @Override
    public boolean hasText() {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        return ((1 << xmlSecEvent.getEventType()) & MASK_GET_TEXT) != 0;
    }

    @Override
    public Location getLocation() {
        return new Location() {
            @Override
            public int getLineNumber() {
                return -1;
            }

            @Override
            public int getColumnNumber() {
                return -1;
            }

            @Override
            public int getCharacterOffset() {
                return -1;
            }

            @Override
            public String getPublicId() {
                return null;
            }

            @Override
            public String getSystemId() {
                return null;
            }
        };
    }

    @Override
    public QName getName() {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        switch (xmlSecEvent.getEventType()) {
            case START_ELEMENT:
                return xmlSecEvent.asStartElement().getName();
            case END_ELEMENT:
                return xmlSecEvent.asEndElement().getName();
            default:
                throw new IllegalStateException(ERR_STATE_NOT_ELEM);
        }
    }

    @Override
    public String getLocalName() {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        switch (xmlSecEvent.getEventType()) {
            case START_ELEMENT:
                return xmlSecEvent.asStartElement().getName().getLocalPart();
            case END_ELEMENT:
                return xmlSecEvent.asEndElement().getName().getLocalPart();
            default:
                throw new IllegalStateException(ERR_STATE_NOT_ELEM);
        }
    }

    @Override
    public boolean hasName() {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        return xmlSecEvent.getEventType() == START_ELEMENT || xmlSecEvent.getEventType() == END_ELEMENT;
    }

    @Override
    public String getNamespaceURI() {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        switch (xmlSecEvent.getEventType()) {
            case START_ELEMENT:
                return xmlSecEvent.asStartElement().getName().getNamespaceURI();
            case END_ELEMENT:
                return xmlSecEvent.asEndElement().getName().getNamespaceURI();
            default:
                throw new IllegalStateException(ERR_STATE_NOT_ELEM);
        }
    }

    @Override
    public String getPrefix() {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        switch (xmlSecEvent.getEventType()) {
            case START_ELEMENT:
                return xmlSecEvent.asStartElement().getName().getPrefix();
            case END_ELEMENT:
                return xmlSecEvent.asEndElement().getName().getPrefix();
            default:
                throw new IllegalStateException(ERR_STATE_NOT_ELEM);
        }
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public boolean isStandalone() {
        return standalone;
    }

    @Override
    public boolean standaloneSet() {
        return standaloneSet;
    }

    @Override
    public String getCharacterEncodingScheme() {
        return characterEncodingScheme;
    }

    @Override
    public String getPITarget() {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        if (xmlSecEvent.getEventType() != PROCESSING_INSTRUCTION) {
            throw new IllegalStateException(ERR_STATE_NOT_PI);
        }
        return ((ProcessingInstruction) xmlSecEvent).getTarget();
    }

    @Override
    public String getPIData() {
        XMLSecEvent xmlSecEvent = getCurrentEvent();
        if (xmlSecEvent.getEventType() != PROCESSING_INSTRUCTION) {
            throw new IllegalStateException(ERR_STATE_NOT_PI);
        }
        return ((ProcessingInstruction) xmlSecEvent).getData();
    }
}
