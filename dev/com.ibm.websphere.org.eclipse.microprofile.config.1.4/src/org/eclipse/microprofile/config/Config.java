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
 *   String archiveUrl = cfg.getString("my.project.archive.endpoint", String.class);
 *   Integer archivePort = cfg.getValue("my.project.archive.port", Integer.class);
 * </pre>
 *
 * <p>For accessing a configuration in a dynamic way you can also use {@link #access(String)}.
 * This method returns a builder-style {@link ConfigAccessor} instance for the given key.
 * You can further specify a Type of the underlying configuration, a cache time, lookup paths and
 * many more.
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
 * <p>See {@link #getValue(String, Class)} and {@link #getOptionalValue(String, Class)} and {@link #access(String)} for accessing a configured value.
 *
 * <p>Configured values can also be accessed via injection.
 * See {@link org.eclipse.microprofile.config.inject.ConfigProperty} for more information.
 *
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 * @author <a href="mailto:gpetracek@apache.org">Gerhard Petracek</a>
 * @author <a href="mailto:rsmeral@apache.org">Ron Smeral</a>
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 * @author <a href="mailto:gunnar@hibernate.org">Gunnar Morling</a>
 * @author <a href="mailto:manfred.huber@downdrown.at">Manfred Huber</a>
 * @author <a href="mailto:elexx@apache.org">Alex Falb</a>
 *
 */
@org.osgi.annotation.versioning.ProviderType
public interface Config {

    /**
     * Return the resolved property value with the specified type for the
     * specified property name from the underlying {@link ConfigSource ConfigSources}.
     *
     * If this method gets used very often then consider to locally store the configured value.
     *
     * <p>Note that no variable replacement like in {@link ConfigAccessor#evaluateVariables(boolean)} will be performed!
     *
     * @param <T>  The property type
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
     * <p>Note that no variable replacement like in {@link ConfigAccessor#evaluateVariables(boolean)} will be performed!
     *
     * @param <T>  The property type
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
     * Create a {@link ConfigAccessor} to access the underlying configuration.
     *
     * @param propertyName the property key
     * @return a {@code ConfigAccessor} to access the given propertyName
     */
    ConfigAccessor<String> access(String propertyName);

    /**
     * <p>This method can be used to access multiple
     * {@link ConfigAccessor} which must be consistent.
     * The returned {@link ConfigSnapshot} is an immutable object which contains all the
     * resolved values at the time of calling this method.
     *
     * <p>An example would be to access some {@code 'myapp.host'} and {@code 'myapp.port'}:
     * The underlying values are {@code 'oldserver'} and {@code '8080'}.
     *
     * <pre>
     *     // get the current host value
     *     ConfigAccessor&lt;String&gt; hostCfg config.resolve("myapp.host")
     *              .cacheFor(60, TimeUnit.MINUTES);
     *
     *     // and right inbetween the underlying values get changed to 'newserver' and port 8082
     *
     *     // get the current port for the host
     *     ConfigAccessor&lt;Integer&gt; portCfg config.resolve("myapp.port")
     *              .as(Integer.class)
     *              .cacheFor(60, TimeUnit.MINUTES);
     * </pre>
     *
     * In ths above code we would get the combination of {@code 'oldserver'} but with the new port {@code 8081}.
     * And this will obviously blow up because that host+port combination doesn't exist.
     *
     * To consistently access n different config values we can start a {@link ConfigSnapshot} for those values.
     *
     * <pre>
     *     ConfigSnapshot cfgSnap = config.createSnapshot(hostCfg, portCfg);
     *
     *     String host = hostCfg.getValue(cfgSnap);
     *     Integer port = portCfg.getValue(cfgSnap);
     * </pre>
     *
     * Note that there is no <em>close</em> on the snapshot.
     * They should be used as local variables inside a method.
     * Values will not be reloaded for an open {@link ConfigSnapshot}.
     *
     * @param configValues the list of {@link ConfigAccessor} to be accessed in an atomic way
     *
     * @return a new {@link ConfigSnapshot} which holds the resolved values of all the {@code configValues}.
     */
    ConfigSnapshot snapshotFor(ConfigAccessor<?>... configValues);


    /**
     * Return all property names used in any of the underlying {@link ConfigSource ConfigSources}.
     * @return the names of all configured keys of the underlying configuration.
     */
    Iterable<String> getPropertyNames();

    /**
     * @return all currently registered {@link ConfigSource configsources} sorted with descending ordinal and ConfigSource name
     */
    Iterable<ConfigSource> getConfigSources();
}
