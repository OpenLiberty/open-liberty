/*
 ******************************************************************************
 * Copyright (c) 2009-2019 Contributors to the Eclipse Foundation
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


import java.time.Duration;

import org.eclipse.microprofile.config.spi.Converter;

 

/**
 * Accessor to a configured value.
 *
 * It follows a builder-like pattern to define in which ways to access the configured value
 * of a certain property name.
 *
 * Accessing the configured value is finally done via {@link ConfigAccessor#getValue()}
 *
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 * @author <a href="mailto:gpetracek@apache.org">Gerhard Petracek</a>
 * @author <a href="mailto:tomas.langer@oracle.com">Tomas Langer</a>
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 */
public interface ConfigAccessorBuilder<T> {


    /**
     * Defines a specific {@link Converter} to be used instead of applying the default Converter resolving logic.
     *
     * @param converter The converter for the target type
     * @return This builder as a typed ConfigAccessor
     */
    ConfigAccessorBuilder<T> useConverter(Converter<T> converter);

    /**
     * Sets the default value to use in case the resolution returns null.
     * @param value the default value
     * @return This builder
     */
    ConfigAccessorBuilder<T> withDefault(T value);

    /**
     * Sets the default value to use in case the resolution returns null. Converts the given String to the type of
     * this resolver using the same method as used for the configuration entries.
     * @param value string value to be converted and used as default
     * @return This builder
     */
    ConfigAccessorBuilder<T> withStringDefault(String value);

    /**
     * Specify that a resolved value will get cached for a certain maximum amount of time.
     * After the time expires the next {@link ConfigAccessor#getValue()} will again resolve the value
     * from the underlying {@link Config}.
     *
     * Note that that the cache will get flushed if a {@code ConfigSource} notifies
     * the underlying {@link Config} about a value change.
     * This is done by invoking the callback provided to the {@code ConfigSource} via
     * {@link org.eclipse.microprofile.config.spi.ConfigSource#onAttributeChange(java.util.function.Consumer)}.
     *
     * @param duration the duration (e.g. 3s) to cache for
     * @return This builder
     */
    ConfigAccessorBuilder<T> cacheFor(Duration duration);

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
    ConfigAccessorBuilder<T> evaluateVariables(boolean evaluateVariables);

    /**
     * Build a ConfigAccessor 
     * @return the configAccessor
     */
    ConfigAccessor<T> build();

}
