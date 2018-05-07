/*
 *******************************************************************************
 * Copyright (c) 2016-2017 Contributors to the Eclipse Foundation
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
 *   2016-07-20 - Romain Manni-Bucau
 *      Initial ConfigBuilder PR    0945b23cbf9dadb75fb9
 *   2016-07-17 - Mark Struberg
 *      Merged and JavaDoc          c8525998a43fe798f367
 *   2016-11-14 - Emily Jiang / IBM
 *      API improvements + JavaDoc  f53258b8eca1253fee52
 *
 *******************************************************************************/
package org.eclipse.microprofile.config.spi;

import org.eclipse.microprofile.config.Config;

/**
 * Builder for manually creating an instance of a {@code Config}.
 *
 * @see ConfigProviderResolver#getBuilder()
 *
 * @author <a href="mailto:rmannibucau@apache.org">Romain Manni-Bucau</a>
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 */
@org.osgi.annotation.versioning.ProviderType
public interface ConfigBuilder {
    /**
     * Add the default config sources appearing on the builder's classpath
     * including:
     * <ol>
     * <li>System properties</li>
     * <li>Environment properties</li>
     * <li>/META-INF/microprofile-config.properties</li>
     * </ol>
     *
     * @return the ConfigBuilder with the default config sources
     */
    ConfigBuilder addDefaultSources();

    /**
     * Add the config sources appearing to be loaded via service loader pattern
     *
     * @return the ConfigBuilder with the autodiscovered config sources
     */
    ConfigBuilder addDiscoveredSources();

    /**
     * Add the converters to be loaded via service loader pattern
     *
     * @return the ConfigBuilder with the autodiscovered converters
     */
    ConfigBuilder addDiscoveredConverters();
    /**
     * Return the ConfigBuilder for a given classloader
     *
     * @param loader the specified classloader
     * @return the ConfigureBuilder for the given classloader
     */
    ConfigBuilder forClassLoader(ClassLoader loader);

    /**
     * Add the specified {@link ConfigSource}.
     *
     * @param sources the config sources
     * @return the ConfigBuilder with the configured sources
     */
    ConfigBuilder withSources(ConfigSource... sources);

    /**
     * Add the specified {@link Converter}.
     * This method uses reflection to determine what type the converter is for.
     * When using lambda expressions for custom converters you should use
     * {@link #withConverter(Class, int, Converter)} and pass the target type explicitly
     * as lambda expressions do not offer enough type information to the reflection API.
     *
     * @param converters the converters
     * @return the ConfigBuilder with the added converters
     */
    ConfigBuilder withConverters(Converter<?>... converters);


    /**
     * Add the specified {@link Converter} for the given type.
     * This method does not rely on reflection to determine what type the converter is for
     * therefore also lambda expressions can be used.
     *
     * @param type the Class of type to convert
     * @param priority the priority of the converter (custom converters have a default priority of 100).
     * @param converter the converter (can not be {@code null})
     * @param <T> the type to convert
     * @return the ConfigBuilder with the added converters
     */
    <T> ConfigBuilder withConverter(Class<T> type, int priority, Converter<T> converter);

    /**
     * Build the {@link Config} object.
     *
     * @return the Config object
     */
    Config build();
}
