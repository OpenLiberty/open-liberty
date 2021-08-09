/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.springboot.internal;

import java.io.IOException;
import java.io.Writer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.ibm.ws.app.manager.springboot.container.config.ConfigElement;
import com.ibm.ws.app.manager.springboot.container.config.HttpEndpoint;
import com.ibm.ws.app.manager.springboot.container.config.HttpOptions;
import com.ibm.ws.app.manager.springboot.container.config.KeyEntry;
import com.ibm.ws.app.manager.springboot.container.config.KeyStore;
import com.ibm.ws.app.manager.springboot.container.config.SSLConfig;
import com.ibm.ws.app.manager.springboot.container.config.ServerConfiguration;
import com.ibm.ws.app.manager.springboot.container.config.SslOptions;
import com.ibm.ws.app.manager.springboot.container.config.VirtualHost;

/**
 * Reads server.xml into memory, writes changes back to server.xml
 *
 */
public class ServerConfigurationWriter {

    private static ServerConfigurationWriter INSTANCE;

    public static ServerConfigurationWriter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ServerConfigurationWriter();
        }
        return INSTANCE;
    }

    /**
     * Expresses a server configuration in an XML document.
     *
     * @param sourceConfig
     *                         the configuration you want to marshal
     * @param outputStream
     *                         the stream where you want to marshal state information. the
     *                         stream will be closed before this method returns.
     * @throws XMLStreamException
     *                                on StAX failure
     * @throws IOException
     *                                on IO failure
     *
     */

    private final static String SERVER_DEFAULT_ENCODING = "UTF-8";

    private final String encoding = SERVER_DEFAULT_ENCODING;

    private XMLStreamWriter xmlStreamWriter;
    private Writer writer;

    public void write(ServerConfiguration sourceConfig, Writer writer) throws XMLStreamException, IOException {
        this.writer = writer;
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        this.xmlStreamWriter = factory.createXMLStreamWriter(writer);
        this.xmlStreamWriter.writeStartDocument(encoding, "1.0");

        indent(0);
        this.xmlStreamWriter.writeStartElement(ServerConfiguration.XML_ELEMENT_NAME_SERVER);

        writeVirtualHost(sourceConfig);
        writeSsl(sourceConfig);
        writeHttpEndpoint(sourceConfig);

        indent(0);
        this.xmlStreamWriter.writeEndElement(); // SERVER

        xmlStreamWriter.writeEndDocument();
        xmlStreamWriter.close();
        writer.close();
    }

    /**
     * @param i
     * @throws IOException
     * @throws XMLStreamException
     */
    void indent(int count) throws IOException, XMLStreamException {
        xmlStreamWriter.flush();
        // We are trying to write good looking indented XML. The IBM JDK's XMLStreamWriter
        // will entity encode a \r character on windows so we can't write the line separator
        // on an IBM JDK. On a Sun JDK it doesn't entity encode \r, but if we use the writer to
        // write the line separator the end  > of the XML element ends up on the next line.
        // So instead we write a single space to the xmlWriter which causes on all JDKs the
        // element to be closed. We write the line separator using the writer, and the remaining
        // characters using the xml stream writer. Very hacky, but seems to work.
        xmlStreamWriter.writeCharacters(" ");
        writer.write(getLineSeparator());
        for (int i = 0; i < count; i++) {
            xmlStreamWriter.writeCharacters("    ");
        }

    }

    private String LINE_SEPARATOR = null;

    String getLineSeparator() {
        if (null == LINE_SEPARATOR) {
            String ls = (String) AccessController.doPrivileged((PrivilegedAction<Object>) (() -> System.getProperty("line.separator")));
            LINE_SEPARATOR = null != ls && (ls.equals("\n") || ls.equals("\r") || ls.equals("\r\n")) ? ls : "\n";
        }
        return LINE_SEPARATOR;
    }

    void writeVirtualHost(ServerConfiguration sourceConfig) throws XMLStreamException, IOException {
        List<VirtualHost> virtualHosts = sourceConfig.getVirtualHosts(); // size = 1
        if (virtualHosts != null && virtualHosts.size() > 0) {
            VirtualHost vh = virtualHosts.get(0);

            indent(1);
            xmlStreamWriter.writeStartElement(ServerConfiguration.XML_ELEMENT_NAME_VIRTUAL_HOST);
            if (vh.getAllowFromEndpointRef() != null && !vh.getAllowFromEndpointRef().isEmpty()) {
                xmlStreamWriter.writeAttribute(VirtualHost.XML_ATTRIBUTE_NAME_ALLOW_FROM_ENDPOINT_REF, vh.getAllowFromEndpointRef());
            }
            xmlStreamWriter.writeAttribute(ConfigElement.XML_ATTRIBUTE_NAME_ID, vh.getId());

            for (String ha : vh.getHostAliases()) {
                if (ha != null && !"".equals(ha)) {

                    indent(2);
                    xmlStreamWriter.writeStartElement(VirtualHost.XML_ELEMENT_NAME_HOST_ALIAS);
                    xmlStreamWriter.writeCharacters(ha);
                    xmlStreamWriter.writeEndElement(); // HOST_ALIAS
                }
            }

            indent(1);
            xmlStreamWriter.writeEndElement(); // VIRTUAL_HOST
        }
    }

    void writeHttpEndpoint(ServerConfiguration sourceConfig) throws XMLStreamException, IOException {
        List<HttpEndpoint> endpoints = sourceConfig.getHttpEndpoints(); // size = 1
        if (endpoints != null && endpoints.size() > 0) {
            HttpEndpoint ep = endpoints.get(0);

            indent(1);
            xmlStreamWriter.writeStartElement(ServerConfiguration.XML_ELEMENT_NAME_HTTP_ENDPOINT);

            xmlStreamWriter.writeAttribute(HttpEndpoint.XML_ATTRIBUTE_NAME_HOST, ep.getHost());
            xmlStreamWriter.writeAttribute(HttpEndpoint.XML_ATTRIBUTE_NAME_HTTP_PORT, ep.getHttpPort().toString());
            xmlStreamWriter.writeAttribute(HttpEndpoint.XML_ATTRIBUTE_NAME_HTTPS_PORT, ep.getHttpsPort().toString());
            String protocolVersion = ep.getProtocolVersion();
            if (protocolVersion != null) {
                xmlStreamWriter.writeAttribute(HttpEndpoint.XML_ATTRIBUTE_NAME_PROTOCOL_VERSION, ep.getProtocolVersion());
            }
            xmlStreamWriter.writeAttribute(ConfigElement.XML_ATTRIBUTE_NAME_ID, ep.getId());

            HttpOptions httpOptions = ep.getHttpOptions();
            if (httpOptions != null) {
                String serverHeader = httpOptions.getServerHeaderValue();
                if (serverHeader != null) {

                    indent(2);
                    xmlStreamWriter.writeStartElement(HttpEndpoint.XML_ELEMENT_NAME_HTTP_OPTIONS);
                    xmlStreamWriter.writeAttribute(HttpOptions.XML_ATTRIBUTE_NAME_SERVER_HEADER_VALUE, serverHeader);
                    xmlStreamWriter.writeEndElement(); // HTTP_OPTIONS
                }
            }

            SslOptions sslOptions = ep.getSslOptions();
            if (sslOptions != null) {
                String sslRef = sslOptions.getSslRef();
                if (sslRef != null) {

                    indent(2);
                    xmlStreamWriter.writeStartElement(HttpEndpoint.XML_ELEMENT_NAME_SSL_OPTIONS);
                    xmlStreamWriter.writeAttribute(SslOptions.XML_ATTRIBUTE_NAME_SSL_REF, sslRef);
                    xmlStreamWriter.writeEndElement(); // SSL_OPTIONS
                }
            }

            indent(1);
            xmlStreamWriter.writeEndElement(); // HTTP_ENDPOINT
        }

    }

    void writeSsl(ServerConfiguration sourceConfig) throws XMLStreamException, IOException {
        List<SSLConfig> ssls = sourceConfig.getSsls(); // size in 0..1
        if (ssls != null && ssls.size() > 0) {
            SSLConfig ssl = ssls.get(0);

            indent(1);
            xmlStreamWriter.writeStartElement(ServerConfiguration.XML_ELEMENT_NAME_SSL);

            xmlStreamWriter.writeAttribute(SSLConfig.XML_ATTRIBUTE_NAME_KEY_STORE_REF, ssl.getKeyStoreRef());
            String trustStoreRef = ssl.getTrustStoreRef();
            if (trustStoreRef != null && !"".equals(trustStoreRef)) {
                xmlStreamWriter.writeAttribute(SSLConfig.XML_ATTRIBUTE_NAME_TRUST_STORE_REF, trustStoreRef);
            }
            String sslProtocol = ssl.getSslProtocol();
            if (sslProtocol != null) {
                xmlStreamWriter.writeAttribute(SSLConfig.XML_ATTRIBUTE_NAME_SSL_PROTOCOL, sslProtocol);
            }
            String enabledCiphers = ssl.getEnabledCiphers();
            if (enabledCiphers != null) {
                xmlStreamWriter.writeAttribute(SSLConfig.XML_ATTRIBUTE_NAME_ENABLED_CIPHERS, enabledCiphers);
            }
            Boolean clientAuth = ssl.getClientAuthentication();
            if (clientAuth != null && clientAuth == true) {
                xmlStreamWriter.writeAttribute(SSLConfig.XML_ATTRIBUTE_NAME_CLIENT_AUTH, clientAuth.toString());
            } else {
                Boolean clientAuthSupported = ssl.getClientAuthenticationSupported();
                if (clientAuthSupported != null && clientAuthSupported == true) {
                    xmlStreamWriter.writeAttribute(SSLConfig.XML_ATTRIBUTE_NAME_CLIENT_AUTH_SUPPORTED, clientAuthSupported.toString());
                }
            }
            xmlStreamWriter.writeAttribute(ConfigElement.XML_ATTRIBUTE_NAME_ID, ssl.getId());

            indent(1);
            xmlStreamWriter.writeEndElement(); // SSL

            writeKeyAndTrustStores(sourceConfig);
        }
    }

    void writeKeyAndTrustStores(ServerConfiguration sourceConfig) throws XMLStreamException, IOException {
        List<KeyStore> keyStores = sourceConfig.getKeyStores(); // size in 0..2
        if (keyStores != null && keyStores.size() > 0) {
            for (KeyStore keyStore : keyStores) {

                indent(1);
                xmlStreamWriter.writeStartElement(ServerConfiguration.XML_ELEMENT_NAME_KEYSTORE);

                xmlStreamWriter.writeAttribute(KeyStore.XML_ATTRIBUTE_NAME_LOCATION, keyStore.getLocation());
                String password = keyStore.getPassword();
                if (password != null && !"".equals(password)) {
                    xmlStreamWriter.writeAttribute(KeyStore.XML_ATTRIBUTE_NAME_PASSWORD, password);
                }
                String type = keyStore.getType();
                if (type != null && !"".equals(type)) {
                    xmlStreamWriter.writeAttribute(KeyStore.XML_ATTRIBUTE_NAME_TYPE, type);
                }
                String provider = keyStore.getProvider();
                if (provider != null && !"".equals(provider)) {
                    xmlStreamWriter.writeAttribute(KeyStore.XML_ATTRIBUTE_NAME_PROVIDER, provider);
                }
                xmlStreamWriter.writeAttribute(ConfigElement.XML_ATTRIBUTE_NAME_ID, keyStore.getId());

                List<KeyEntry> keyEntries = keyStore.getKeyEntries();
                if (keyEntries != null && keyEntries.size() > 0) { // size in 0..1
                    KeyEntry keyEntry = keyEntries.get(0);

                    indent(2);
                    xmlStreamWriter.writeStartElement(KeyStore.XML_ELEMENT_NAME_KEY_ENTRY);
                    xmlStreamWriter.writeAttribute(KeyEntry.XML_ATTRIBUTE_NAME_NAME, keyEntry.getName());
                    String keyPassword = keyEntry.getKeyPassword();
                    if (keyPassword != null) {
                        xmlStreamWriter.writeAttribute(KeyEntry.XML_ATTRIBUTE_NAME_KEY_PASSWORD, keyPassword);
                    }
                    // No id attribute
                    xmlStreamWriter.writeEndElement(); // KEYENTRY
                }

                indent(1);
                xmlStreamWriter.writeEndElement(); // KEYSTORE
            }
        }
    }

}
