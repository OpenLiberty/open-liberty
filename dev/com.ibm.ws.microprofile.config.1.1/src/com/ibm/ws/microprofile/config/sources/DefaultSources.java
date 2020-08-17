/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.sources;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.config.interfaces.ConfigConstants;

import io.openliberty.microprofile.config.internal.common.ConfigException;

public class DefaultSources {

    private static final TraceComponent tc = Tr.register(DefaultSources.class);

    /**
     * The classloader's loadResources method is used to locate resources of
     * name {#link ConfigConstants.CONFIG_PROPERTIES} as well as process environment
     * variables and Java System.properties
     *
     * @param classloader
     * @return the default sources found
     */
    public static ArrayList<ConfigSource> getDefaultSources(ClassLoader classloader) {
        ArrayList<ConfigSource> sources = new ArrayList<>();

        sources.add(new SystemConfigSource());
        sources.add(new EnvConfigSource());

        sources.addAll(getPropertiesFileConfigSources(classloader));

        return sources;
    }

    /**
     * Add resources of name {#link ConfigConstants.CONFIG_PROPERTIES} to a List of sources using
     * the classloader's loadResources method to locate resources.
     *
     * @param classloader
     * @param sources
     */
    public static List<ConfigSource> getPropertiesFileConfigSources(ClassLoader classloader) {
        ArrayList<ConfigSource> sources = new ArrayList<>();
        try {
            Enumeration<URL> propsResources = classloader.getResources(ConfigConstants.CONFIG_PROPERTIES);
            if (propsResources != null) {
                while (propsResources.hasMoreElements()) {
                    URL prop = propsResources.nextElement();
                    ConfigSource source = new PropertiesConfigSource(prop);
                    sources.add(source);
                }
            }
        } catch (IOException e) {
            //TODO maybe we should just output a warning and continue??
            //TODO NLS
            throw new ConfigException("Could not load " + ConfigConstants.CONFIG_PROPERTIES, e);
        }
        return sources;
    }

    /**
     * Get ConfigSources found using the ServiceLoader pattern - both directly
     * as found ConfigSources and those found via found ConfigSourceProviders'
     * getConfigSources method.
     *
     * @param classloader
     * @return
     */
    public static ArrayList<ConfigSource> getDiscoveredSources(ClassLoader classloader) {
        ArrayList<ConfigSource> sources = new ArrayList<>();

        //load config sources using the service loader
        try {
            ServiceLoader<ConfigSource> sl = ServiceLoader.load(ConfigSource.class, classloader);
            for (ConfigSource source : sl) {
                sources.add(source);
            }
        } catch (ServiceConfigurationError e) {
            throw new ConfigException(Tr.formatMessage(tc, "unable.to.discover.config.sources.CWMCG0010E", e), e);
        }

        try {
            //load config source providers using the service loader
            ServiceLoader<ConfigSourceProvider> providerSL = ServiceLoader.load(ConfigSourceProvider.class, classloader);
            for (ConfigSourceProvider provider : providerSL) {
                for (ConfigSource source : provider.getConfigSources(classloader)) {
                    sources.add(source);
                }
            }
        } catch (ServiceConfigurationError e) {
            throw new ConfigException(Tr.formatMessage(tc, "unable.to.discover.config.source.providers.CWMCG0011E", e), e);
        }

        return sources;
    }

}
