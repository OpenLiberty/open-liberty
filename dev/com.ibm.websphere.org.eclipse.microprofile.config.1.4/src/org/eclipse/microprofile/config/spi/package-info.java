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
 *******************************************************************************/

/**
 * <p>This package contains classes which are used to extend the standard functionality in a portable way.
 * <p>A user can provide own {@link org.eclipse.microprofile.config.spi.ConfigSource ConfigSources} and
 * {@link org.eclipse.microprofile.config.spi.Converter Converters} to extend the information available in the Config.
 *
 * <p>The package also contains the class {@link org.eclipse.microprofile.config.spi.ConfigProviderResolver}
 * which is used to pick up the actual implementation.
 * <h2>Usage</h2>
 * <p>This is used to build up a builder and manually add {@link org.eclipse.microprofile.config.spi.ConfigSource ConfigSources}..</p>
 *
 *
 * <ol>
 * <li>Create a builder:
 * <pre>ConfigProviderResolver resolver = ConfigProviderResolver.instance(); </pre>
 * <pre>ConfigBuilder builder = resolver.getBuilder();</pre>
 * </li>
 * <li>Add config sources and build:
 * <pre>
 * Config config = builder.addDefaultSources().withSources(mySource).withConverters(myConverter).build;
 * </pre>
 * </li>
 * <li>(optional)Manage the lifecycle of the config
 * <pre> resolver.registerConfig(config, classloader);</pre>
 * <pre> resolver.releaseConfig(config);</pre>
 * </li>
 * </ol>
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 * 
 */
@org.osgi.annotation.versioning.Version("1.4")
package org.eclipse.microprofile.config.spi;
