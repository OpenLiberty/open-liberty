/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package io.openliberty.microprofile.config.internal.common;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.wsspi.logging.Introspector;
import com.ibm.wsspi.logging.SensitiveIntrospector;

/**
 * Introspector for dumping the contents of a set of Config objects
 * <p>
 * Each of our config implementations needs to implement {@link ConfigIntrospectionProvider} to provide access to all the configs registered for each application.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class ConfigIntrospector extends SensitiveIntrospector implements Introspector {

    @Reference
    private ConfigIntrospectionProvider introspectionProvider;

    /**
     * We have no metadata to indicate whether a given config value is sensitive, so this is a basic heuristic to avoid things which are likely to be sensitive, in addition to the
     * protection offered by SensitiveIntrospector.
     */
    private final static Pattern MAYBE_SENSITIVE = Pattern.compile("password|pass$|pwd|key$", Pattern.CASE_INSENSITIVE);

    @Override
    public String getIntrospectorName() {
        return "MicroProfileConfig";
    }

    @Override
    public String getIntrospectorDescription() {
        return "Lists the config properties available to each application via MicroProfile Config";
    }

    @Override
    public void introspect(PrintWriter pw) {
        Map<String, Set<Config>> configs = introspectionProvider.getConfigsByApplication();
        for (Entry<String, Set<Config>> entry : configs.entrySet()) {
            introspectApp(pw, entry.getKey(), entry.getValue());
        }
    }

    private void introspectApp(PrintWriter pw, String appName, Set<Config> configs) {
        pw.println();
        pw.println("########################");
        pw.println("Config for " + appName);
        pw.println("########################");

        for (Config config : configs) {
            introspectConfig(pw, config);
        }
    }

    private void introspectConfig(PrintWriter pw, Config config) {
        try {
            pw.println("Config: " + config.toString());
            for (ConfigSource source : config.getConfigSources()) {
                introspectConfigSource(pw, source);
            }
        } catch (Exception e) {
            pw.printf("Exception introspecting config: %s", e);
        }
    }

    private void introspectConfigSource(PrintWriter pw, ConfigSource source) {
        try {
            pw.printf(" - %s config source (ordinal %d)\n", source.getName(), source.getOrdinal());

            Map<String, String> properties = source.getProperties();
            if (properties == null) {
                pw.println("     Cannot list properties from this config source because ConfigSource.getProperties() returned null.");
                return;
            }

            for (Entry<String, String> entry : properties.entrySet()) {
                String obscuredValue = getObscuredValue(entry.getKey(), entry.getValue());
                pw.printf("   - %s = %s\n", entry.getKey(), obscuredValue);
            }
        } catch (Exception e) {
            pw.printf("Exception introspecting config source: %s\n", e);
        }
    }

    @Override
    @Sensitive
    protected String getObscuredValue(String key, Object value) {
        if (MAYBE_SENSITIVE.matcher(key).find()) {
            return "*****";
        }
        return super.getObscuredValue(key, value);
    }
}
