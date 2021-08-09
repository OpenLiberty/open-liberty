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

import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.microprofile.config.internal.serverxml.AppPropertyConfigSource;
import io.openliberty.microprofile.config.internal.serverxml.ServerXMLDefaultVariableConfigSource;
import io.openliberty.microprofile.config.internal.serverxml.ServerXMLVariableConfigSource;
import io.smallrye.config.ProfileConfigSourceInterceptor;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class OLSmallRyeConfigBuilder extends SmallRyeConfigBuilder {

    private static final TraceComponent tc = Tr.register(OLSmallRyeConfigBuilder.class);

    @Override
    protected List<ConfigSource> getDefaultSources() {

        List<ConfigSource> defaultSources = super.getDefaultSources();

        defaultSources.add(new AppPropertyConfigSource());
        defaultSources.add(new ServerXMLVariableConfigSource());
        defaultSources.add(new ServerXMLDefaultVariableConfigSource());

        return defaultSources;
    }

    @Override
    @Trivial
    public SmallRyeConfig build() {
        SmallRyeConfig config = super.build();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            // Note: SMALLRYE_PROFILE gets internally mapped to also pick up the standard Config.PROFILE
            String profileName = config.getRawValue(ProfileConfigSourceInterceptor.SMALLRYE_PROFILE);
            Tr.event(this, tc, "Config created with profile: " + profileName, config);
        }
        return config;
    }

}
