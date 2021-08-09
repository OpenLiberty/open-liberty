/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.utils.metagen;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;

import javax.resource.spi.Activation;
import javax.resource.spi.ConnectionDefinition;
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
import com.ibm.ws.jca.utils.xml.ra.RaActivationSpec;
import com.ibm.ws.jca.utils.xml.ra.RaAdminObject;
import com.ibm.ws.jca.utils.xml.ra.RaConfigProperty;
import com.ibm.ws.jca.utils.xml.ra.RaConnectionDefinition;
import com.ibm.ws.jca.utils.xml.ra.RaConnector;
import com.ibm.ws.jca.utils.xml.ra.RaInboundResourceAdapter;
import com.ibm.ws.jca.utils.xml.ra.RaMessageAdapter;
import com.ibm.ws.jca.utils.xml.ra.RaMessageListener;
import com.ibm.ws.jca.utils.xml.ra.RaOutboundResourceAdapter;
import com.ibm.ws.jca.utils.xml.ra.RaResourceAdapter;
import com.ibm.ws.jca.utils.xml.ra.v10.Ra10Connector;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaActivationSpec;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaAdminObject;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaConfigProperty;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaConnectionDefinition;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaConnector;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaInboundResourceAdapter;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaMessageAdapter;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaMessageListener;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaOutboundResourceAdapter;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaResourceAdapter;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 *
 */
public class DeploymentDescriptorParser {
    private static final TraceComponent tc = Tr.register(DeploymentDescriptorParser.class);

    private static JAXBContext raContext, wlpRaContext, ra10Context = null;

