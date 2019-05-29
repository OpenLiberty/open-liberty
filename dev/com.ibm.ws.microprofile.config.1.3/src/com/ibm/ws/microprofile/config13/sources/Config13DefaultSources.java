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
package com.ibm.ws.microprofile.config13.sources;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.microprofile.config.sources.DefaultSources;
import com.ibm.ws.microprofile.config.sources.SystemConfigSource;

@Component(service = Config13DefaultSources.class, immediate = true)
public class Config13DefaultSources extends DefaultSources {

    private static List<ConfigSource> listOfConfigSources = new ArrayList<ConfigSource>();

    /**
     * The classloader's loadResources method is used to locate resources of
     * name {#link ConfigConstants.CONFIG_PROPERTIES} as well as process environment
     * variables and Java System.properties
     *
     * @param classloader
     * @return the default sources found
     */
    public synchronized static ArrayList<ConfigSource> getDefaultSources(ClassLoader classloader) {
        ArrayList<ConfigSource> sources = new ArrayList<>();

        sources.add(new SystemConfigSource());
        sources.add(new EnvConfig13Source());
        sources.add(new AppPropertyConfigSource());
        sources.add(new ServerXMLVariableConfigSource());
        sources.add(new ServerXMLDefaultVariableConfigSource());

        sources.addAll(getPropertiesFileConfigSources(classloader));

        if (!listOfConfigSources.isEmpty())
            sources.addAll(listOfConfigSources);

        return sources;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected synchronized void setConfigSource(ConfigSource configSource) {
        listOfConfigSources.add(configSource);
    }

    protected synchronized void unsetConfigSource(ConfigSource configSource) {
        listOfConfigSources.remove(configSource);
    }
}
