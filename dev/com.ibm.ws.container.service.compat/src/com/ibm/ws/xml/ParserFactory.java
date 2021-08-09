/*******************************************************************************
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * The purpose of this class is to force the usage of the Parser provided by the Runtime and not
 * any one that an application may provide.
 */
public class ParserFactory {

    public static DocumentBuilderFactory newDocumentBuilderFactory() throws FactoryConfigurationError {
        try {
            return (DocumentBuilderFactory) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws FactoryConfigurationError {
                    Thread currThread = Thread.currentThread();
                    ClassLoader oldLoader = currThread.getContextClassLoader();
                    currThread.setContextClassLoader(ParserFactory.class.getClassLoader());
                    try {
                        return DocumentBuilderFactory.newInstance();
                    }
                                        finally {
                                            currThread.setContextClassLoader(oldLoader);
                                        }
                                    }
            });
        } catch (PrivilegedActionException pae) {
            Throwable t = pae.getCause();
            if (t instanceof FactoryConfigurationError) {
                throw (FactoryConfigurationError) t;
            }
        }
        return null;
    }

    public static SAXParserFactory newSAXParserFactory() throws FactoryConfigurationError {
        try {
            return (SAXParserFactory) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws FactoryConfigurationError {
                    Thread currThread = Thread.currentThread();
                    ClassLoader oldLoader = currThread.getContextClassLoader();
                    currThread.setContextClassLoader(ParserFactory.class.getClassLoader());
                    try {
                        return SAXParserFactory.newInstance();
                    }
                                        finally {
                                            currThread.setContextClassLoader(oldLoader);
                                        }
                                    }
            });
        } catch (PrivilegedActionException pae) {
            Throwable t = pae.getCause();
            if (t instanceof FactoryConfigurationError) {
                throw (FactoryConfigurationError) t;
            }
        }
        return null;
    }

    /* Start 198795 */
    public static SAXParser newSAXParser() throws FactoryConfigurationError, ParserConfigurationException, SAXException {
        // PK95911
        return newSAXParser(false, false);
    }

    /* End 198795 */

    /* Start PK95911 */
    public static SAXParser newSAXParser(final boolean setNamespaceAware, final boolean setValidating) throws FactoryConfigurationError, ParserConfigurationException, SAXException {
        try {
            return (SAXParser) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws ParserConfigurationException, SAXException, FactoryConfigurationError {
                    Thread currThread = Thread.currentThread();
                    ClassLoader oldLoader = currThread.getContextClassLoader();
                    currThread.setContextClassLoader(ParserFactory.class.getClassLoader());
                    try {
                        if (!setNamespaceAware && !setValidating) {
                            return SAXParserFactory.newInstance().newSAXParser();
                        } else {
                            final SAXParserFactory factory = SAXParserFactory.newInstance();
                            factory.setNamespaceAware(setNamespaceAware);
                            factory.setValidating(setValidating);
                            return factory.newSAXParser();
                        }
                    }
                                        finally {
                                            currThread.setContextClassLoader(oldLoader);
                                        }
                                    }
            });
        } catch (PrivilegedActionException pae) {
            Throwable t = pae.getCause();
            if (t instanceof FactoryConfigurationError) {
                throw (FactoryConfigurationError) t;
            } else if (t instanceof ParserConfigurationException) {
                throw (ParserConfigurationException) t;
            } else if (t instanceof SAXException) {
                throw (SAXException) t;
            }
        }
        return null;
    }

    /* End PK95911 */

    /* 170319: Modified parser retrieval. */
    public static XMLReader createXMLReader() throws SAXException, ParserConfigurationException, FactoryConfigurationError {
        try {
            return (XMLReader) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws SAXException, ParserConfigurationException, FactoryConfigurationError {
                    Thread currThread = Thread.currentThread();
                    ClassLoader oldLoader = currThread.getContextClassLoader();
                    currThread.setContextClassLoader(ParserFactory.class.getClassLoader());
                    try {
                        /**
                         * Changed to use pluggable parser version. Needed for JDK 1.4.1
                         */
                        return SAXParserFactory.newInstance().newSAXParser().getXMLReader();
                        //return XMLReaderFactory.createXMLReader();
                    }
                                        finally {
                                            currThread.setContextClassLoader(oldLoader);
                                        }
                                    }
            });
        } catch (PrivilegedActionException pae) {
            Throwable t = pae.getCause();
            if (t instanceof SAXException) {
                throw (SAXException) t;
            } else if (t instanceof ParserConfigurationException) {
                throw (ParserConfigurationException) t;
            } else if (t instanceof FactoryConfigurationError) {
                throw (FactoryConfigurationError) t;
            }
        }
        return null;
    }

    public static XMLReader createXMLReader(String parser) throws SAXException {
        final String parserStr = parser;
        try {
            return (XMLReader) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws SAXException {
                    Thread currThread = Thread.currentThread();
                    ClassLoader oldLoader = currThread.getContextClassLoader();
                    currThread.setContextClassLoader(ParserFactory.class.getClassLoader());
                    try {
                        return XMLReaderFactory.createXMLReader(parserStr);
                    }
                                        finally {
                                            currThread.setContextClassLoader(oldLoader);
                                        }
                                    }
            });
        } catch (PrivilegedActionException pae) {
            Throwable t = pae.getCause();
            if (t instanceof SAXException) {
                throw (SAXException) t;
            }
        }
        return null;
    }

    public static DocumentBuilder newDocumentBuilder(boolean setNamespaceAware, boolean setValidating) throws ParserConfigurationException {
        final boolean setNamespaceAwareBool = setNamespaceAware;
        final boolean setValidatingBool = setValidating;
        try {
            return (DocumentBuilder) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws ParserConfigurationException {
                    Thread currThread = Thread.currentThread();
                    ClassLoader oldLoader = currThread.getContextClassLoader();
                    currThread.setContextClassLoader(ParserFactory.class.getClassLoader());
                    try {
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

                        factory.setNamespaceAware(setNamespaceAwareBool);
                        factory.setValidating(setValidatingBool);

                        DocumentBuilder builder = factory.newDocumentBuilder();
                        return builder;
                    }
                                        finally {
                                            currThread.setContextClassLoader(oldLoader);
                                        }
                                    }
            });
        } catch (PrivilegedActionException pae) {
            Throwable t = pae.getCause();
            if (t instanceof ParserConfigurationException) {
                throw (ParserConfigurationException) t;
            }
        }
        return null;
    }

    public static Document newDocument(DocumentBuilder builder) throws ParserConfigurationException {
        final DocumentBuilder docBuilder = builder;
        try {
            return (Document) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws ParserConfigurationException {
                    Thread currThread = Thread.currentThread();
                    ClassLoader oldLoader = currThread.getContextClassLoader();
                    currThread.setContextClassLoader(ParserFactory.class.getClassLoader());
                    try {
                        return docBuilder.newDocument();
                    }
                                        finally {
                                            currThread.setContextClassLoader(oldLoader);
                                        }
                                    }
            });
        } catch (PrivilegedActionException pae) {
            Throwable t = pae.getCause();
            if (t instanceof ParserConfigurationException) {
                throw (ParserConfigurationException) t;
            }
        }
        return null;
    }

    // PQ76707 begin
    public static Document parseDocument(DocumentBuilder builder, EntityResolver entityResolver, ErrorHandler errorHandler, InputStream is) throws IOException, SAXException {
        final DocumentBuilder docBuilder = builder;
        final EntityResolver eResolver = entityResolver;
        final ErrorHandler errHandler = errorHandler;
        final InputStream iStream = is;
        try {
            return (Document) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws SAXException, IOException {
                    Thread currThread = Thread.currentThread();
                    ClassLoader oldLoader = currThread.getContextClassLoader();
                    currThread.setContextClassLoader(ParserFactory.class.getClassLoader());
                    try {
                        docBuilder.setEntityResolver(eResolver);
                        docBuilder.setErrorHandler(errHandler);
                        return docBuilder.parse(iStream);
                    }
                                        finally {
                                            currThread.setContextClassLoader(oldLoader);
                                        }
                                    }
            });
        } catch (PrivilegedActionException pae) {
            Throwable t = pae.getCause();
            if (t instanceof SAXException) {
                throw (SAXException) t;
            } else if (t instanceof IOException) {
                throw (IOException) t;
            }
        }
        return null;
    }

    // PQ76707 end

    // D190462 - START
    public static Document parseDocument(DocumentBuilder builder, File file) throws IOException, SAXException {
        final DocumentBuilder docBuilder = builder;
        final File parsingFile = file;
        try {
            return (Document) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws SAXException, IOException {
                    Thread currThread = Thread.currentThread();
                    ClassLoader oldLoader = currThread.getContextClassLoader();
                    currThread.setContextClassLoader(ParserFactory.class.getClassLoader());
                    try {
                        return docBuilder.parse(parsingFile);
                    }
                                        finally {
                                            currThread.setContextClassLoader(oldLoader);
                                        }
                                    }
            });
        } catch (PrivilegedActionException pae) {
            Throwable t = pae.getCause();
            if (t instanceof SAXException) {
                throw (SAXException) t;
            } else if (t instanceof IOException) {
                throw (IOException) t;
            }
        }
        return null;
    }
    // D190462 - END
}
