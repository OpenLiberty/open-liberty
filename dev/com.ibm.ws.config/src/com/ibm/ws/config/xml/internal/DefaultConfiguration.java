/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal;

import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import com.ibm.websphere.config.ConfigParserException;
import com.ibm.websphere.config.ConfigUpdateException;
import com.ibm.websphere.config.ConfigValidationException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.xml.internal.XMLConfigParser.MergeBehavior;
import com.ibm.ws.config.xml.internal.variables.ConfigVariableRegistry;

/**
 *
 */
class DefaultConfiguration {

    private final Map<Bundle, BaseConfiguration> defaultConfigurationMap;
    private final Map<String, BaseConfiguration> runtimeDefaultConfigurationMap;
    private final XMLConfigParser parser;

    static final TraceComponent tc = Tr.register(DefaultConfiguration.class, XMLConfigConstants.TR_GROUP, XMLConfigConstants.NLS_PROPS);

    public DefaultConfiguration(XMLConfigParser xmlParser) {
        this.defaultConfigurationMap = new ConcurrentHashMap<Bundle, BaseConfiguration>();
        this.runtimeDefaultConfigurationMap = new ConcurrentHashMap<String, BaseConfiguration>();
        this.parser = xmlParser;
    }

    protected class DefaultConfigFile {
        /**
         * @param nextElement
         * @param merge
         */
        public DefaultConfigFile(URL url, boolean requireExisting, boolean requireNotExisting) {
            this.fileURL = url;
            if (requireExisting)
                this.behavior = MergeBehavior.MERGE_WHEN_EXISTS;
            else if (requireNotExisting)
                this.behavior = MergeBehavior.MERGE_WHEN_MISSING;
            else
                this.behavior = MergeBehavior.MERGE;
        }

        final URL fileURL;
        final MergeBehavior behavior;

    }

