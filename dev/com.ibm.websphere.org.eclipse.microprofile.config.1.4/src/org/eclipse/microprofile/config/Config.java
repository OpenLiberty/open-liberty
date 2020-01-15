/*
 *******************************************************************************
 * Copyright (c) 2011-2019 Contributors to the Eclipse Foundation
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
 *   2011-12-28 - Mark Struberg & Gerhard Petracek
 *      Initially authored in Apache DeltaSpike as ConfigResolver fb0131106481f0b9a8fd
 *   2015-04-30 - Ron Smeral
 *      Typesafe Config authored in Apache DeltaSpike 25b2b8cc0c955a28743f
 *   2016-07-14 - Mark Struberg
 *      Extracted the Config part out of Apache DeltaSpike and proposed as Microprofile-Config
 *   2016-11-14 - Emily Jiang / IBM Corp
 *      Experiments with separate methods per type, JavaDoc, method renaming
 *   2018-04-04 - Mark Struberg, Manfred Huber, Alex Falb, Gerhard Petracek
 *      ConfigSnapshot added. Initially authored in Apache DeltaSpike fdd1e3dcd9a12ceed831dd
 *      Additional reviews and feedback by Tomas Langer.
 *
 *******************************************************************************/

package org.eclipse.microprofile.config;

import java.util.Optional;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * <p>
 * Resolves the property value by searching through all configured
 * {@link ConfigSource ConfigSources}. If the same property is specified in multiple
 * {@link ConfigSource ConfigSources}, the value in the {@link ConfigSource} with the highest
 * ordinal will be used.
 * <p>If multiple {@link ConfigSource ConfigSources} are specified with
 * the same ordinal, the {@link ConfigSource#getName()} will be used for sorting.
 * <p>
 * The config objects produced via the injection model {@code @Inject Config} are guaranteed to be serializable, while
 * the programmatically created ones are not required to be serializable.
 * <p>
 * If one or more converters are registered for a class of a requested value then the registered {@link org.eclipse.microprofile.config.spi.Converter}
 * which has the highest {@code @javax.annotation.Priority} is used to convert the string value retrieved from the config sources.
 *
 * <h3>Usage</h3>
 *
 * For accessing the config you can use the {@link ConfigProvider}:
 *
 * <pre>
 * public void doSomething(
 *   Config cfg = ConfigProvider.getConfig();
 *   String archiveUrl = cfg.getValue("my.project.archive.endpoint", String.class);
 *   Integer archivePort = cfg.getValue("my.project.archive.port", Integer.class);
 * </pre>
 *
 * <p>It is also possible to inject the Config if a DI container is available:
 *
 * <pre>
 * public class MyService {
 *     &#064;Inject
 *     private Config config;
 * }
 * </pre>
 *
 * <p>See {@link #getValue(String, Class)} and {@link #getOptionalValue(String, Class)} for accessing a configured value.
 *
 * <p>Configured values can also be accessed via injection.
 * See {@link org.eclipse.microprofile.config.inject.ConfigProperty} for more information.
 *
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 * @author <a href="mailto:gpetracek@apache.org">Gerhard Petracek</a>
 * @author <a href="mailto:rsmeral@apache.org">Ron Smeral</a>
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 * @author <a href="mailto:gunnar@hibernate.org">Gunnar Morling</a>
 */
@org.osgi.annotation.versioning.ProviderType
public interface Config {

    /**
     * Return the resolved property value with the specified type for the
     * specified property name from the underlying {@link ConfigSource ConfigSources}.
     *
     * If this method gets used very often then consider to locally store the configured value.
     *
     * @param <T>
     *             The property type
     * @param propertyName
     *             The configuration propertyName.
     * @param propertyType
     *             The type into which the resolve property value should get converted
     * @return the resolved property value as an object of the requested type.
     * @throws java.lang.IllegalArgumentException if the property cannot be converted to the specified type.
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration.
     */
    <T> T getValue(String propertyName, Class<T> propertyType);

    /**
     * Return the resolved property value with the specified type for the
     * specified property name from the underlying {@link ConfigSource ConfigSources}.
     *
     * If this method is used very often then consider to locally store the configured value.
     *
     * @param <T>
     *             The property type
     * @param propertyName
     *             The configuration propertyName.
     * @param propertyType
     *             The type into which the resolve property value should be converted
     * @return The resolved property value as an Optional of the requested type.
     *
     * @throws java.lang.IllegalArgumentException if the property cannot be converted to the specified type.
     */
    <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType);

    /**
     * Return all property names used in any of the underlying {@link ConfigSource ConfigSources}.
     * @return the names of all configured keys of the underlying configuration.
     */
    Iterable<String> getPropertyNames();

    /**
     * @return all currently registered {@link ConfigSource ConfigSources} sorted by descending ordinal and ConfigSource name
     */
    Iterable<ConfigSource> getConfigSources();
}
