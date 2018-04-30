/*
 *******************************************************************************
 * Copyright (c) 2011-2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *   2016-07-14 - Mark Struberg
 *      Initial revision            cf41cf130bcaf5447ff8
 *   2016-07-20 - Romain Manni-Bucau
 *      Initial ConfigBuilder PR    0945b23cbf9dadb75fb9
 *   2016-11-14 - Emily Jiang / IBM Corp
 *      SPI reworked into own ConfigProviderResolver
 *   2016-12-02 - Viktor Klang
 *      removed ConfigFilter and security related functionality.
 *
 *******************************************************************************/

package org.eclipse.microprofile.config;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

/**
 * <p>
 * This is the central class to access a {@link Config}.
 * A {@link Config} provides access to application Configuration.
 * That might be auto-discovered {@code Config} or even manually created one.
 *
 * <p>
 * The default usage is to use {@link #getConfig()} to automatically pick up the
 * 'Configuration' for the Thread Context ClassLoader (See
 * {@link Thread#getContextClassLoader()}).
 *
 * <p>
 * A 'Configuration' consists of the information collected from the registered {@link org.eclipse.microprofile.config.spi.ConfigSource ConfigSources}.
 * These {@link org.eclipse.microprofile.config.spi.ConfigSource ConfigSources} get sorted according to
 * their <em>ordinal</em> defined via {@link org.eclipse.microprofile.config.spi.ConfigSource#getOrdinal()}.
 * Thus it is possible to overwrite configuration by providing in a ConfigSource with higher importance from outside.
 *
 * <p>
 * It is also possible to register custom {@link org.eclipse.microprofile.config.spi.ConfigSource ConfigSources} to flexibly
 * extend the configuration mechanism. An example would be to pick up
 * configuration values from a database table.
 *
 * Example usage:
 *
 * <pre>
 * String restUrl = ConfigProvider.getConfig().getValue(&quot;myproject.some.remote.service.url&quot;, String.class);
 * Integer port = ConfigProvider.getConfig().getValue(&quot;myproject.some.remote.service.port&quot;, Integer.class);
 * </pre>
 *
 * For more advanced use cases like e.g. registering a manually created {@link Config} please see
 * {@link ConfigProviderResolver#registerConfig(Config, ClassLoader)} and {@link ConfigProviderResolver#getBuilder()}.
 *
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 * @author <a href="mailto:rmannibucau@apache.org">Romain Manni-Bucau</a>
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 * @author <a href="mailto:viktor.klang@gmail.com">Viktor Klang</a>
 */
public final class ConfigProvider {
    private static final ConfigProviderResolver INSTANCE = ConfigProviderResolver.instance();

    private ConfigProvider() {
    }

    /**
     * Provide a {@link Config} based on all {@link org.eclipse.microprofile.config.spi.ConfigSource ConfigSources} of the
     * current Thread Context ClassLoader (TCCL)
     * <p>
     * 
     * <p>
     * 
     * The {@link Config} will be stored for future retrieval.
     * <p>
     * There is exactly a single Config instance per ClassLoader
     *
     * @return the config object for the thread context classloader
     */
    public static Config getConfig() {
        return INSTANCE.getConfig();
    }

    /**
     * Provide a {@link Config} based on all {@link org.eclipse.microprofile.config.spi.ConfigSource ConfigSources} of the
     * specified ClassLoader
     *
     * <p>
     * There is exactly a single Config instance per ClassLoader
     *
     * @param cl the specified classloader
     * @return the config for the specified classloader
     */
    public static Config getConfig(ClassLoader cl) {
        return INSTANCE.getConfig(cl);
    }
}
