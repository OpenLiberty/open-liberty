/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.generator;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class FeatureListWriter {
    private final XMLStreamWriter writer;
    private final Indenter i;

    public FeatureListWriter(FeatureListUtils utils) throws XMLStreamException, UnsupportedEncodingException {
        this.writer = utils.getXMLStreamWriter();
        this.i = utils.getIndenter();
    }

    public void writeTextElement(String nodeName, String text) throws IOException, XMLStreamException {
        i.indent(2);
        writer.writeStartElement(nodeName);
        writer.writeCharacters(text);
        writer.writeEndElement();
    }

    public void writeTextElementWithAttributes(String nodeName, String text, Map<String, String> attrs) throws IOException, XMLStreamException {
        i.indent(2);
        writer.writeStartElement(nodeName);
        for (Map.Entry<String, String> attr : attrs.entrySet()) {
            String rawKey = attr.getKey();
            String key = rawKey.endsWith(":") ? rawKey.substring(0, rawKey.length() -1) : rawKey;
            writer.writeAttribute(key, attr.getValue());
        }
        writer.writeCharacters(text);
        writer.writeEndElement();
    }

    public void startFeature(String nodeName, String name) throws IOException, XMLStreamException {
        i.indent(1);
        writer.writeStartElement(nodeName);
        if (name != null) {
            writer.writeAttribute("name", name);
        }
    }

    public void endFeature() throws IOException, XMLStreamException {
        i.indent(1);
        writer.writeEndElement();
    }

    /**
     * @param nodeName
     * @throws XMLStreamException
     * @throws IOException
     */
    public void startFeature(String nodeName) throws IOException, XMLStreamException {
        startFeature(nodeName, null);
    }

    public void writeIncludeFeature(String preferred, List<String> tolerates, String shortName) throws IOException, XMLStreamException {
        i.indent(2);
        writer.writeStartElement("include");
        writer.writeAttribute("symbolicName", preferred);
        if (shortName != null)
            writer.writeAttribute("shortName", shortName);
        if (tolerates != null) {
            StringBuilder toleratesValue = new StringBuilder();
            for (String tolerate : tolerates) {
                if (toleratesValue.length() > 0) {
                    toleratesValue.append(',');
                }
                toleratesValue.append(tolerate);
            }
            writer.writeAttribute("tolerates", toleratesValue.toString());
        }
        writer.writeEndElement();
    }
}