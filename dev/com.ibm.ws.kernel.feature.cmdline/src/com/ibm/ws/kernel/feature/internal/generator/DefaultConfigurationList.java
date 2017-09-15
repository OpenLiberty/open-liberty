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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.TransformerException;

import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.xml.sax.SAXException;

import com.ibm.ws.config.xml.internal.XMLConfigConstants;
import com.ibm.ws.config.xml.internal.XMLConfigParser;
import com.ibm.ws.kernel.feature.internal.generator.FeatureListOptions.ReturnCode;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.kernel.provisioning.ContentBasedLocalBundleRepository;

/**
 * Output default instance information in the form:
 * 
 * <defaultInstance providingFeatures="com.ibm.websphere.superduper-1.0,com.ibm.websphere.notSoSuper-3.1">
 * <httpEndpoint
 * httpPort="9080"
 * httpsPort="9443"
 * id="defaultHttpEndpoint"/>
 * </defaultInstance>
 */
public class DefaultConfigurationList {

    private static final String DEFAULT_INSTANCE = "defaultInstance";
    private static final String PROVIDING_FEATURES = "providingFeatures";

    private final Map<String, ProvisioningFeatureDefinition> features;
    private final FeatureListOptions options;
    private final XMLStreamWriter writer;
    private final Indenter indenter;
    private final Map<FeatureResource, Set<String>> bundleNameToFeaturesMap = new HashMap<FeatureResource, Set<String>>();

    /**
     * 
     * @param options
     * @param features
     * @param utils
     */
    public DefaultConfigurationList(FeatureListOptions options, Map<String, ProvisioningFeatureDefinition> features,
                                    FeatureListUtils utils) {
        this.options = options;
        this.features = features;

        this.writer = utils.getXMLStreamWriter();

        this.indenter = utils.getIndenter();

    }

    /**
     * Wraps an individual bundle. Responsible for parsing the manifest and creating a new BundleWrapperFile for each IBM-Default-Config entry
     */
    private class BundleWrapper {

        protected JarFile jar;
        private final List<BundleWrapperFile> bundleFiles = new ArrayList<BundleWrapperFile>();

        /**
         * @param mfp
         * @param bundle
         * @throws IOException
         * @throws BundleException
         * @throws FactoryConfigurationError
         * @throws XMLStreamException
         */
        public BundleWrapper(ManifestFileProcessor mfp, FeatureResource bundle) throws IOException, BundleException, XMLStreamException, FactoryConfigurationError {

            ContentBasedLocalBundleRepository repo = mfp.getBundleRepository(bundle.getBundleRepositoryType(), null);
            File bundleFile = repo.selectBundle(bundle.getLocation(), bundle.getSymbolicName(), bundle.getVersionRange());
            if (bundleFile != null) {
                this.jar = new JarFile(bundleFile);
                Manifest manifest = jar.getManifest();
                Attributes headers = manifest.getMainAttributes();
                if (headers != null) {
                    String defaultConfig = headers.getValue(XMLConfigConstants.DEFAULT_CONFIG_HEADER);
                    if (defaultConfig != null) {

                        ManifestElement[] elements =
                                        ManifestElement.parseHeader(XMLConfigConstants.DEFAULT_CONFIG_HEADER, defaultConfig);
                        for (ManifestElement element : elements) {
                            bundleFiles.add(new BundleWrapperFile(element));
                        }

                    }
                }
            }

        }

        /**
         * @return
         */
        public List<DefaultElement> getDefaultElements() {
            List<DefaultElement> elements = new ArrayList<DefaultElement>();
            for (BundleWrapperFile file : bundleFiles) {
                elements.addAll(file.getDefaultElements());

            }
            return elements;
        }

        /**
         * Contains information about an individual defaultInstances.xml file in a bundle. Responsible for parsing the file.
         */
        private class BundleWrapperFile {
            private static final String SERVER = "server";
            private static final String CLIENT = "client";

            private XMLStreamReader reader;
            private final List<DefaultElement> elements = new ArrayList<DefaultElement>();

