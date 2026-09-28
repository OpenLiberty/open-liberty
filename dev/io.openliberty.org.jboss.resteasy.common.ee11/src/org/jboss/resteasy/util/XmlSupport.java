/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.util;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;

import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.jboss.resteasy.spi.ResteasyConfiguration;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * A utility for creating secure XML serialization and deserialization resources.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class XmlSupport {
    private static final Set<String> ALREADY_LOGGED = ConcurrentHashMap.newKeySet(2);

    private XmlSupport() {
    }

    /**
     * Configures the {@linkplain SAXParserFactory factory} for secure processing. If the
     * {@linkplain ResteasyConfiguration configuration} is not {@code null}, the following properties are looked up
     * to determine which features are enabled/disabled:
     * <ul>
     * <li>{@link ResteasyContextParameters#RESTEASY_EXPAND_ENTITY_REFERENCES}: Determines whether to enable or disable
     * {@code http://xml.org/sax/features/external-general-entities} and
     * {@code http://xml.org/sax/features/external-parameter-entities}. Defaults to {@code false}.</li>
     * <li>{@link ResteasyContextParameters#RESTEASY_SECURE_PROCESSING_FEATURE}: Determines whether to enable or disable
     * {@link XMLConstants#FEATURE_SECURE_PROCESSING}. Defaults to {@code true}.</li>
     * <li>{@link ResteasyContextParameters#RESTEASY_DISABLE_DTDS}: Determines whether to enable or disable
     * {@code http://apache.org/xml/features/disallow-doctype-decl}. Defaults to {@code true}.</li>
     * </ul>
     *
     * <p>
     * If any of these properties are not supported by the {@link SAXParserFactory} implementation, a
     * {@link SAXNotSupportedException} will be thrown.
     * </p>
     *
     * @param factory the factory to be configured
     * @param config  the configuration to be used to override the default values
     *
     * @throws SAXNotSupportedException     if the property is not supported by the underlying implementation
     * @throws SAXNotRecognizedException    if the property is not recognized by the underlying implementation
     * @throws ParserConfigurationException if there is an error configuring the parser
     * @see SAXParserFactory
     */
    public static void configureParserFactory(final SAXParserFactory factory, final ResteasyConfiguration config)
            throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
        final XmlSecurityConfiguration securityConfiguration = resolveConfiguration(config);
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, securityConfiguration.enableSecureProcessingFeature());
        factory.setFeature("http://xml.org/sax/features/external-general-entities",
                securityConfiguration.expandEntityReferences());
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities",
                securityConfiguration.expandEntityReferences());
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", securityConfiguration.disableDTDs());
    }

    /**
     * Creates a new {@link XMLReader} from the default {@link SAXParserFactory}. The {@code SAXParserFactory} is
     * configured via the {@link #configureParserFactory(SAXParserFactory, ResteasyConfiguration)}.
     *
     * <p>
     * The configuration is looked up in the current {@linkplain ResteasyContext context}.
     * </p>
     *
     * @return a new XMLReader
     *
     * @throws ParserConfigurationException if the configuration for the {@link SAXParserFactory} fails
     * @throws SAXException                 if there is a general failure creating the reader
     * @see #configureParserFactory(SAXParserFactory, ResteasyConfiguration)
     */
    public static XMLReader newXmlReader() throws ParserConfigurationException, SAXException {
        return newXmlReader(ResteasyContext.getContextData(ResteasyConfiguration.class));
    }

    /**
     * Creates a new {@link XMLReader} from the default {@link SAXParserFactory}. The {@code SAXParserFactory} is
     * configured via the {@link #configureParserFactory(SAXParserFactory, ResteasyConfiguration)}.
     *
     * @param config the optional configuration to use
     *
     * @return a new XMLReader
     *
     * @throws ParserConfigurationException if the configuration for the {@link SAXParserFactory} fails
     * @throws SAXException                 if there is a general failure creating the reader
     * @see #configureParserFactory(SAXParserFactory, ResteasyConfiguration)
     */
    public static XMLReader newXmlReader(final ResteasyConfiguration config) throws ParserConfigurationException, SAXException {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        configureParserFactory(factory, config);
        return factory.newSAXParser().getXMLReader();
    }

    /**
     * Creates a new {@link TransformerFactory} and configures it for secure processing.
     *
     * <p>
     * The configuration is looked up in the current {@linkplain ResteasyContext context}.
     * </p>
     *
     * @return a newly configured transformer factory
     *
     * @throws TransformerConfigurationException if an error occurs configuring the transformer factory
     */
    public static TransformerFactory newTransformerFactory() throws TransformerConfigurationException {
        return newTransformerFactory(ResteasyContext.getContextData(ResteasyConfiguration.class));
    }

    /**
     * Creates a new {@link TransformerFactory} and configures it for secure processing.
     *
     * <p>
     * The {@link ResteasyContextParameters#RESTEASY_SECURE_PROCESSING_FEATURE}: Determines whether to enable or disable
     * {@link XMLConstants#FEATURE_SECURE_PROCESSING}. Defaults to {@code true}.
     * </p>
     *
     * <p>
     * This also attempts to set two attributes which are not configurable:
     * <ul>
     * <li>{@link XMLConstants#ACCESS_EXTERNAL_DTD}</li>
     * <li>{@link XMLConstants#ACCESS_EXTERNAL_STYLESHEET}</li>
     * </ul>
     *
     * Both attributes are set to empty string values if they are supported by the underlying implementation. If they
     * are not supported, a warning will be logged once indicating the attribute is not supported.
     * </p>
     *
     * @param config the optional configuration to check for the
     *               {@link ResteasyContextParameters#RESTEASY_SECURE_PROCESSING_FEATURE}
     *
     * @return a newly configured transformer factory
     *
     * @throws TransformerConfigurationException if an error occurs configuring the transformer factory
     */
    public static TransformerFactory newTransformerFactory(final ResteasyConfiguration config)
            throws TransformerConfigurationException {
        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        final XmlSecurityConfiguration securityConfiguration = resolveConfiguration(config);
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,
                securityConfiguration.enableSecureProcessingFeature());
        setAttribute(transformerFactory, XMLConstants.ACCESS_EXTERNAL_DTD, "");
        setAttribute(transformerFactory, XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        return transformerFactory;
    }

    private static XmlSecurityConfiguration resolveConfiguration(final ResteasyConfiguration config) {
        boolean expandEntityReferences = false;
        boolean enableSecureProcessingFeature = true;
        boolean disableDTDs = true;
        if (config != null) {
            try {
                final String s = config.getParameter(ResteasyContextParameters.RESTEASY_EXPAND_ENTITY_REFERENCES);
                expandEntityReferences = Boolean.parseBoolean(s);
            } catch (Exception e) {
                LogMessages.LOGGER.unableToRetrieveConfigExpand();
            }
            try {
                final String s = config.getParameter(ResteasyContextParameters.RESTEASY_SECURE_PROCESSING_FEATURE);
                enableSecureProcessingFeature = (s == null || Boolean.parseBoolean(s));
            } catch (Exception e) {
                LogMessages.LOGGER.unableToRetrieveConfigSecure();
            }
            try {
                final String s = config.getParameter(ResteasyContextParameters.RESTEASY_DISABLE_DTDS);
                disableDTDs = (s == null || Boolean.parseBoolean(s));
            } catch (Exception e) {
                LogMessages.LOGGER.unableToRetrieveConfigDTDs();
            }
        }
        return new XmlSecurityConfiguration(expandEntityReferences, enableSecureProcessingFeature, disableDTDs);
    }

    private static void setAttribute(final TransformerFactory transformerFactory, final String name, final String value) {
        try {
            transformerFactory.setAttribute(name, value);
        } catch (IllegalArgumentException e) {
            if (ALREADY_LOGGED.add(name)) {
                LogMessages.LOGGER.transformerPropertyNotSupported(e, name, value);
            }
        }
    }

    private record XmlSecurityConfiguration(boolean expandEntityReferences, boolean enableSecureProcessingFeature,
            boolean disableDTDs) {
    }
}