    private static EntityResolver resolver = new org.xml.sax.EntityResolver() {
        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            // do not resolve the external url for the dtd.
            if (systemId != null)
                if (systemId.toLowerCase().endsWith(".dtd") || systemId.toLowerCase().endsWith(".xsd")) {
                    return new org.xml.sax.InputSource(new java.io.StringReader(""));
                }
            return null;
        }
    };

    public synchronized static void init() {
        if (raContext != null)
            return;
        // TODOCJN need to sort this out where it should be, I think it is here to get rid of the linkage errors I was seeing
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws ExceptionInInitializerError {
                    try {
                        raContext = JAXBContext.newInstance(RaConnector.class);
                        wlpRaContext = JAXBContext.newInstance(WlpRaConnector.class);
                        return null;
                    } catch (JAXBException e) {
                        throw new ExceptionInInitializerError(e);
                    }
                }
            });
        } catch (PrivilegedActionException e1) {
            if (e1.getCause() instanceof ExceptionInInitializerError)
                throw (ExceptionInInitializerError) e1.getCause();
            else
                throw new RuntimeException(e1);
        }
    }

    /**
     * This method is used to inspect the ra.xml and check if its a 1.0 RA. This is called only
     * if we fail parsing the ra.xml using JAXB for 1.5/1.6
     *
     * @param xmlStream The ra.xml file
     * @return whether the resource adapter is a 1.0 resource adapter
     */
    public static boolean isVersion10ResourceAdapter(InputStream xmlStream) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            XMLReader parser = factory.newSAXParser().getXMLReader();
            SAXVersionHandler handler = new SAXVersionHandler();
            parser.setEntityResolver(resolver);
            parser.setContentHandler(handler);
            parser.parse(new InputSource(xmlStream));
            return handler.isVersion10ResourceAdapter;
        } catch (SAXException ex) {
            // Check for FFDC
        } catch (ParserConfigurationException e) {
            // Check for FFDC
        } catch (IOException e) {
            // Check for FFDC
        }
        return false;
    }

    /**
     * Converts a ra.xml (or wlp-ra.xml) into a RaConnector/RaConnector10 or WlpRaConnector object.
     *
     * @param xmlStream the stream to the wlp-/ra.xml file
     * @return the parsed xml file
     * @throws SAXException
     * @throws JAXBException
     * @throws ParserConfigurationException
     */
    public static Object parseResourceAdapterXml(InputStream xmlStream, String name, boolean version10) throws JAXBException, SAXException, ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        if (!version10) {
            factory.setNamespaceAware(true);
        }
        factory.setValidating(false);
        SAXParser parser = factory.newSAXParser();
        NamespaceFilter filter = null;
        if (!version10) {
            filter = new NamespaceFilter();
            filter.setParent(parser.getXMLReader());
        }
        parser.getXMLReader().setEntityResolver(resolver);
        Unmarshaller unmarshaller = null;
        if (name.equals("ra.xml") && !version10) {
            unmarshaller = raContext.createUnmarshaller();
        } else if (name.equals("ra.xml") && version10) {
            unmarshaller = ra10Context.createUnmarshaller();
        } else {
            unmarshaller = wlpRaContext.createUnmarshaller();
        }
        SAXSource source = null;
        if (!version10) {
            filter.setContentHandler(unmarshaller.getUnmarshallerHandler());
            source = new SAXSource(filter, new InputSource(xmlStream));
        } else {
            parser.getXMLReader().setContentHandler(unmarshaller.getUnmarshallerHandler());
            source = new SAXSource(parser.getXMLReader(), new InputSource(xmlStream));
        }
        Object connector = unmarshaller.unmarshal(source);
        if (connector instanceof Ra10Connector) {
            RaConnector temp = new RaConnector();
            temp.copyRa10Settings((Ra10Connector) connector);
            connector = temp;
        }

        return connector;
    }

    /**
     * Called for parsing the ra.xml/wlp-ra.xml dd for the resource adapter.
     *
     * @param ddEntry Entry corresponding to the deployment descriptor
     *
     * @throws JAXBException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    @FFDCIgnore(JAXBException.class)
    public static Object parseRaDeploymentDescriptor(com.ibm.wsspi.adaptable.module.Entry ddEntry) throws JAXBException, SAXException, ParserConfigurationException, UnableToAdaptException {
        Object connector = null;
        try {
            connector = parseResourceAdapterXml(ddEntry.adapt(InputStream.class), ddEntry.getName(), false);
        } catch (JAXBException jax) {
            if (isVersion10ResourceAdapter(ddEntry.adapt(InputStream.class))) {
                if (ra10Context == null) {
                    ra10Context = JAXBContext.newInstance(Ra10Connector.class);
                }
                connector = parseResourceAdapterXml(ddEntry.adapt(InputStream.class), ddEntry.getName(), true);
            } else {
                throw jax;
            }
        }
        return connector;
    }

    // TODOCJN raConnector should be the ra.xml combined with annotations and javabeans, if any
    // before being called since it can be used to override any of those?  Need to look for all places where this is
    // invoked.  It's probably ok to merge ra.xml + wlp-ra.xml before doing annotations/javabeans?
    // That will happen in ConnectorAdapter
    /**
     * Combines a converted wlp-ra.xml into a parsed ra.xml
     *
     * @param raConnector the combined parsed ra.xml file, annotations, java bean properties, if any
     * @param wlpRaConnector the parsed wlp-ra.xml file
     * @throw InvalidPropertyException
     * @throws UnavailableException
     */
    public static void combineWlpAndRaXmls(final String adapterName, RaConnector raConnector, WlpRaConnector wlpRaConnector) throws InvalidPropertyException, UnavailableException {
        raConnector.copyWlpSettings(wlpRaConnector);

        WlpRaResourceAdapter wlpResourceAdapter = wlpRaConnector.getResourceAdapter();
        RaResourceAdapter raResourceAdapter = raConnector.getResourceAdapter();

        if (wlpResourceAdapter != null) {
            raResourceAdapter.copyWlpSettings(wlpResourceAdapter);
            // process resourceadapter level config-property
            if (wlpResourceAdapter.getConfigProperties() != null) {
                List<WlpRaConfigProperty> wlpConfigProperties = wlpResourceAdapter.getConfigProperties();
                for (WlpRaConfigProperty wlpConfigProperty : wlpConfigProperties) {
                    if (wlpConfigProperty.addWlpPropertyToMetatype()) {
                        if (raResourceAdapter.isConfigPropertyAlreadyDefined(wlpConfigProperty.getWlpPropertyName()))
                            throw new InvalidPropertyException(Tr.formatMessage(tc, "J2CA9908.duplicate.copy", wlpConfigProperty.getWlpPropertyName(), adapterName));
                        else {
                            RaConfigProperty property = new RaConfigProperty();
                            property.copyWlpSettings(wlpConfigProperty);
                            raResourceAdapter.getConfigProperties().add(property);
                        }
                    } else {
                        RaConfigProperty raConfigProperty = raResourceAdapter.getConfigPropertyById(wlpConfigProperty.getWlpPropertyName());
                        if (raConfigProperty == null)
                            throw new UnavailableException(Tr.formatMessage(tc, "J2CA9909.missing.matching.config.prop", wlpConfigProperty.getWlpPropertyName(), adapterName));
                        else
                            raConfigProperty.copyWlpSettings(wlpConfigProperty);
                    }
                }
            }

            // process resourceadapter level adminobject
            List<WlpRaAdminObject> wlpAdminObjects = wlpResourceAdapter.getAdminObjects();
            if (wlpAdminObjects != null) {
                for (WlpRaAdminObject wlpAdminObject : wlpAdminObjects) {
                    RaAdminObject raAdminObject = raResourceAdapter.getAdminObject(wlpAdminObject.getAdminObjectInterface(), wlpAdminObject.getAdminObjectClass());
                    if (raAdminObject == null)
                        throw new UnavailableException(Tr.formatMessage(tc, "J2CA9910.missing.matching.adminobject",
                                                                        wlpAdminObject.getAdminObjectInterface(), wlpAdminObject.getAdminObjectClass(), adapterName));
                    else {
                        raAdminObject.copyWlpSettings(wlpAdminObject);
                        if (wlpAdminObject.getConfigProperties() != null) {
                            List<WlpRaConfigProperty> wlpConfigProperties = wlpAdminObject.getConfigProperties();
                            for (WlpRaConfigProperty wlpConfigProperty : wlpConfigProperties) {
                                if (wlpConfigProperty.addWlpPropertyToMetatype()) {
                                    if (raAdminObject.isConfigPropertyAlreadyDefined(wlpConfigProperty.getWlpPropertyName()))
                                        throw new InvalidPropertyException(Tr.formatMessage(tc, "J2CA9908.duplicate.copy", wlpConfigProperty.getWlpPropertyName(), adapterName));
                                    else {
                                        RaConfigProperty property = new RaConfigProperty();
                                        property.copyWlpSettings(wlpConfigProperty);
                                        raAdminObject.getConfigProperties().add(property);
                                    }
                                } else {
                                    RaConfigProperty raConfigProperty = raAdminObject.getConfigPropertyById(wlpConfigProperty.getWlpPropertyName());
                                    if (raConfigProperty == null)
                                        throw new UnavailableException(Tr.formatMessage(tc, "J2CA9909.missing.matching.config.prop", wlpConfigProperty.getWlpPropertyName(),
                                                                                        adapterName));
                                    else
                                        raConfigProperty.copyWlpSettings(wlpConfigProperty);
                                }
                            }
                        }
                    }
                }
            }

            // process outbound-resourceadapter
            WlpRaOutboundResourceAdapter wlpOutboundAdapter = wlpResourceAdapter.getOutboundResourceAdapter();
            if (wlpOutboundAdapter != null) {
                RaOutboundResourceAdapter raOutboundAdapter = raResourceAdapter.getOutboundResourceAdapter();
                if (wlpOutboundAdapter.getConnectionDefinitions() != null) {
                    List<WlpRaConnectionDefinition> wlpConnectionDefinitions = wlpOutboundAdapter.getConnectionDefinitions();
                    for (WlpRaConnectionDefinition wlpConnectionDefinition : wlpConnectionDefinitions) {
                        RaConnectionDefinition raConnectionDefinition = raOutboundAdapter.getConnectionDefinitionByInterface(wlpConnectionDefinition.getConnectionFactoryInterface());
                        if (raConnectionDefinition == null)
                            throw new UnavailableException(Tr.formatMessage(tc, "J2CA9911.missing.matching.type",
                                                                            "connection-definition", wlpConnectionDefinition.getConnectionFactoryInterface(),
                                                                            ConnectionDefinition.class.getSimpleName(), adapterName));
                        else {
                            raConnectionDefinition.copyWlpSettings(wlpConnectionDefinition);
                            if (wlpConnectionDefinition.getConfigProperties() != null) {
                                List<WlpRaConfigProperty> wlpConfigProperties = wlpConnectionDefinition.getConfigProperties();
                                // process connection-definition config-property
                                for (WlpRaConfigProperty wlpConfigProperty : wlpConfigProperties) {
                                    if (wlpConfigProperty.addWlpPropertyToMetatype()) {
                                        if (raConnectionDefinition.isConfigPropertyAlreadyDefined(wlpConfigProperty.getWlpPropertyName()))
                                            throw new InvalidPropertyException(Tr.formatMessage(tc, "J2CA9908.duplicate.copy", wlpConfigProperty.getWlpPropertyName(),
                                                                                                adapterName));
                                        else {
                                            RaConfigProperty property = new RaConfigProperty();
                                            property.copyWlpSettings(wlpConfigProperty);
                                            raConnectionDefinition.getConfigProperties().add(property);
                                        }
                                    } else {
                                        RaConfigProperty raConfigProperty = raConnectionDefinition.getConfigPropertyById(wlpConfigProperty.getWlpPropertyName());
                                        if (raConfigProperty == null)
                                            throw new UnavailableException(Tr.formatMessage(tc, "J2CA9909.missing.matching.config.prop", wlpConfigProperty.getWlpPropertyName(),
                                                                                            adapterName));
                                        else
                                            raConfigProperty.copyWlpSettings(wlpConfigProperty);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // process inbound-resourceadapter
            WlpRaInboundResourceAdapter wlpInboundAdapter = wlpResourceAdapter.getInboundResourceAdapter();
            if (wlpInboundAdapter != null) {
                RaInboundResourceAdapter raInboundAdapter = raResourceAdapter.getInboundResourceAdapter();
                // process messageadapter
                WlpRaMessageAdapter wlpMessageAdapter = wlpInboundAdapter.getMessageAdapter();
                if (wlpMessageAdapter != null) {
                    if (wlpMessageAdapter.getMessageListeners() != null) {
                        RaMessageAdapter raMessageAdapter = raInboundAdapter.getMessageAdapter();
                        List<WlpRaMessageListener> wlpMessageListeners = wlpMessageAdapter.getMessageListeners();
                        for (WlpRaMessageListener wlpMessageListener : wlpMessageListeners) {
                            RaMessageListener raMessageListener = raMessageAdapter == null ? null : raMessageAdapter.getMessageListenerByType(wlpMessageListener.getMessageListenerType());
                            if (raMessageListener == null)
                                throw new UnavailableException(Tr.formatMessage(tc, "J2CA9911.missing.matching.type",
                                                                                "messagelistener", wlpMessageListener.getMessageListenerType(),
                                                                                Activation.class.getSimpleName(), adapterName));
                            else {
                                raMessageListener.copyWlpSettings(wlpMessageListener);
                                if (wlpMessageListener.getActivationSpec() != null) {
                                    WlpRaActivationSpec wlpActivationSpec = wlpMessageListener.getActivationSpec();
                                    RaActivationSpec raActivationSpec = raMessageListener.getActivationSpec();
                                    if (raActivationSpec == null)
                                        throw new UnavailableException(Tr.formatMessage(tc, "J2CA9911.missing.matching.type",
                                                                                        "activationspec", raMessageListener.getMessageListenerType(),
                                                                                        Activation.class.getSimpleName(), adapterName));
                                    else if (wlpActivationSpec.getConfigProperties() != null) {
                                        List<WlpRaConfigProperty> wlpConfigProperties = wlpActivationSpec.getConfigProperties();
                                        for (WlpRaConfigProperty wlpConfigProperty : wlpConfigProperties) {
                                            if (wlpConfigProperty.addWlpPropertyToMetatype())
                                                if (raActivationSpec.isConfigPropertyAlreadyDefined(wlpConfigProperty.getWlpPropertyName()))
                                                    throw new InvalidPropertyException(Tr.formatMessage(tc, "J2CA9908.duplicate.copy", wlpConfigProperty.getWlpPropertyName(),
                                                                                                        adapterName));
                                                else {
                                                    RaConfigProperty property = new RaConfigProperty();
                                                    property.copyWlpSettings(wlpConfigProperty);
                                                    raActivationSpec.getConfigProperties().add(property);
                                                }
                                            else {
                                                RaConfigProperty raConfigProperty = raActivationSpec.getConfigPropertyById(wlpConfigProperty.getWlpPropertyName());
                                                if (raConfigProperty == null)
                                                    throw new UnavailableException(Tr.formatMessage(tc, "J2CA9909.missing.matching.config.prop",
                                                                                                    wlpConfigProperty.getWlpPropertyName(),
                                                                                                    adapterName));
                                                else
                                                    raConfigProperty.copyWlpSettings(wlpConfigProperty);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Trivial
    static class NamespaceFilter extends XMLFilterImpl {
        @Override
        public void startElement(String namespaceURI, String localName, String qualifiedName, Attributes attributes) throws SAXException {
            String JCA15NamespaceURI = "http://java.sun.com/xml/ns/j2ee";
            String JCA16NamespaceURI = "http://java.sun.com/xml/ns/javaee";
            String JCA17NamespaceURI = "http://xmlns.jcp.org/xml/ns/javaee";
            String Connectors20NamespaceURI = "https://jakarta.ee/xml/ns/jakartaee";
            namespaceURI = namespaceURI.trim().toLowerCase().intern(); // on zOS it is required that Namespace URIs are interned
            //Convert the older namespaces as we need to process for multiple namespaces with the same objects.
            if (namespaceURI.equals(JCA15NamespaceURI) || namespaceURI.equals(JCA16NamespaceURI) || namespaceURI.contentEquals(JCA17NamespaceURI)) {
                super.startElement(Connectors20NamespaceURI, localName, qualifiedName, attributes);
            } else {
                super.startElement(namespaceURI, localName, qualifiedName, attributes);
            }
        }
    }

    // Handler for the SAX Parser to check version.
    @Trivial
    static class SAXVersionHandler extends DefaultHandler {
        public boolean isVersion10ResourceAdapter = false;
        boolean isVersion = false;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equalsIgnoreCase("spec-version"))
                isVersion = true;
        }

        @Override
        public void characters(char ch[], int start, int length) throws SAXException {
            if (isVersion) {
                String version = new String(ch, start, length);
                if ("1.0".equals(version)) {
                    isVersion10ResourceAdapter = true;
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equalsIgnoreCase("spec-version"))
                isVersion = false;
        }

    }
}
