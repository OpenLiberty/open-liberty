/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
package io.openliberty.jcache.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.cache.CacheManager;
import javax.cache.spi.CachingProvider;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Configuration utility class that contains logic that is to be shared between
 * the HTTP session cache code, and the new generic JCache configuration code.
 */
public class CacheConfigUtil {

    private static final TraceComponent tc = Tr.register(CacheConfigUtil.class);
    private static final String HAZELCAST_PROVIDER = "com.hazelcast.cache.HazelcastCachingProvider";
    private static final String HAZELCAST_MEMBER_PROVIDER = "com.hazelcast.cache.HazelcastMemberCachingProvider";
    private static final String HAZELCAST_SERVER_IMPL_PROVIDER = "com.hazelcast.cache.impl.HazelcastServerCachingProvider";
    private static final String HAZELCAST_CLIENT_PROVIDER = "com.hazelcast.client.cache.HazelcastClientCachingProvider";
    private static final String HAZELCAST_CLIENT_IMPL_PROVIDER = "com.hazelcast.client.cache.impl.HazelcastClientCachingProvider";

    private static final String INFINISPAN_EMBEDDED_PROVIDER = "org.infinispan.jcache.embedded.JCachingProvider";
    private static final String INFINISPAN_REMOTE_PROVIDER = "org.infinispan.jcache.remote.JCachingProvider";

    /**
     * Temporary config file that is created for Infinispan in the absence of an
     * explicitly specified config file uri or to augment existing config.
     */
    private File tempConfigFile;

    /**
     * Cleanup any artifacts that are no longer in use.
     */
    public void cleanup() {
        if (tempConfigFile != null) {
            tempConfigFile.delete();
        }
    }

