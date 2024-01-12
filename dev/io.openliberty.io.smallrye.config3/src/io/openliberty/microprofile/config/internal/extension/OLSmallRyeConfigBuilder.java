/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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
package io.openliberty.microprofile.config.internal.extension;

import static java.security.AccessController.doPrivileged;

import java.security.PrivilegedAction;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.openliberty.microprofile.config.internal.extension.OLSmallRyeConfigExtension.UnpauseRecording;
import io.openliberty.microprofile.config.internal.serverxml.AppPropertyConfigSource;
import io.openliberty.microprofile.config.internal.serverxml.ServerXMLDefaultVariableConfigSource;
import io.openliberty.microprofile.config.internal.serverxml.ServerXMLVariableConfigSource;
import io.smallrye.config.EnvConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class OLSmallRyeConfigBuilder extends SmallRyeConfigBuilder {

    private static final TraceComponent tc = Tr.register(OLSmallRyeConfigBuilder.class);

    @Override
    protected List<ConfigSource> getDefaultSources() {

        // TODO ideally we should not have to call super here and instead just do what super does directly
        // This would avoid having to replace the EnvConfigSource with the right one.
        List<ConfigSource> defaultSources = super.getDefaultSources();

        // Replace the EnvConfigSource that super created with an instance that does not copy the env Map.
        // For Liberty we do not want to copy the env Map into the config source.  This allows
        // for the config source to be updated properly when restoring a checkpoint process with an
        // updated environment.
        for (ListIterator<ConfigSource> iSources = defaultSources.listIterator(); iSources.hasNext();) {
            ConfigSource source = iSources.next();
            if (source instanceof EnvConfigSource) {
                iSources.set(new EnvConfigSource(doPrivileged((PrivilegedAction<Map<String, String>>) System::getenv), source.getOrdinal()));
            }
        }

        defaultSources.add(new AppPropertyConfigSource());
        defaultSources.add(new ServerXMLVariableConfigSource());
        defaultSources.add(new ServerXMLDefaultVariableConfigSource());
        // Getting the phase non null implies that the server did a checkpoint.
        // Wrap all the config sources during the checkpoint to record the configuration values read during checkpoint by the application before doing a server restore.
        CheckpointPhase phase = CheckpointPhase.getPhase();
        if (!phase.restored()) {
            for (ListIterator<ConfigSource> iSources = defaultSources.listIterator(); iSources.hasNext();) {
                ConfigSource source = iSources.next();
                iSources.set(new ConfigSourceWrapper(source, phase));
            }
        }
        return defaultSources;
    }

    @Override
    @Trivial
    @FFDCIgnore(Throwable.class) // Ignoring Throwable because try-with-resources block adds implicit catch(Throwable) which we want to ignore.
    public SmallRyeConfig build() {
        // Do not record the configuration values read during the super.build(). Start recording the values read after this method.
        // We want to record the configuration values only when the application reads it, therefore pausing it.
        try (UnpauseRecording unpauseRecording = OLSmallRyeConfigExtension.pauseRecordingReads()) {
            return doBuild();
        }
    }

    private SmallRyeConfig doBuild() {
        SmallRyeConfig config = super.build();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            // Note: SMALLRYE_PROFILE gets internally mapped to also pick up the standard Config.PROFILE
            String profileName = config.getRawValue(SmallRyeConfig.SMALLRYE_CONFIG_PROFILE);
            Tr.event(this, tc, "Config created with profile: " + profileName, config);
        }
        return config;
    }
}
