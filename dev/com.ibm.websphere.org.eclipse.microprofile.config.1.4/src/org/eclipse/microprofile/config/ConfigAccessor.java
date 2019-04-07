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


import java.util.Optional;


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
     * Returns the value from a previously taken {@link ConfigSnapshot}.
     *
     * @param configSnapshot previously taken via {@link Config#snapshotFor(ConfigAccessor[])}
     * @return the resolved value as Optional
     * @see Config#snapshotFor(ConfigAccessor...)
     * @throws IllegalArgumentException if the {@link ConfigSnapshot} hasn't been resolved
     *          for this {@link ConfigAccessor}
     */
    Optional<T> getOptionalValue(ConfigSnapshot configSnapshot);
    

    /**
     * Returns the converted resolved filtered value.
     * @return the resolved value as Optional
     *
     * @throws IllegalArgumentException if the property cannot be converted to the specified type.
     */
    Optional<T> getOptionalValue();

    /**
     * Returns the property name key given in {@link Config#access(String, Class)}.
     * @return the original property name
     */
    String getPropertyName();

    /**
     * Returns the default value provided by {@link ConfigAccessorBuilder#withDefault(Object)} 
     * or {@link ConfigAccessorBuilder#withStringDefault(String)}.
     *
     * @return the default value or {@code null} if no default was provided.
     */
    T getDefaultValue();

}

