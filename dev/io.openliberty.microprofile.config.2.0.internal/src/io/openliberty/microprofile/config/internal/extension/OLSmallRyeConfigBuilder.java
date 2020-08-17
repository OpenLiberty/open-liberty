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
package io.openliberty.microprofile.config.internal.extension;

import io.openliberty.microprofile.config.internal.serverxml.AppPropertyConfigSource;
import io.openliberty.microprofile.config.internal.serverxml.ServerXMLDefaultVariableConfigSource;
import io.openliberty.microprofile.config.internal.serverxml.ServerXMLVariableConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class OLSmallRyeConfigBuilder extends SmallRyeConfigBuilder {

    private boolean addDefaultSources = false;

    @Override
    public SmallRyeConfigBuilder addDefaultSources() {
        addDefaultSources = true;
        return super.addDefaultSources();
    }

    @Override
    public SmallRyeConfig build() {
        if (this.addDefaultSources) {
            withSources(new AppPropertyConfigSource(),
                        new ServerXMLVariableConfigSource(),
                        new ServerXMLDefaultVariableConfigSource());
        }
        return super.build();
    }

    //The methods above are a temporary work around for adding OL's custom config sources

    //When the new version of SmallRye Config is uploaded to Maven Central the method below will be used

    //The updated version will make the getDefaultSources() method protected so that it can be @Override'n

    /*
     * @Override
     * List<ConfigSource> getDefaultSources() {
     *
     * List<ConfigSource> defaultSources = super.getDefaultSources();
     *
     * defaultSources.add(new AppPropertyConfigSource());
     * defaultSources.add(new ServerXMLVariableConfigSource());
     * defaultSources.add(new ServerXMLDefaultVariableConfigSource());
     *
     * return defaultSources;
     * }
     */

}