            /**
             * @param element
             * @throws IOException
             * @throws FactoryConfigurationError
             * @throws XMLStreamException
             */
            public BundleWrapperFile(ManifestElement element) throws XMLStreamException, FactoryConfigurationError, IOException {
                // Read values for requireExisting and addIfMissing from the IBM-Default-Config entry
                boolean existing = false;
                boolean notExisting = false;

                String requireExistingStr = element.getAttribute(XMLConfigParser.REQUIRE_EXISTING);
                if (requireExistingStr != null) {
                    existing = Boolean.valueOf(requireExistingStr);
                }

                String requireDoesNotExistStr = element.getAttribute(XMLConfigParser.REQUIRE_DOES_NOT_EXIST);
                if (requireDoesNotExistStr != null) {
                    notExisting = Boolean.valueOf(requireDoesNotExistStr);
                }

                // Get the actual defaultInstances.xml file
                String fileFilter = element.getValue();

                ZipEntry defaultConfigFile = jar.getEntry(fileFilter);
                if (defaultConfigFile != null) {
                    this.reader = XMLInputFactory.newInstance().createXMLStreamReader(jar.getInputStream(defaultConfigFile));

                    // Parse the config into a list of top level elements. 
                    parseConfig(elements, existing, notExisting);

                }
            }

            /**
             * It would be really nice if we could just load a DOM tree using standard APIs. Unfortunately, serializing to the stream doesn't seem to work well
             * in conjunction with the stream reader used for the rest of the file. So, we'll parse what we need into a simple structure here.
             * Fortunately, we don't have to worry about namespaces, etc.
             * 
             * @throws XMLStreamException
             */
            private void parseConfig(List<DefaultElement> elements, boolean requireExisting, boolean addIfMissing) throws XMLStreamException {

                while (reader.hasNext()) {

                    if (reader.isStartElement()) {
                        // Ignore <server> and <client>
                        if (SERVER.equals(reader.getLocalName()) || CLIENT.equals(reader.getLocalName())) {
                            nextTag();
                            continue;
                        }

                        // Create a new DefaultElement that contains the element name, attributes, and child elements
                        DefaultElement element = new DefaultElement(reader, requireExisting, addIfMissing);
                        elements.add(element);
                        nextTag();

                        // Write out any text elements and advance the reader to the next start/end tag
                        while (reader.isCharacters()) {
                            element.setText(reader.getText());
                            nextTag();
                        }

                        // Done with this element, continue to the next sibling
                        if (reader.isEndElement()) {
                            reader.next();
                            continue;
                        } else if (reader.isStartElement()) {
                            // Starting new subtree, parse it
                            parseConfig(element.getChildren(), element.requiresExisting(), element.addIfMissing());
                        }

                    } else if (reader.isEndElement()) {
                        // Done with this subtree
                        return;
                    }

                    nextTag();

                }

            }

            /**
             * Advance the reader to the next start element, end element, or text.
             * 
             * @throws XMLStreamException
             */
            private void nextTag() throws XMLStreamException {
                while (reader.hasNext()) {
                    reader.next();
                    if (reader.isCharacters() || reader.isStartElement() || reader.isEndElement())
                        return;
                }
            }

            /**
             * Return the list of top level configurations
             * 
             * @return
             */
            public List<DefaultElement> getDefaultElements() {
                return this.elements;
            }

        }

    }

    /**
     * Contains information about an individual element's name, attributes, and children.
     */
    private class DefaultElement {

        private final String localName;
        private final List<DefaultElement> children = new ArrayList<DefaultElement>();
        private final HashMap<String, String> attributes = new HashMap<String, String>();
        private final boolean addIfMissing;
        private final boolean requireExisting;
        private String text = null;

        /**
         * @param reader
         * @throws XMLStreamException
         */
        public DefaultElement(XMLStreamReader reader, boolean requireExisting, boolean addIfMissing) throws XMLStreamException {
            this.localName = reader.getLocalName();
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                attributes.put(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
            }

            this.requireExisting = requireExisting;
            this.addIfMissing = addIfMissing;

        }

        void setText(String txt) {
            this.text = txt;
        }

        /**
         * @return
         */
        public List<DefaultElement> getChildren() {
            return this.children;
        }

        /**
         * @throws XMLStreamException
         * @throws IOException
         * 
         */
        public void writeElement(int index) throws XMLStreamException, IOException {
            // Write element start
            indenter.indent(index);
            writer.writeStartElement(localName);

            // Write attributes
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                writer.writeAttribute(entry.getKey(), entry.getValue());
            }

