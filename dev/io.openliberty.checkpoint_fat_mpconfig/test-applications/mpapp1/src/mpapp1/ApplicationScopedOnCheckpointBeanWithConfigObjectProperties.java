/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package mpapp1;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;

@ApplicationScoped
public class ApplicationScopedOnCheckpointBeanWithConfigObjectProperties {

    @Inject
    Config config;

    public void observeInit(@Observes @Initialized(ApplicationScoped.class) Object event) {
        System.out.println(getClass() + ": " + "Initializing application context");
        check("defaultValue");
    }

    public void appScopeDefaultValueTest() {
        check("defaultValue");
    }

    public void appScopeEnvValueTest() {
        check("envValue");
    }

    public void appScopeServerValueTest() {
        check("serverValue");
    }

    private void check(String expected) {
        String actual = "";
        int highestOrdinal = 0;

        Iterable<ConfigSource> configSources = config.getConfigSources();
        for (Iterator<ConfigSource> iSources = configSources.iterator(); iSources.hasNext();) {
            ConfigSource source = iSources.next();
            String value = source.getProperties().get("config_object_properties_app_scope_key");
            if (source.getOrdinal() > highestOrdinal && value != null) {
                highestOrdinal = source.getOrdinal();
                actual = value;
            }
        }
        assertEquals("Wrong value for test key.", expected, actual);
    }
}
