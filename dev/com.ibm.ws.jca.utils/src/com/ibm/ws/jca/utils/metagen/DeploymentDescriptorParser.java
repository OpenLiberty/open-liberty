/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.utils.metagen;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.UnavailableException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;

import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLFilterImpl;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jca.utils.xml.ra.RaConnector;
import com.ibm.ws.jca.utils.xml.ra.v10.Ra10Connector;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaConnector;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

public class DeploymentDescriptorParser {
    @SuppressWarnings("unused")
    private static final TraceComponent tc = Tr.register(DeploymentDescriptorParser.class);

    public static final String jca15NamespaceURI = "http://java.sun.com/xml/ns/j2ee";
    public static final String jca16NamespaceURI = "http://java.sun.com/xml/ns/javaee";
    public static final String jca17NamespaceURI = "http://xmlns.jcp.org/xml/ns/javaee";
    public static final String connectors20NamespaceURI = "https://jakarta.ee/xml/ns/jakartaee";     
    
    // JCA 1.0 - J2EE Version 1.3
    // JCA 1.5 - J2EE Version 1.4
    // JCA 1.6 - Java EE Version 6
    // JCA 1.7 - Java EE Version 7?
    // JCA 2.0 - Jakarta 9
    // JCA 2.1 - Jakarta 10

    /**
     * Custom stub entity resolver: Resolve all DTD and XSD entity references
     * as empty streams. Answer null (fail) all other entity references.
     */
    private static EntityResolver stubResolver = new org.xml.sax.EntityResolver() {
        @SuppressWarnings("unused")
        @Override
        public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {

            if ( systemId != null ) {
                systemId = systemId.toLowerCase();
                if ( systemId.endsWith(".dtd") || systemId.endsWith(".xsd") ) {
                    return new InputSource( new StringReader("") );
                }
            }
            return null;
        }
    };

    // The RA 10 context is not created unless and until
    // a version 1.0 descriptor is parsed.

    private static JAXBContext raContext;
    private static JAXBContext wlpRaContext;
    
    private static Object ra10ContextLock = new Object();
    private static JAXBContext ra10Context;

    private static JAXBContext getRA10Context() throws JAXBException {
        synchronized ( ra10ContextLock ) {
            if ( ra10Context == null ) {
                ra10Context = JAXBContext.newInstance(Ra10Connector.class);                
            }
            return ra10Context;
        }
    }
    
