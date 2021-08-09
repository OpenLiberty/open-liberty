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
package com.ibm.ws.config.schemagen.internal;

import java.io.IOException;
import java.io.Writer;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Simple XMLStreamWriter wrapper that indents the XML elements.
 */
class IndentingXMLStreamWriter extends DelegatingXMLStreamWriter {

    private static final String DEFAULT_INDENT = "    ";
    private static final String LINE_SEPARATOR = getLineSeparator();

    enum TextElement {
        NONE, START_ELEMENT, TEXT
    };

    private final Writer writer;
    private String identString = DEFAULT_INDENT;
    private int depth = 0;
    private TextElement textElement = TextElement.NONE;

    public IndentingXMLStreamWriter(XMLStreamWriter xmlWriter, Writer writer) {
        super(xmlWriter);
        this.writer = writer;
    }

    public void setIndentString(String identString) {
        this.identString = identString;
    }

    @Override
    public void writeEndDocument() throws XMLStreamException {
        super.writeEndDocument();
        indent();
    }

    @Override
    public void writeEmptyElement(String localName) throws XMLStreamException {
        indent();
        super.writeEmptyElement(localName);
        textElement = TextElement.NONE;
    }

    @Override
    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        indent();
        super.writeEmptyElement(namespaceURI, localName);
        textElement = TextElement.NONE;
    }

    @Override
    public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        indent();
        super.writeEmptyElement(prefix, localName, namespaceURI);
        textElement = TextElement.NONE;
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        depth--;
        if (textElement != TextElement.TEXT) {
            indent();
        }
        textElement = TextElement.NONE;
        super.writeEndElement();
    }

    @Override
    public void writeStartElement(String localName) throws XMLStreamException {
        indent();
        super.writeStartElement(localName);
        depth++;
        textElement = TextElement.START_ELEMENT;
    }

    @Override
    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        indent();
        super.writeStartElement(namespaceURI, localName);
        depth++;
        textElement = TextElement.START_ELEMENT;
    }

    @Override
    public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        indent();
        super.writeStartElement(prefix, localName, namespaceURI);
        depth++;
        textElement = TextElement.START_ELEMENT;
    }

    @Override
    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        super.writeCharacters(text, start, len);
        if (textElement == TextElement.START_ELEMENT) {
            textElement = TextElement.TEXT;
        } else if (textElement != TextElement.TEXT) {
            textElement = TextElement.NONE;
        }
    }

    @Override
    public void writeCharacters(String text) throws XMLStreamException {
        super.writeCharacters(text);
        if (textElement == TextElement.START_ELEMENT) {
            textElement = TextElement.TEXT;
        } else if (textElement != TextElement.TEXT) {
            textElement = TextElement.NONE;
        }
    }

    @Override
    public void writeCData(String data) throws XMLStreamException {
        super.writeCData(data);
        textElement = TextElement.NONE;
    }

    @Override
    public void writeComment(String data) throws XMLStreamException {
        super.writeComment(data);
        textElement = TextElement.NONE;
    }

    @Override
    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
        super.writeProcessingInstruction(target, data);
        textElement = TextElement.NONE;
    }

    @Override
    public void writeProcessingInstruction(String target) throws XMLStreamException {
        super.writeProcessingInstruction(target);
        textElement = TextElement.NONE;
    }

    private void indent() throws XMLStreamException {
        flush();
        try {
            writeCharacters(" ");
            writer.write(LINE_SEPARATOR);
            for (int i = 0; i < depth; i++) {
                writer.write(identString);
            }
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    private static String getLineSeparator() {
        String separator = getSystemProperty("line.separator");
        if (separator != null && (separator.equals("\n") || separator.equals("\r") || separator.equals("\r\n"))) {
            return separator;
        } else {
            return "\n";
        }
    }

    private static String getSystemProperty(final String name) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(name);
            }
        });
    }
}
