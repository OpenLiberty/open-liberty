/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal.common;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.wsspi.logging.Introspector;

/**
 * Common code for dumping the contents of a set of Config objects
 * <p>
 * Intended to be called from an {@link Introspector}
 */
public class ConfigIntrospection {

    /**
     * Dump the config objects for a set of applications
     *
     * @param pw the PrintWriter to dump to
     * @param appInfo map of application names to the set of configs used by that application
     */
    public static void introspect(PrintWriter pw, Map<String, Set<Config>> appInfo) {
        for (Entry<String, Set<Config>> entry : appInfo.entrySet()) {
            introspectApp(pw, entry.getKey(), entry.getValue());
        }
    }

    static void introspectApp(PrintWriter pw, String appName, Set<Config> configs) {
        pw.println();
        pw.println("########################");
        pw.println("Config for " + appName);
        pw.println("########################");

        for (Config config : configs) {
            introspectConfig(pw, config);
        }
    }

    private static void introspectConfig(PrintWriter pw, Config config) {
        try {
            pw.println("Config: " + config.toString());
            for (ConfigSource source : config.getConfigSources()) {
                introspectConfigSource(pw, source);
            }
        } catch (Exception e) {
            pw.printf("Exception introspecting config: %s", e);
        }
    }

    private static void introspectConfigSource(PrintWriter pw, ConfigSource source) {
        try {
            pw.printf(" - %s config source (ordinal %d)\n", source.getName(), source.getOrdinal());

            Map<String, String> properties = source.getProperties();
            if (properties == null) {
                pw.println("     Cannot list properties from this config source because ConfigSource.getProperties() returned null.");
                return;
            }

            for (Entry<String, String> entry : properties.entrySet()) {
                pw.printf("   - %s = %s\n", entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            pw.printf("Exception introspecting config source: %s\n", e);
        }
    }
}
