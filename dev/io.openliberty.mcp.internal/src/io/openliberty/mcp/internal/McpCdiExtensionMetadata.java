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
import io.openliberty.mcp.internal.encoders.EncoderRegistry;
import io.openliberty.mcp.internal.encoders.JsonTextContentEncoder;
import io.openliberty.mcp.internal.sessions.McpSessionStore;
import jakarta.enterprise.inject.spi.Extension;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class McpCdiExtensionMetadata implements CDIExtensionMetadata {

    @Override
    public Set<Class<? extends Extension>> getExtensions() {
        return Set.of(McpCdiExtension.class);
    }

    @Override
    public Set<Class<?>> getBeanClasses() {
        return Set.of(McpRequestTracker.class,
                      McpSessionStore.class,
                      JsonTextContentEncoder.class,
                      EncoderRegistry.class,
                      McpCdiProducers.class,
                      McpConfigProducer.class);
    }

}
