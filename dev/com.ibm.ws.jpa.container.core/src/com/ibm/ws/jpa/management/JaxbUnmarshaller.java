/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.management;

import static com.ibm.ws.jpa.management.JPAConstants.JPA_RESOURCE_BUNDLE_NAME;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_TRACE_GROUP;

import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.PersistenceException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshallerHandler;
import javax.xml.parsers.SAXParser;
import javax.xml.validation.Schema;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.xml.ParserFactory;

/**
 * Provides access to, and an abstraction of the JAXB representation of
 * persistence.xml files (all versions). <p>
 *
 * This 'unmarshaller' is intended to be used to invoke the JAXB unmarshaller,
 * automatically determining the correct schema version to validate against,
 * and returns non-JAXB and non-version specific classes. Clients of this
 * class can be coded and compiled independent of the JAXB classes and do
 * not need to worry about schema version specific packages. <p>
 */
final class JaxbUnmarshaller extends DefaultHandler {
    private static final String CLASS_NAME = JaxbUnmarshaller.class.getName();

    private static final TraceComponent tc = Tr.register(JaxbUnmarshaller.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME);

    private static final String PERSISTENCE_NAMESPACE_URI = "http://java.sun.com/xml/ns/persistence"; // d656864
    private static final String JCP_PERSISTENCE_NAMESPACE_URI = "http://xmlns.jcp.org/xml/ns/persistence";
    private static final String JAKARTA_PERSISTENCE_NAMESPACE_URI = "https://jakarta.ee/xml/ns/persistence";

    private static final String PERSISTENCE_LOCAL_NAME = "persistence"; // d656864
    private static final String VERSION_ATTRIBUTE_NAME = "version"; // d656864

    private static final ClassLoader svClassLoader = JaxbUnmarshaller.class.getClassLoader();

