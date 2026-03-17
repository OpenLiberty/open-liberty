/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import io.openliberty.cdi.spi.CDIExtensionMetadata;
import io.openliberty.mcp.internal.config.McpConfigProducer;
import io.openliberty.mcp.internal.encoders.JsonTextContentEncoder;
import io.openliberty.mcp.internal.sessions.McpSessionStores;
import jakarta.enterprise.inject.spi.Extension;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class McpCdiExtensionMetadata implements CDIExtensionMetadata {

    /**
     * Tells Liberty's CDI runtime: "For every CDI-enabled module, instantiate McpCdiExtension and call its observer methods"
     * Each module gets its own instance of McpCdiExtension
     * This is how McpCdiExtension discovers tools and encoders in each module
     *
     * @return
     */
    @Override
    public Set<Class<? extends Extension>> getExtensions() {
        return Set.of(McpCdiExtension.class);
    }

    /*
     * Tells Liberty's CDI runtime: "Make these classes available as CDI beans in every module"
     * These beans are automatically discovered and injectable in all modules
     */
    @Override
    public Set<Class<?>> getBeanClasses() {
        return Set.of(McpRequestTrackers.class,
                      McpSessionStores.class,
                      JsonTextContentEncoder.class,
                      ConverterRegistry.class,
                      McpCdiProducers.class,
                      McpConfigProducer.class);
    }
}
