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
package com.ibm.ws.microprofile.config14.sources;

import java.util.ArrayList;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.ws.microprofile.config.sources.DefaultSources;
import com.ibm.ws.microprofile.config.sources.SystemConfigSource;
import com.ibm.ws.microprofile.config13.sources.AppPropertyConfigSource;
import com.ibm.ws.microprofile.config13.sources.ServerXMLDefaultVariableConfigSource;
import com.ibm.ws.microprofile.config13.sources.ServerXMLVariableConfigSource;

public class Config14DefaultSources extends DefaultSources {

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
        sources.add(new EnvConfig14Source());
        sources.add(new AppPropertyConfigSource());
        sources.add(new ServerXMLVariableConfigSource());
        sources.add(new ServerXMLDefaultVariableConfigSource());

        sources.addAll(getPropertiesFileConfigSources(classloader));

        return sources;
    }
}