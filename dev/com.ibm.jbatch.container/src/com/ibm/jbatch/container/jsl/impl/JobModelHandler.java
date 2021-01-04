/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.jsl.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshallerHandler;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.jbatch.container.jsl.JSLValidationEventHandler;
import com.ibm.jbatch.container.jsl.ValidatorHelper;
import com.ibm.jbatch.jsl.model.JSLJob;

/**
 *
 */
public class JobModelHandler extends DefaultHandler {

    private final static String sourceClass = JobModelHandler.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);

    private static final String JCP_NAMESPACE_URI = "http://xmlns.jcp.org/xml/ns/javaee";
    private static final String JAKARTA_NAMESPACE_URI = "https://jakarta.ee/xml/ns/jakartaee";

    public final static String SCHEMA_LOCATION_V1 = "jobXML_1_0.xsd";

    public final static String SCHEMA_LOCATION_V2 = "jobXML_2_0.xsd";

    /**
     * The JAXB unmarshaller that has been selected for the job XML file.
     * This field will be null until the root job element has been
     * parsed and its version has been determined.
     */
    UnmarshallerHandler ivHandler;

    /**
     * The buffered locator that will be set on ivHandler once created.
     */
    private Locator ivLocator;

    /**
     * The buffered startPrefixMapping events that will be replaced to ivHandler
     * once created.
     */
    private final List<PrefixMapping> ivPrefixMappings = new ArrayList<PrefixMapping>();

    final JSLValidationEventHandler validationHandler = new JSLValidationEventHandler();

    @Override
    public void setDocumentLocator(Locator locator) {
        ivLocator = locator;
    }

    @Override
    public void endDocument() throws SAXException {
        if (ivHandler != null) {
            ivHandler.endDocument();
        }
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        // Buffer events until startElement is called.
        if (ivHandler != null) {
            ivHandler.startPrefixMapping(prefix, uri);
        } else {
            ivPrefixMappings.add(new PrefixMapping(prefix, uri));
        }
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        if (ivHandler != null) {
            ivHandler.endPrefixMapping(prefix);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if (ivHandler == null) {
            if (!"job".equals(localName) ||
                (!(JCP_NAMESPACE_URI.equals(uri))
                 && !(JAKARTA_NAMESPACE_URI.equals(uri)))) {
                throw new SAXParseException("unexpected root element {" +
                                            uri + "}" + localName, ivLocator);
            }

            String version = atts.getValue("", "version");

            final String jaxbPackageName;
            String schemaName = SCHEMA_LOCATION_V1;

            if ("1.0".equals(version)) {
                jaxbPackageName = "com.ibm.jbatch.jsl.model.v1";
                schemaName = SCHEMA_LOCATION_V1;
            } else {
                jaxbPackageName = "com.ibm.jbatch.jsl.model.v2";
                schemaName = SCHEMA_LOCATION_V2;
            }

            final ClassLoader currentClassLoader = JSLJob.class.getClassLoader();

            logger.fine("JobModelResolver classloader obtained.");

            // Now that we've determined which JAXBContext to use, create a
            // handler and replay the events we've received so far.
            try {
                JAXBContext ctx = null;
                try {
                    ctx = JAXBContext.newInstance(jaxbPackageName, currentClassLoader);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Exception creating JAXBContext to unmarshal jobXML", e);
                }

                logger.fine("JobModelResolver JAXBContext obtained.");

                final Unmarshaller u = ctx.createUnmarshaller();
                u.setSchema(ValidatorHelper.getXJCLSchema(schemaName));

                u.setEventHandler(validationHandler);
                JAXBContext jaxbContext = JAXBContext.newInstance(jaxbPackageName, currentClassLoader);
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

                ivHandler = unmarshaller.getUnmarshallerHandler();
            } catch (JAXBException ex) {
                throw new RuntimeException(ex);
            }

            ivHandler.setDocumentLocator(ivLocator);
            ivHandler.startDocument();
            for (PrefixMapping prefixMapping : ivPrefixMappings) {
                ivHandler.startPrefixMapping(prefixMapping.ivPrefix, prefixMapping.ivURI);
            }
        }

        ivHandler.startElement(uri, localName, qName, atts);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        ivHandler.endElement(uri, localName, qName);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        ivHandler.characters(ch, start, length);
    }

    /**
     * Data for buffering startPrefixMapping calls.
     */
    private static class PrefixMapping {
        final String ivPrefix;
        final String ivURI;

        PrefixMapping(String prefix, String uri) {
            ivPrefix = prefix;
            ivURI = uri;
        }
    }
}