    public synchronized static void init() {
        if ( raContext != null ) {
            return;
        }

        // TODO: CJN:
        //
        // Need to sort this out where this should be.
        // I think it needs to be here to avoid linkage errors.

        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws ExceptionInInitializerError {
                    JAXBContext useRaContext;
                    JAXBContext useWlpRaContext;

                    try {
                        useRaContext = JAXBContext.newInstance(RaConnector.class);
                        useWlpRaContext = JAXBContext.newInstance(WlpRaConnector.class);
                    } catch ( JAXBException e ) {
                        throw new ExceptionInInitializerError(e);
                    }

                    raContext = useRaContext;
                    wlpRaContext = useWlpRaContext;

                    return null;
                }
            });

        } catch ( PrivilegedActionException e1 ) {
            if ( e1.getCause() instanceof ExceptionInInitializerError ) {
                throw (ExceptionInInitializerError) e1.getCause();
            } else {
                throw new RuntimeException(e1);
            }
        }
    }

    /**
     * Tell if the specification version of a resource adapter descriptor
     * (ra.xml) is 1.0.
     *
     * This is expensive, and is only done if a parse using a later version fails.
     *
     * @param xmlStream A stream opened on a resource adapter descriptor.
     * @return True or false telling if the descroptor version is 1.0.
     */
    @FFDCIgnore(ParseCompletedException.class)
    public static boolean isVersion10ResourceAdapter(InputStream xmlStream) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);

            XMLReader parser = factory.newSAXParser().getXMLReader();
            parser.setEntityResolver(stubResolver);
            parser.setContentHandler( new SAXVersionHandler() );
            parser.parse( new InputSource(xmlStream) );

            return false;

        } catch ( ParseCompletedException e ) {
            return e.isVersion10; // FFDCIgnore

        } catch ( SAXException | ParserConfigurationException | IOException e ) {
            return false; // AutoFFDC
        }
    }

    /**
     * Used to complete parsing as soon as the version has been read. 
     */
    private static class ParseCompletedException extends SAXException {
        private static final long serialVersionUID = 1L;

        public final boolean isVersion10;

        public ParseCompletedException(boolean isVersion10) {
            this.isVersion10 = isVersion10;
        }
    }

    /**
     * Handler used to read the specification version of
     * a resource adapter.
     *
     * This handler throws a <@link ParseCompletedException>
     * immediately after parsing the text of the "spec-version"
     * element, or immediately after completing the "spec-version"
     * element, in case no text was provided.
     *
     * The thrown completion exception encapsulates whether the
     * version was "1.0".  False is stored if no version text was
     * handled.
     *
     * Use of non-local control flow is to avoid processing the entire
     * document.  SAX processing does not provide a convenient way
     * to terminate parsing.
     * 
     * This handler implementation works only for resource descriptors,
     * which are expected to have at most one "spec-version" element,
     * which specifies the specification version of the document.
     */
    @Trivial
    private static class SAXVersionHandler extends DefaultHandler {
        private boolean inSpecVersion;

        @SuppressWarnings("unused")
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if ( qName.equalsIgnoreCase("spec-version") ) {
                inSpecVersion = true;
            }
        }

        @Override
        @FFDCIgnore(ParseCompletedException.class)        
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if ( qName.equalsIgnoreCase("spec-version") ) {
                inSpecVersion = false;
                throw new ParseCompletedException(false);
            }
        }

        @Override
        @FFDCIgnore(ParseCompletedException.class)        
        public void characters(char ch[], int start, int length) throws SAXException {
            if ( inSpecVersion ) {
                String versionStr = new String(ch, start, length);
                throw new ParseCompletedException( "1.0".equals(versionStr) );
            }
        }
    }

    public static final boolean IS_VERSION_10 = true;

    /**
     * Create a RaConnector or WlpRaConnector from an input stream for
     * a resource adapter "ra.xml" "wlp-ra.xml" resources.
     * 
     * If parsing creates an RaConnector10, convert that into an RaConnector.
     * 
     * RaConnector implements Connector.  WlpRaConnector does not implement
     * connector.  Object is the only common super-type of the two return types.
     * 
     * @param xmlStream Stream containing XML format data, usually from a
     *     "ra.xml" or "wlp-ra.xml" resource.
     * @param name The name of the resource.  Should be "ra.xml" or "wlp-ra.xml".
     * @param isVersion10 Control parameter.  Tells if the resource is to be
     *     parsed using the 1.0 specification rules.  This is only attempted if
     *     parsing using post-1.0 specification rules fails. 
     *     
     * @throws JAXBException Thrown if parsing fails.
     * @throws SAXException Thrown if parsing fails.
     * @throws ParserConfigurationException Thrown if parsing fails.
     */
    @SuppressWarnings("null")
    public static Object parseResourceAdapterXml(
        InputStream xmlStream, String name,
        boolean isVersion10)
        throws JAXBException, SAXException, ParserConfigurationException {

        SAXParserFactory factory = SAXParserFactory.newInstance();
        if ( !isVersion10 ) {
            factory.setNamespaceAware(true);
        }
        factory.setValidating(false);

        SAXParser parser = factory.newSAXParser();

        NamespaceFilter filter = null;
        if ( !isVersion10 ) {
            filter = new NamespaceFilter();
            filter.setEntityResolver(stubResolver);
            filter.setParent( parser.getXMLReader() );
        }

        parser.getXMLReader().setEntityResolver(stubResolver);

        JAXBContext context =
            ( name.equals("ra.xml") ? (isVersion10 ? getRA10Context() : raContext)
                                    : wlpRaContext );
        Unmarshaller unmarshaller = context.createUnmarshaller();
        
        SAXSource source;
        if ( !isVersion10 ) {
            filter.setContentHandler( unmarshaller.getUnmarshallerHandler() );
            source = new SAXSource( filter, new InputSource(xmlStream) );
        } else {
            parser.getXMLReader().setContentHandler( unmarshaller.getUnmarshallerHandler() );
            source = new SAXSource( parser.getXMLReader(), new InputSource(xmlStream) );
        }

        Object connector = unmarshaller.unmarshal(source);
        if ( connector instanceof Ra10Connector ) {
            RaConnector raConnector = new RaConnector();
            raConnector.copyRa10Settings((Ra10Connector) connector);
            connector = raConnector;
        }
        // Or, is a RaConnector already, or is a WlpRaConnector.

        return connector;
    }

    /**
     * Parse an entry as an XML serialized connector.
     *
     * The return type depends on the entry name: "ra.xml" parses as
     * <@link RaConnector>.  "wlp-ra.xml" parses as <@link WlpRaConnector>.
     *
     * A descriptor which uses the 1.0 specification is parsed as
     * <@link Ra10Connector>.  That is converted to <@link RaConnector> as
     * the return value.
     *
     * Parsing is attempted first using the post 1.0 specifications.  Only
     * if that first parse attempt fails is a parse of the 1.0 specification
     * attempt.
     *
     * @param ddEntry The entry of the deployment descriptor.
     * 
     * @throws JAXBException, SAXException, ParserConfigurationException
     *     Thrown if parsing fails.  Usually, because the descriptor XML
     *     text is not valid.
     * @throws UnableToAdaptException Thrown if there was a problem reading
     *     the XML resource.
     */
    // FIXME - Throwable shouldn't be necessary, but is a work around for the following issue: https://github.com/OpenLiberty/open-liberty/issues/22347
    @FFDCIgnore({ JAXBException.class, Throwable.class})
    public static Object parseRaDeploymentDescriptor(Entry ddEntry)
        throws JAXBException, SAXException, ParserConfigurationException, UnableToAdaptException {

        String ddName = ddEntry.getName();

        try {
            try ( InputStream ddStream = ddEntry.adapt(InputStream.class) ) {
                return parseResourceAdapterXml(ddStream, ddName, !IS_VERSION_10);
            } catch ( IOException e ) {
                throw new UnableToAdaptException(e);
            }

        } catch ( JAXBException jax ) {
            boolean isVersion10;
            try ( InputStream ddStream = ddEntry.adapt(InputStream.class) ) {
                isVersion10 = isVersion10ResourceAdapter(ddStream);
            } catch ( IOException e ) {
                throw new UnableToAdaptException(e);
            }
            if ( !isVersion10 ) {
                throw jax;
            }

            try ( InputStream ddStream = ddEntry.adapt(InputStream.class) ) {
                return parseResourceAdapterXml(ddStream, ddName, IS_VERSION_10);
            } catch ( IOException e ) {
                throw new UnableToAdaptException(e);
            }
        }
    }

    @Deprecated // Use DeploymentDescriptorMerger::merge directly.
    public static void combineWlpAndRaXmls(
        String adapterName,
        RaConnector raConnector,
        WlpRaConnector wlpRaConnector) throws InvalidPropertyException, UnavailableException {

        DeploymentDescriptorMerger.merge(adapterName, raConnector, wlpRaConnector);
    }

    @Trivial
    static class NamespaceFilter extends XMLFilterImpl {
        /**
         * Override: Change the namespace URI to the current maximum supported
         * namespace URI.  That is the connectors 2.0 namespace URI, which also
         * is the connetors 2.1 namespace URI.
         * 
         * This means that connector parsing is always enabled to the highest
         * supported specification, regardless of which specification version
         * is provisioned.  Any uplevel data values will be read, and will
         * be ignored.
         */
        @Override
        public void startElement(
                String namespaceURI,
                String localName, String qualifiedName,
                Attributes attributes)
            throws SAXException {

            // On zOS it is required that Namespace URIs be interned.
            // TODO: Why??

            namespaceURI = namespaceURI.trim().toLowerCase().intern();

            // Convert the older namespaces as we need to process for multiple namespaces
            // with the same objects.

            if ( namespaceURI.equals(jca15NamespaceURI) ||
                 namespaceURI.equals(jca16NamespaceURI) ||
                 namespaceURI.contentEquals(jca17NamespaceURI) ) {
                namespaceURI = connectors20NamespaceURI;
            }

            super.startElement(namespaceURI, localName, qualifiedName, attributes);
        }
    }
}
