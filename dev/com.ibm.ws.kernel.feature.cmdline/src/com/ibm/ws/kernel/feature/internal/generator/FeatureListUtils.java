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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.ibm.ws.kernel.feature.internal.generator.FeatureListOptions.ReturnCode;

/**
 *
 */
public class FeatureListUtils {

    private final static String FEATURE_LIST_DEFAULT_ENCODING = "UTF-8";

    private final Indenter indenter;
    private final XMLStreamWriter xmlStreamWriter;

    private String encoding;

    private FeatureListOptions options;

    private final FileOutputStream fileOutputStream;

    public FeatureListUtils(FeatureListOptions options) {
        try {
            this.fileOutputStream = new FileOutputStream(options.getOutputFile());
            String encoding = options.getEncoding();
            if (encoding == null) {
                encoding = FEATURE_LIST_DEFAULT_ENCODING;
            }

            this.options = options;
            this.encoding = encoding;

            Writer w = new OutputStreamWriter(fileOutputStream, encoding);

            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            try {
                //     this.eventWriter = factory.createXMLEventWriter(w);
                this.xmlStreamWriter = factory.createXMLStreamWriter(w);

            } catch (XMLStreamException e) {
                throw new IOException("Error generating feature list", e);
            }
            this.indenter = new Indenter(xmlStreamWriter, w);
        } catch (IOException ex) {
            options.setReturnCode(ReturnCode.RUNTIME_EXCEPTION);
            throw new RuntimeException(ex);
        }

    }

    protected XMLStreamWriter getXMLStreamWriter() {
        return this.xmlStreamWriter;
    }

    protected Indenter getIndenter() {
        return this.indenter;
    }

    public void writeStartDocument() {
        try {
            this.xmlStreamWriter.writeStartDocument(encoding, "1.0");
        } catch (XMLStreamException e) {
            IOException ex = new IOException("Error generating feature list", e);
            options.setReturnCode(ReturnCode.RUNTIME_EXCEPTION);
            throw new RuntimeException(ex);
        }
    }

    public void writeEndDocument() {
        try {
            try {
                indenter.indent(0);
                xmlStreamWriter.writeEndDocument();
            } catch (XMLStreamException ex) {
                throw new IOException("Error generating feature list", ex);
            }

        } catch (IOException ex) {
            options.setReturnCode(ReturnCode.RUNTIME_EXCEPTION);
            throw new RuntimeException(ex);
        }
    }

    public void startFeatureInfo(String name, String location, String productId) {
        try {
            try {
                indenter.indent(0);
                xmlStreamWriter.writeStartElement("featureInfo");
                xmlStreamWriter.writeAttribute("name", name);

                if (productId != null) {
                    xmlStreamWriter.writeAttribute("productId", productId);
                }

                xmlStreamWriter.writeAttribute("location", location);
            } catch (XMLStreamException ex) {
                throw new IOException("Error generating feature list", ex);
            }

        } catch (IOException e) {
            options.setReturnCode(ReturnCode.RUNTIME_EXCEPTION);
            throw new RuntimeException(e);
        }

    }

    public void endFeatureInfo() {
        try {
            try {
                indenter.indent(0);
                xmlStreamWriter.writeEndElement();
            } catch (XMLStreamException e) {
                throw new IOException("Error generating feature list", e);
            }
        } catch (IOException e) {
            options.setReturnCode(ReturnCode.RUNTIME_EXCEPTION);
            throw new RuntimeException(e);
        }

    }

}
