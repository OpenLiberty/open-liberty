/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.encoders;

import com.ibm.websphere.csi.J2EEName;

import io.openliberty.mcp.internal.AbstractModuleScopedStore;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Manages encoder registries, one of these is maintained per McpCdiExtension (application instance)
 */
@ApplicationScoped
public class EncoderRegistries extends AbstractModuleScopedStore<EncoderRegistry> {

    // Application registry for EAR/lib shared encoders (no parent)
    private final EncoderRegistry globalRegistry = new EncoderRegistry();

    /**
     * Returns the global encoder registry for this application.
     * This registry contains encoders from EAR/lib that are shared across all modules.
     *
     * @return the global encoder registry
     */
    public EncoderRegistry getGlobal() {
        return globalRegistry;
    }

    @Override
    protected EncoderRegistry createInstance(J2EEName moduleName) {
        return new EncoderRegistry(globalRegistry);
    }
}