    /**
     * Determines the correct schema version of the specified persistence.xml
     * file, and invokes the JAXB unmarshaller using the corresponding
     * pre-generated JAXB classes for that version. <p>
     *
     * Non-JAXB, non-version specific objects are returned so the caller
     * does not need to handle different JAXB packaging per schema version. <p>
     **/
    public static JaxbPersistence unmarshal(JPAPXml pxml) throws PersistenceException //d86387
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "unmarshal : " + pxml);

        InputStream is = null;
        JaxbPersistence persistence = null;

        try {
            SAXParser parser = ParserFactory.newSAXParser(true, false);
            JaxbUnmarshaller handler = new JaxbUnmarshaller(pxml); // d656864

            is = pxml.openStream();
            parser.parse(is, handler);

            persistence = handler.ivPersistence;
            persistence.setResult(handler.ivHandler.getResult());
        } catch (Throwable ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".unmarshal", "109", pxml);

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "  unmarshal : caught exception : " + ex);

            // 7.1.1 Responsibilities of the Container
            //
            // At deployment time the container is responsible for scanning the locations specified
            // in Section 6.2 and discovering the persistence.xml files and processing them.
            //
            // When the container finds a persistence.xml file, it processes the persistence unit
            // definitions that it contains. The container must validate the persistence.xml file
            // against the persistence_1_0.xsd schema and report any validation errors.
            //
            // Provider or data source information not specified in the persistence.xml file must be
            // provided at deployment time or defaulted by the container.

            Throwable root = findRootCause(ex);

            // Log an error message and create a MetaDataException to throw.
            PersistenceException pex = null; // d86387
            String appName = pxml.getApplInfo().getApplName();
            String modName = pxml.getArchiveName();

            if (root instanceof SAXParseException) {
                // Use data in SAXParseException for a more meaningful error message.
                SAXParseException saxEx = (SAXParseException) root;

                // SYNTAX_ERROR_IN_PERSISTENCE_XML_CWWJP0040E=CWWJP0040E:
                // Incorrect syntax or error detected in the persistence.xml
                // file in application: {0}, module: {1}, at line number: {2},
                // column number: {3}. The following associated error message
                // occurred: {4}
                int line = saxEx.getLineNumber();
                int column = saxEx.getColumnNumber();
                Tr.error(tc, "SYNTAX_ERROR_IN_PERSISTENCE_XML_CWWJP0040E", appName, modName, line, column, saxEx);

                pex = new PersistenceException("CWWJP0040E: The persistence.xml" +
                                               " in application " + appName +
                                               ", module " + modName +
                                               ", has a syntax error at line number: " +
                                               line + ", column number: " + column +
                                               ".", saxEx); // d86387
            } else {
                // SAXParseException did not occur, so we must log a more generic error
                // message to indicate some kind of failure occurred while trying to read
                // the persistence.xml file.

                // MALFORMED_PERSISTENCE_XML_CWWJP0018E=CWWJP0018E: Incorrect syntax
                // or error detected in the {0} file. The following associated error
                // message occurred: {1}
                String urlString = pxml.getRootURL().getPath();
                Tr.error(tc, "MALFORMED_PERSISTENCE_XML_CWWJP0018E", urlString, root);

                pex = new PersistenceException("CWWJP0018E: Incorrect syntax " +
                                               "or error detected in " + urlString +
                                               " for application " + appName +
                                               " module " + modName +
                                               ". The following associated error occurred:", root); // 86387
            }

            throw pex;
        } finally {
            // Normally, the close will be performed as soon as possible, to
            // insure the jar file locks are not held (which can be problematic
            // on windows)... but just in case there is a failure before the
            // close() occurs, make sure the close occurs here.
            if (is != null) {
                try {
                    is.close();
                } catch (Throwable ex) {
                    // The only way this occurs is if an exception was already
                    // thrown above... so just log this, and allow the above
                    // exception to flow out of this method.
                    FFDCFilter.processException(ex, CLASS_NAME + ".unmarshal", "192", is);
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "  unmarshal : caught exception : " + ex);
                }
            }

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "unmarshal : " + persistence);
        }

        return persistence;
    }

    /**
     * Find root cause of a specified Throwable object.
     *
     * @param t is the Throwable object.
     *
     * @return the root cause object.
     */
    private static Throwable findRootCause(Throwable t) {
        Throwable root = t;
        Throwable cause = root.getCause();
        while (cause != null) {
            root = cause;
            cause = root.getCause();
        }
        return root;
    }

    /**
     * Data about the persistence.xml file being opened.
     */
    private final JPAPXml ivJPAPXml;

    /**
     * The persistence helper being used. This field will be null until the
     * root persistence element has been parsed.
     */
    private JaxbPersistence ivPersistence;

    /**
     * The JAXB unmarshaller that has been selected for the persistence.xml.
     * This field will be null until the root persistence element has been
     * parsed and its version has been determined.
     */
    private UnmarshallerHandler ivHandler;

    /**
     * The buffered locator that will be set on ivHandler once created.
     */
    private Locator ivLocator;

    /**
     * The buffered startPrefixMapping events that will be replaced to ivHandler
     * once created.
     */
    private final List<PrefixMapping> ivPrefixMappings = new ArrayList<PrefixMapping>();

    /**
     * Default constructor is private to prevent instances from being created.
     **/
    private JaxbUnmarshaller(JPAPXml jpapXml) {
        ivJPAPXml = jpapXml;
    }

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
            if (!PERSISTENCE_LOCAL_NAME.equals(localName) ||
                (!(PERSISTENCE_NAMESPACE_URI.equals(uri)) && !(JCP_PERSISTENCE_NAMESPACE_URI.equals(uri))
                 && !(JAKARTA_PERSISTENCE_NAMESPACE_URI.contentEquals(uri)))) {
                throw new SAXParseException("expected root element {" +
                                            PERSISTENCE_NAMESPACE_URI + "}" +
                                            PERSISTENCE_LOCAL_NAME, ivLocator);
            }

            String version = atts.getValue("", VERSION_ATTRIBUTE_NAME);

            if (JaxbPersistence10.SCHEMA_VERSION.equals(version)) {
                ivPersistence = new JaxbPersistence10(ivJPAPXml);
            } else if (JaxbPersistence20.SCHEMA_VERSION.equals(version)) {
                ivPersistence = new JaxbPersistence20(ivJPAPXml);
            } else if (JaxbPersistence21.SCHEMA_VERSION.equals(version)) {
                ivPersistence = new JaxbPersistence21(ivJPAPXml);
            } else if (JaxbPersistence22.SCHEMA_VERSION.equals(version)) {
                ivPersistence = new JaxbPersistence22(ivJPAPXml);
            } else if (JaxbPersistence30.SCHEMA_VERSION.equals(version)) {
                ivPersistence = new JaxbPersistence30(ivJPAPXml);
            } else {
                // TODO, this is a new situation, in the past we've always been able to default to the latest spec.
            }

            // Now that we've determined which JAXBContext to use, create a
            // handler and replay the events we've received so far.
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(ivPersistence.ivJAXBPackageName, new DelegateToJVMClassLoader(svClassLoader));
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

                // Setup schema factory and load schema for unmarshaller so that JAXB
                // will do syntax and semantic checking for the persistence.xml file.
                Schema schema = ivJPAPXml.newSchema(ivPersistence.ivXSDName);
                unmarshaller.setSchema(schema);

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

    private static class DelegateToJVMClassLoader extends ClassLoader {
        final ClassLoader jvmCL;

        DelegateToJVMClassLoader(ClassLoader parent) {
            super(parent);
            jvmCL = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {

                @Override
                public ClassLoader run() {
                    return ClassLoader.getSystemClassLoader();
                }
            });
        }

        @FFDCIgnore(ClassNotFoundException.class)
        @Override
        public Class<?> findClass(String className) throws ClassNotFoundException {
            return jvmCL.loadClass(className);
        }
    }
}