    public BaseConfiguration load(Bundle bundle, ServerXMLConfiguration serverXMLConfig,
                                  ConfigVariableRegistry variableRegistry) throws ConfigValidationException, ConfigUpdateException {
        BaseConfiguration bundleDefaultConfiguration = null;
        Collection<DefaultConfigFile> defaultConfigFiles = Collections.emptyList();
        try {
            defaultConfigFiles = getDefaultConfigurationFiles(bundle);
        } catch (BundleException e1) {
            throw new ConfigUpdateException(e1);
        }
        if (!defaultConfigFiles.isEmpty()) {
            bundleDefaultConfiguration = new BaseConfiguration();
            for (DefaultConfigFile defaultConfigFile : defaultConfigFiles) {
                try {
                    BaseConfiguration configuration = parser.parseDefaultConfiguration(defaultConfigFile);
                    bundleDefaultConfiguration.append(configuration);
                } catch (ConfigParserException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Exception while loading default configuration.  Message=" + e.getMessage());
                    }

                    parser.handleParseError(e, bundle);

                    if (ErrorHandler.INSTANCE.fail()) {
                        throw new ConfigUpdateException(e);
                    }
                }
            }
            BaseConfiguration defaultConfiguration = serverXMLConfig.getDefaultConfiguration();
            defaultConfiguration.remove(defaultConfigurationMap.get(bundle));
            defaultConfiguration.add(bundleDefaultConfiguration);
            defaultConfigurationMap.put(bundle, bundleDefaultConfiguration);

            // Add variables loaded from default configurations
            variableRegistry.setDefaultVariables(defaultConfiguration.getVariables());
        }
        return bundleDefaultConfiguration;
    }

    public BaseConfiguration add(InputStream defaultConfig, ServerXMLConfiguration serverXMLConfig,
                                 ConfigVariableRegistry variableRegistry) throws ConfigValidationException, ConfigUpdateException {
        BaseConfiguration configuration = new BaseConfiguration();
        try {

            if (!parser.parseServerConfiguration(defaultConfig, "runtimeDefaultConfig", configuration, MergeBehavior.MERGE)) {
                configuration = null;
            } else {
                // We have to keep track of what we're adding so we can remove it later. This involves
                // splitting the configuration we just parsed so we can store it in a map by pid.
                for (String name : configuration.getConfigurationNames()) {
                    BaseConfiguration newConfig = runtimeDefaultConfigurationMap.get(name);
                    if (newConfig == null) {
                        newConfig = new BaseConfiguration();
                        runtimeDefaultConfigurationMap.put(name, newConfig);
                    }

                    newConfig.getConfigurationList(name).add(configuration.getConfigurationList(name));
                }

                BaseConfiguration defaultConfiguration = serverXMLConfig.getDefaultConfiguration();
                defaultConfiguration.add(configuration);

                variableRegistry.setDefaultVariables(defaultConfiguration.getVariables());

            }

            return configuration;

        } catch (ConfigParserException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception while loading default configuration.  Message=" + e.getMessage());
            }

            parser.handleParseError(e, null);

            if (ErrorHandler.INSTANCE.fail()) {
                throw new ConfigUpdateException(e);
            }
        }

        return configuration;
    }

    public BaseConfiguration add(String pid, Dictionary<String, String> props, ServerXMLConfiguration serverXMLConfig,
                                 ConfigVariableRegistry variableRegistry) throws ConfigMergeException {
        SimpleElement element = new SimpleElement(pid);
        element.setDocumentLocation("runtimeDefaultConfiguration");
        Enumeration<String> keys = props.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            element.addAttribute(key, props.get(key));
            if (XMLConfigConstants.CFG_INSTANCE_ID.equals(key)) {
                element.setId(props.get(key));
            }
        }

        BaseConfiguration newConfig = runtimeDefaultConfigurationMap.get(pid);
        if (newConfig == null) {
            newConfig = new BaseConfiguration();
            runtimeDefaultConfigurationMap.put(pid, newConfig);
        }
        newConfig.addConfigElement(element);

        BaseConfiguration defaultConfiguration = serverXMLConfig.getDefaultConfiguration();
        defaultConfiguration.add(newConfig);

        variableRegistry.setDefaultVariables(defaultConfiguration.getVariables());

        return newConfig;
    }

    /**
     * @param pid
     * @param serverXMLConfig
     * @param variableRegistry
     * @return
     */
    public BaseConfiguration getRuntimeDefaultConfiguration(String pid) {
        return runtimeDefaultConfigurationMap.get(pid);

    }

    /**
     * @param bundle
     * @return
     */
    public BaseConfiguration remove(Bundle bundle) {
        return defaultConfigurationMap.remove(bundle);
    }

    private Collection<DefaultConfigFile> getDefaultConfigurationFiles(Bundle bundle) throws BundleException {
        Dictionary<String, String> headers = bundle.getHeaders("");
        if (headers == null) {
            return Collections.emptySet();
        }

        String defaultConfigHeader = headers.get(XMLConfigConstants.DEFAULT_CONFIG_HEADER);
        if (defaultConfigHeader == null) {
            return Collections.emptySet();
        }

        Set<DefaultConfigFile> configurationFiles = new LinkedHashSet<DefaultConfigFile>();
        ManifestElement[] elements = ManifestElement.parseHeader(XMLConfigConstants.DEFAULT_CONFIG_HEADER, defaultConfigHeader);
        for (ManifestElement element : elements) {
            boolean requireExisting = false;
            boolean requireNotExisting = false;
            String requireExistingStr = element.getAttribute(XMLConfigParser.REQUIRE_EXISTING);
            if (requireExistingStr != null) {
                requireExisting = Boolean.valueOf(requireExistingStr);
            }

            String requireDoesNotExistStr = element.getAttribute(XMLConfigParser.REQUIRE_DOES_NOT_EXIST);
            if (requireDoesNotExistStr != null) {
                requireNotExisting = Boolean.valueOf(requireDoesNotExistStr);
            }

            if (requireExisting && requireNotExisting) {
                // I'm not going to add an error message here because (1) it would be extraordinarily unusual for someone to specify
                // requireExisting=true;requireDoesNotExist=true (2) end users can't specify default config
                // The effect of having both require_existing and require_does_not_exist would be the config never being loaded, so
                // we'll just skip it here.
                continue;
            }

            String fileFilter = element.getValue();
            if (fileFilter.contains("*")) {
                String path;
                String filePattern;
                int pos = fileFilter.lastIndexOf('/');
                if (pos < 0) {
                    path = "/";
                    filePattern = fileFilter;
                } else {
                    path = fileFilter.substring(0, pos + 1);
                    filePattern = fileFilter.substring(pos + 1);
                }
                Enumeration<URL> entries = bundle.findEntries(path, filePattern, false);
                if (entries != null) {
                    while (entries.hasMoreElements()) {
                        DefaultConfigFile file = new DefaultConfigFile(entries.nextElement(), requireExisting, requireNotExisting);
                        configurationFiles.add(file);
                    }
                }
            } else {
                DefaultConfigFile file = new DefaultConfigFile(bundle.getEntry(fileFilter), requireExisting, requireNotExisting);
                if (file.fileURL == null) {
                    throw new NullPointerException("Bundle: " + bundle + " specifies default config at " + fileFilter + " which is missing");
                }
                configurationFiles.add(file);
            }
        }

        return configurationFiles;
    }

}
