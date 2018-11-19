/*
 ******************************************************************************
 * Copyright (c) 2009-2018 Contributors to the Eclipse Foundation
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
 *      Contributed to Apache DeltaSpike fb0131106481f0b9a8fd
 *   2016-07-07 - Mark Struberg
 *      Extracted the Config part out of DeltaSpike and proposed as Microprofile-Config 8ff76eb3bcfaf4fd
 *
 *******************************************************************************/
package org.eclipse.microprofile.config;


import org.eclipse.microprofile.config.spi.Converter;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 * Accessor to a configured value.
 *
 * It follows a builder-like pattern to define in which ways to access the configured value
 * of a certain property name.
 *
 * Accessing the configured value is finally done via {@link #getValue()}
 *
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 * @author <a href="mailto:gpetracek@apache.org">Gerhard Petracek</a>
 * @author <a href="mailto:tomas.langer@oracle.com">Tomas Langer</a>
 */
public interface ConfigAccessor<T> {

    /**
     * Sets the type of the configuration entry to the given class and returns this builder.
     * The default type of a ConfigAccessor is {@code String}.
     *
     * <p>Usage:
     * <pre>
     * Integer timeout = config.access("some.timeout")
     *                         .as(Integer.class)
     *                         .getValue();
     * </pre>
     *
     * Attention: this method should always be the first to be used and might
     * return a new {@code ConfigAccessor} for the given target clazz.
     *
     *
     * @param clazz The target type
     * @param <N> The target type
     * @return This builder as a typed ConfigAccessor
     */
//    <N> ConfigAccessor<N> as(Class<N> clazz);

    /**
     * Declare the ConfigAccessor to return a List of the given Type.
     * When getting value it will be split on each comma (',') character.
     * If a comma is contained in the values it must get escaped with a preceding backslash (&quot;\,&quot;).
     * Any backslash needs to get escaped via double-backslash (&quot;\\&quot;).
     * Note that in property files this leads to &quot;\\\\&quot; as properties escape themselves.
     *
     * @return a ConfigAccessor for a list of configured comma separated values
     */
//    ConfigAccessor<List<T>> asList();

    /**
     * Declare the ConfigAccessor to return a Set of the given Type.
     * The notation and escaping rules are the same like explained in {@link #asList()}
     *
     * @return a ConfigAccessor for a list of configured comma separated values
     */
//    ConfigAccessor<Set<T>> asSet();

    /**
     * Defines a specific {@link Converter} to be used instead of applying the default Converter resolving logic.
     *
     * @param converter The converter for the target type
     * @return This builder as a typed ConfigAccessor
     */
//    ConfigAccessor<T> useConverter(Converter<T> converter);

    /**
     * Sets the default value to use in case the resolution returns null.
     * @param value the default value
     * @return This builder
     */
//    ConfigAccessor<T> withDefault(T value);

    /**
     * Sets the default value to use in case the resolution returns null. Converts the given String to the type of
     * this resolver using the same method as used for the configuration entries.
     * @param value string value to be converted and used as default
     * @return This builder
     */
//    ConfigAccessor<T> withStringDefault(String value);

    /**
     * Specify that a resolved value will get cached for a certain maximum amount of time.
     * After the time expires the next {@link #getValue()} will again resolve the value
     * from the underlying {@link org.eclipse.microprofile.config.Config}.
     *
     * Note that that the cache will get flushed if a {@code ConfigSource} notifies
     * the underlying {@link Config} about a value change.
     * This is done by invoking the callback provided to the {@code ConfigSource} via
     * {@link org.eclipse.microprofile.config.spi.ConfigSource#setOnAttributeChange(java.util.function.Consumer)}.
     *
     * @param value the amount of the TimeUnit to wait
     * @param timeUnit the TimeUnit for the value
     * @return This builder
     */
    ConfigAccessor<T> cacheFor(long value, TimeUnit timeUnit);

    /**
     * Whether to evaluate variables in configured values.
     * A variable starts with '${' and ends with '}', e.g.
     * <pre>
     * mycompany.some.url=${myserver.host}/some/path
     * myserver.host=http://localhost:8081
     * </pre>
     * If 'evaluateVariables' is enabled, the result for the above key
     * {@code "mycompany.some.url"} would be:
     * {@code "http://localhost:8081/some/path"}
     *
     * <p><b>ATTENTION:</b> This defaults to {@code true}! That means variable replacement is enabled by default!</p>
     *
     * @param evaluateVariables whether to evaluate variables in values or not
     *
     * @return This builder
     */
    ConfigAccessor<T> evaluateVariables(boolean evaluateVariables);

    /**
     * The methods {@link #addLookupSuffix(String)} and {@link #addLookupSuffix(ConfigAccessor)}
     * append the given parameters as optional suffixes to the {@link #getPropertyName()}.
     * Those methods can be called multiple times.
     * Each time the given suffix will be added to the end of suffix chain.
     *
     * This very version
     *
     * <p>Usage:
     * <pre>
     * String tenant = getCurrentTenant();
     *
     * Integer timeout = config.access("some.server.url")
     *                         .addLookupSuffix(tenant)
     *                         .addLookupSuffix(config.access("javax.config.projectStage"))
     *                         .getValue();
     * </pre>
     *
     * Given the current tenant name is 'myComp' and the property
     * {@code javaconfig.projectStage} is 'Production' this would lead to the following lookup order:
     *
     * <ul>
     *     <li>"some.server.url.myComp.Production"</li>
     *     <li>"some.server.url.myComp"</li>
     *     <li>"some.server.url.Production"</li>
     *     <li>"some.server.url"</li>
     * </ul>
     *
     * The algorithm to use in {@link #getValue()} is a binary count down.
     * Every parameter is either available (1) or not (0).
     * Having 3 parameters, we start with binary {@code 111} and count down to zero.
     * The first combination which resolves to a result is being treated as result.
     *
     * @param suffixValue fixed String to be used as suffix
     * @return This builder
     */
    ConfigAccessor<T> addLookupSuffix(String suffixValue);

    /**
     *
     * @param suffixAccessor {@link ConfigAccessor} to be used to resolve the suffix.
     * @return This builder
     * @see #addLookupSuffix(String)
     */
    ConfigAccessor<T> addLookupSuffix(ConfigAccessor<String> suffixAccessor);

    /**
     * Returns the converted resolved filtered value.
     * @return the resolved value
     *
     * @throws IllegalArgumentException if the property cannot be converted to the specified type.
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration.
     */
    T getValue();


    /**
     * Returns the value from a previously taken {@link ConfigSnapshot}.
     *
     * @param configSnapshot previously taken via {@link Config#snapshotFor(ConfigAccessor[])}
     * @return the resolved Value
     * @see Config#snapshotFor(ConfigAccessor...)
     * @throws IllegalArgumentException if the {@link ConfigSnapshot} hasn't been resolved
     *          for this {@link ConfigAccessor}
     */
    T getValue(ConfigSnapshot configSnapshot);

    /**
     * Returns the converted resolved filtered value.
     * @return the resolved value as Optional
     *
     * @throws IllegalArgumentException if the property cannot be converted to the specified type.
     */
    Optional<T> getOptionalValue();

    /**
     * Returns the value from a previously taken {@link ConfigSnapshot}.
     *
     * @param configSnapshot previously taken via {@link Config#snapshotFor(ConfigAccessor[])}
     * @return the resolved Value as Optional
     * @see Config#snapshotFor(ConfigAccessor...)
     * @throws IllegalArgumentException if the property cannot be converted to the specified type.
     */
    Optional<T> getOptionalValue(ConfigSnapshot configSnapshot);
    
    /**
     * Returns the property name key given in {@link Config#access(String)}.
     * @return the original property name
     */
    String getPropertyName();

    /**
     * Returns the actual key which led to successful resolution and corresponds to the resolved value.
     * This is useful when {@link #addLookupSuffix(String)} is used.
     * Otherwise the resolved key should always be equal to the original key.
     * This method is provided for cases, when parameterized resolution is
     * requested and some of the fallback keys is used.
     *
     * This should be called only after calling {@link #getValue()} otherwise the value is undefined (but likely
     * null).
     *
     * Note that this will only give you the resolved key from the last non-cached value resolving.
     * @return the actual property name which led to successful resolution and corresponds to the resolved value.
     */
//    String getResolvedPropertyName();

    /**
     * Returns the default value provided by {@link #withDefault(Object)} or {@link #withStringDefault(String)}.
     *
     * @return the default value or {@code null} if no default was provided.
     */
//    T getDefaultValue();

}
