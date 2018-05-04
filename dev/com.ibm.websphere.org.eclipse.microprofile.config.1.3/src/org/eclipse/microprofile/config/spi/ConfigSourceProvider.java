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
 *      Initially authored in Apache DeltaSpike fb0131106481f0b9a8fd
 *   2016-07-14 - Mark Struberg
 *      Extracted the Config part out of Apache DeltaSpike and proposed as Microprofile-Config
 *   2016-11-14 - Emily Jiang / IBM Corp
 *      Methods renamed, JavaDoc and cleanup
 *
 *******************************************************************************/

package org.eclipse.microprofile.config.spi;

/**
 * <p>Implement this interfaces to provide multiple ConfigSources.
 * This is e.g. needed if there are multiple property files of a given name on the classpath
 * but they are not all known at compile time.
 *
 * <p>If a single ConfigSource exists, then there is no need
 * to register it using a custom implementation of ConfigSourceProvider, it can be
 * registered directly as a {@link ConfigSource}.
 *
 * <p>A ConfigSourceProvider will get picked up via the
 * {@link java.util.ServiceLoader} mechanism and can be registered by providing a
 * {@code META-INF/services/org.eclipse.microprofile.config.spi.ConfigSourceProvider} file which contains
 * the fully qualified classname of the custom ConfigSourceProvider.
 *
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 * @author <a href="mailto:gpetracek@apache.org">Gerhard Petracek</a>
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 *
 */
public interface ConfigSourceProvider {

    /**
     * Return the collection of {@link ConfigSource}s.
     * For each e.g. property file, we return a single ConfigSource or an empty list if no ConfigSource exists.
     *
     * @param forClassLoader the classloader which should be used if any is needed
     * @return the {@link ConfigSource ConfigSources} to register within the {@link org.eclipse.microprofile.config.Config}.
     */
    Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader);
}
