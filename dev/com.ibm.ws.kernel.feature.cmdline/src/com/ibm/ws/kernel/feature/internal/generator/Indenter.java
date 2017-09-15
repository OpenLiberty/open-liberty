/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
import java.io.Writer;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

class Indenter {

    private final String LINE_SEPARATOR = getLineSeparator();
    private final XMLStreamWriter xmlWriter;
    private final Writer writer;

    /**
     * @param writer
     * @param w
     */
    public Indenter(XMLStreamWriter xw, Writer w) {
        this.xmlWriter = xw;
        writer = w;
    }

    /**
     * @param i
     * @throws IOException
     * @throws XMLStreamException
     */
    public void indent(int count) throws IOException, XMLStreamException {
        xmlWriter.flush();
        // We are trying to write good looking indented XML. The IBM JDK's XMLStreamWriter
        // will entity encode a \r character on windows so we can't write the line separator
        // on an IBM JDK. On a Sun JDK it doesn't entity encode \r, but if we use the writer to
        // write the line separator the end  > of the XML element ends up on the next line.
        // So instead we write a single space to the xmlWriter which causes on all JDKs the
        // element to be closed. We write the line separator using the writer, and the remaining
        // characters using the xml stream writer. Very hacky, but seems to work.
        xmlWriter.writeCharacters(" ");
        writer.write(LINE_SEPARATOR);
        for (int i = 0; i < count; i++) {
            xmlWriter.writeCharacters("    ");
        }

    }

    private String getLineSeparator() {
        String separator = getSystemProperty("line.separator");
        if (separator != null && (separator.equals("\n") || separator.equals("\r") || separator.equals("\r\n"))) {
            return separator;
        } else {
            return "\n";
        }
    }

    private String getSystemProperty(final String name) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(name);
            }
        });
    }

}