    /**
     * Pre-configure the CacheManager. This method will update or generate
     * configuration for use when getting the {@link CacheManager} from the
     * {@link CachingProvider}.
     *
     * @param uriValue        The configured URI.
     * @param cachingProvider The {@link CachingProvider} that will be used to get
     *                            the {@link CacheManager} from.
     * @param properties      The configured properties. These may be updated.
     * @return The URI to be used when getting the {@link CacheManager}. This may be
     *         the same as configuredUri, or it may be updated.
     * @throws IOException If there was an error generating or updating the
     *                         configuration.
     */
    public URI preConfigureCacheManager(String uriValue, CachingProvider cachingProvider, Properties properties) throws IOException {

        URI uriToReturn = null;
        final URI configuredUri;
        if (uriValue != null) {
            try {
                configuredUri = new URI(uriValue);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(Tr.formatMessage(tc, "INCORRECT_URI_SYNTAX", e), e);
            }
        } else {
            configuredUri = null;
        }

        switch (cachingProvider.getClass().getName()) {

            case HAZELCAST_PROVIDER:
            case HAZELCAST_MEMBER_PROVIDER:
            case HAZELCAST_SERVER_IMPL_PROVIDER:
            case HAZELCAST_CLIENT_PROVIDER:
            case HAZELCAST_CLIENT_IMPL_PROVIDER:
                if (uriValue != null)
                    properties.setProperty("hazelcast.config.location", uriValue);
                break;

            case INFINISPAN_EMBEDDED_PROVIDER:
                /*
                 * For embedded Infinispan, augment existing config file to recognize cache
                 * names used by HTTP Session Persistence, or create a new config file if absent
                 * altogether.
                 */
                uriToReturn = generateOrUpdateInfinispanConfig(configuredUri);
                break;

            case INFINISPAN_REMOTE_PROVIDER:

                /*
                 * For remote Infinispan, augment existing vendorProperties to configure HTTP
                 * Session Persistence caches to be replicated:
                 * infinispan.client.hotrod.cache.[com.ibm.ws.session.*].template_name=org.
                 * infinispan.REPL_SYNC
                 */
                boolean isRemoteCachingConfigured = false;
                for (String key : properties.stringPropertyNames()) {
                    /*
                     * Check if cache configuration was set by the user already for the HTTP Session
                     * Persistence caches.
                     */
                    if (key != null && key.contains("com.ibm.ws.session.")
                        && (key.endsWith(".template_name") || key.endsWith(".configuration_uri"))) {
                        isRemoteCachingConfigured = true;
                        break;
                    }
                }
                if (!isRemoteCachingConfigured) {
                    properties.put("infinispan.client.hotrod.cache.[com.ibm.ws.session.*].template_name",
                                   "org.infinispan.REPL_SYNC");
                }
                uriToReturn = configuredUri;
                break;

            default:
                uriToReturn = configuredUri;
                break;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "The following properties will be configured for Infinispan: " + properties);
        }
        return uriToReturn;
    }

    /**
     * Generates new Infinispan config if absent or lacks
     * replicated-cache-configuration for the caches used by HTTP session
     * persistence. Otherwise, if no changes are needed, returns the supplied URI.
     *
     * @param configuredURI URI (if any) that is configured by the user. Otherwise
     *                          null.
     * @return URI for generated Infinispan config. If no changes are needed,
     *         returns the original URI.
     */
    private URI generateOrUpdateInfinispanConfig(URI configuredURI) throws IOException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        if (configuredURI == null) {
            tempConfigFile = File.createTempFile("infinispan", ".xml");
            tempConfigFile.setReadable(true);
            tempConfigFile.setWritable(true);
            StringWriter sw = new StringWriter();
            try (PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(tempConfigFile)));
                            PrintWriter pw = new PrintWriter(sw)) {
                final List<String> lines = Arrays.asList("<infinispan>", " <jgroups>",
                                                         "  <stack-file name=\"jgroups-udp\" path=\"/default-configs/default-jgroups-udp.xml\"/>",
                                                         " </jgroups>", " <cache-container>", "  <transport stack=\"jgroups-udp\"/>",
                                                         "  <replicated-cache-configuration name=\"com.ibm.ws.session.*\"/>", " </cache-container>",
                                                         "</infinispan>");
                pw.println();
                for (String line : lines) {
                    out.println(line);
                    pw.println(line);
                }
            }

            /*
             * This message was taken from the HTTP session cache, which was the first
             * feature to include JCache configuration. It was moved here when JCache configuration
             * was genericized to be used in multiple features.
             */
            Tr.info(tc, "SESN0310_GEN_INFINISPAN_CONFIG", sw.toString());
            return tempConfigFile.toURI();
        }

        /*
         * Determine if changes are needed to provided Infinispan configuration
         */
        try {
            URLConnection con = configuredURI.toURL().openConnection();
            DocumentBuilder docbuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = docbuilder.parse(con.getInputStream());

            LinkedList<Node> cacheContainers = new LinkedList<Node>();
            LinkedList<Node> elements = new LinkedList<Node>();
            elements.add(doc);
            for (Node element; (element = elements.poll()) != null;) {
                NodeList children = element.getChildNodes();
                for (int i = children.getLength() - 1; i >= 0; i--) {
                    Node child = children.item(i);
                    if (child.getNodeType() == Node.ELEMENT_NODE)
                        elements.add(child);
                }

                String elementName = element.getNodeName().toLowerCase();
                if ("cache-container".equalsIgnoreCase(elementName))
                    cacheContainers.add(element);

                NamedNodeMap attributes = element.getAttributes();
                if (attributes != null)
                    for (int i = attributes.getLength() - 1; i >= 0; i--) {
                        /*
                         * Leave existing Infinispan config as is if already configured for the
                         * com.ibm.ws.session cache names.
                         */
                        Node nameAttribute = attributes.getNamedItem("name");
                        if (nameAttribute != null) {
                            String nameValue = nameAttribute.getNodeValue();
                            if (nameValue != null && (elementName.endsWith("-cache")
                                                      || elementName.endsWith("-cache-configuration"))) {
                                String regex = infinispanCacheNameToRegEx(nameValue);
                                Pattern pattern = Pattern.compile(regex);
                                if (pattern.matcher("com.ibm.ws.session.attr.").matches()
                                    || pattern.matcher("com.ibm.ws.session.meta.").matches()) {
                                    if (trace && tc.isDebugEnabled())
                                        Tr.debug(this, tc, "No changes due to " + elementName + " name=" + nameValue);
                                    return configuredURI;
                                }
                            }
                        }
                    }
            }

            /*
             * A cache name matching com.ibm.ws.session.* was not found in the provided
             * configuration. Add it to cache-container.
             */
            for (Node cacheContainer : cacheContainers) {
                Element replicatedCacheConfig = doc.createElement("replicated-cache-configuration");
                Attr nameAttribute = doc.createAttribute("name");
                nameAttribute.setNodeValue("com.ibm.ws.session.*");
                replicatedCacheConfig.setAttributeNode(nameAttribute);
                cacheContainer.appendChild(replicatedCacheConfig);
            }

            if (cacheContainers.isEmpty()) {
                /*
                 * Infinispan config is likely invalid. Let Infinispan deal with this.
                 */
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "No cache-container was found");
                return configuredURI;
            }

            tempConfigFile = File.createTempFile("infinispan", ".xml");
            tempConfigFile.setReadable(true);
            tempConfigFile.setWritable(true);
            tempConfigFile.deleteOnExit();

            StreamResult uriResult = new StreamResult(new FileOutputStream(tempConfigFile));
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            DOMSource source = new DOMSource(doc);
            transformer.transform(source, uriResult);

            if (trace && tc.isDebugEnabled()) {
                /*
                 * Avoid tracing passwords:
                 */
                NodeList allElements = doc.getElementsByTagName("*");
                for (int i = allElements.getLength() - 1; i >= 0; i--) {
                    Node element = allElements.item(i);
                    String elementName = element.getNodeName();
                    if (!"jgroups".equals(elementName) && !"stack".equals(elementName)
                        && !"stack-file".equals(elementName) && !"transport".equals(elementName)
                        && !elementName.endsWith("-cache") && !elementName.endsWith("-cache-configuration")) {
                        NamedNodeMap attrs = element.getAttributes();
                        if (attrs != null) {
                            for (int j = attrs.getLength() - 1; j >= 0; j--) {
                                Node attr = attrs.item(j);
                                attr.setNodeValue("***");
                            }
                        }
                    }
                }

                StringWriter sw = new StringWriter();
                StreamResult loggableResult = new StreamResult(sw);
                transformer.transform(source, loggableResult);
                Tr.debug(this, tc, "generateOrUpdateInfinispanConfig", tempConfigFile, sw.toString());
            }
            return tempConfigFile.toURI();
        } catch (ParserConfigurationException | SAXException | TransformerException x) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "unable to enhance Infinispan config", x);
            return configuredURI;
        }
    }

    /**
     * Convert an Infinispan cache name that might include wild cards (*) into a
     * Java regular expression, so that we can determine if it would match the cache
     * names used by HTTP session persistence.
     *
     * @param s configured cache name value, possibly including wild card characters
     *              (*).
     * @return Java regular expression that can be used to match the HTTP session
     *         persistence cache names.
     */
    private String infinispanCacheNameToRegEx(String s) {
        int len = s.length();
        StringBuilder regex = new StringBuilder(len + 5);

        int start = 0;
        for (int i = 0; (i = s.indexOf('*', i)) >= 0; start = i + 1, i++) {
            String part = s.substring(start, i);
            if (part.length() > 0)
                regex.append("\\Q").append(part).append("\\E");
            regex.append(".*");
        }

        if (start < len)
            regex.append("\\Q").append(s.substring(start)).append("\\E");

        return regex.toString();
    }
}
