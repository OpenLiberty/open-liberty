/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.spi;

import java.util.HashMap;
import java.util.Map;

import org.mcpjava.server.MetaCarrier;

/**
 * Base class for builders for objects which implement {@link MetaCarrier}
 *
 * @param <THIS> the actual builder class
 */
public class MetaCarrierBuilderImpl<THIS extends MetaCarrierBuilderImpl<THIS>> {

    protected Map<String, Object> metadata = new HashMap<>();

    @SuppressWarnings("unchecked")
    public THIS putMetadata(String key, Object value) {
        metadata.put(key, value);
        return (THIS) this;
    }

    @SuppressWarnings("unchecked")
    public THIS setMetadata(Map<String, Object> metadata) {
        if (metadata == null) {
            this.metadata = new HashMap<>();
        } else {
            this.metadata = new HashMap<>(metadata);
        }
        return (THIS) this;
    }
}