            // Write children
            for (DefaultElement e : children) {
                e.writeElement(index + 1);
            }

            // Write text if it exists
            if (text != null) {
                writer.writeCharacters(text);
            }

            // Write end element
            indenter.indent(index);
            writer.writeEndElement();
        }

        /**
         * @return
         */
        public boolean addIfMissing() {
            return addIfMissing;
        }

        /**
         * @return
         */
        public boolean requiresExisting() {
            return requireExisting;
        }

    }

    /**
     * Builds a representation of default configuration and writes it out to the feature list file
     * 
     * @param mfp
     */
    public void writeDefaultConfiguration(ManifestFileProcessor mfp) {
        try {
            try {
                // Build the list of configurations
                buildDefaultConfigurationList(mfp);

                // Write <defaultConfiguration>
                startDefaultConfigurationSection();

                // We now have a map of bundles to the features that enable them. Loop through it and write out the default configuration for each one.
                for (Entry<FeatureResource, Set<String>> entry : bundleNameToFeaturesMap.entrySet()) {
                    FeatureResource bundle = entry.getKey();

                    BundleWrapper bw = new BundleWrapper(mfp, bundle);

                    for (DefaultElement element : bw.getDefaultElements()) {

                        indenter.indent(1);
                        writer.writeStartElement(DEFAULT_INSTANCE);
                        writer.writeAttribute(PROVIDING_FEATURES, getFeatureString(entry.getValue()));
                        if (element.requiresExisting())
                            writer.writeAttribute(XMLConfigParser.REQUIRE_EXISTING, "true");
                        if (element.addIfMissing())
                            writer.writeAttribute(XMLConfigParser.REQUIRE_DOES_NOT_EXIST, "true");
                        element.writeElement(2);
                        indenter.indent(1);
                        writer.writeEndElement();
                    }
                }

                // </defaultConfiguration>
                endDefaultConfigurationSection();
            } catch (XMLStreamException e) {
                throw new IOException("Error generating feature list", e);
            } catch (BundleException e) {
                throw new IOException("Error generating feature list", e);
            }
        } catch (IOException ex) {
            options.setReturnCode(ReturnCode.RUNTIME_EXCEPTION);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Build a comma separated string based on the set of feature names.
     * 
     * @return
     */
    private String getFeatureString(Set<String> featureNames) {
        StringBuilder featureBuilder = new StringBuilder();

        if (featureNames != null) {
            Iterator<String> iter = featureNames.iterator();
            while (iter.hasNext()) {
                featureBuilder.append(iter.next());
                if (iter.hasNext())
                    featureBuilder.append(',');
            }
        }
        return featureBuilder.toString();
    }

    /**
     * <defaultConfiguration>
     * 
     * @throws XMLStreamException
     * @throws IOException
     * 
     */
    private void startDefaultConfigurationSection() throws XMLStreamException, IOException {

        indenter.indent(0);
        writer.writeStartElement("defaultConfiguration");

    }

    /**
     * </defaultConfiguration>
     * 
     * @throws XMLStreamException
     * @throws IOException
     */
    private void endDefaultConfigurationSection() throws XMLStreamException, IOException {
        indenter.indent(0);
        writer.writeEndElement();
    }

    /**
     * Build a map of bundles to the features that enable them.
     * 
     * @param mfp
     * @throws IOException
     * @throws BundleException
     * @throws XMLStreamException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws TransformerException
     */
    private void buildDefaultConfigurationList(ManifestFileProcessor mfp) {

        for (Map.Entry<String, ProvisioningFeatureDefinition> entry : features.entrySet()) {
            ProvisioningFeatureDefinition feature = entry.getValue();
            Collection<FeatureResource> featureResources = feature.getConstituents(SubsystemContentType.BUNDLE_TYPE);
            for (FeatureResource featureResource : featureResources) {
                Set<String> featureSet = bundleNameToFeaturesMap.get(featureResource);
                if (featureSet == null) {
                    featureSet = new HashSet<String>();
                    bundleNameToFeaturesMap.put(featureResource, featureSet);
                }
                featureSet.add(feature.getSymbolicName());
            }
        }
    }

}
