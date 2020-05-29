/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.smallrye.config;

import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.ws.microprofile.config.serverxml.AppPropertyConfigSource;
import com.ibm.ws.microprofile.config.serverxml.ServerXMLDefaultVariableConfigSource;
import com.ibm.ws.microprofile.config.serverxml.ServerXMLVariableConfigSource;

/**
 *
 */
public class OLSmallRyeConfigBuilder extends SmallRyeConfigBuilder {

    @Override
    List<ConfigSource> getDefaultSources() {

        List<ConfigSource> defaultSources = super.getDefaultSources();

        defaultSources.add(new AppPropertyConfigSource());
        defaultSources.add(new ServerXMLVariableConfigSource());
        defaultSources.add(new ServerXMLDefaultVariableConfigSource());

        return defaultSources;
    }

}
