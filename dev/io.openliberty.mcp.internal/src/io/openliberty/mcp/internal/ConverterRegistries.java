/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import com.ibm.websphere.csi.J2EEName;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Manages converter registries, one per module, with a global registry for EAR/lib converters.
 * <p>
 * In multi-module EAR deployments, each WAR or EJB module maintains its own converter registry
 * that can access both module-specific converters and shared converters from the global registry.
 * </p>
 */
@ApplicationScoped
public class ConverterRegistries extends AbstractModuleScopedStore<ConverterRegistry> {

    // Global registry for EAR/lib shared converters (no parent)
    private final ConverterRegistry globalRegistry = new ConverterRegistry();

    /**
     * Returns the global converter registry for this application.
     * This registry contains converters from EAR/lib that are shared across all modules.
     *
     * @return the global converter registry
     */
    public ConverterRegistry getGlobal() {
        return globalRegistry;
    }

    @Override
    protected ConverterRegistry createInstance(J2EEName moduleName) {
        return new ConverterRegistry(globalRegistry);
    }

    @PreDestroy
    public void cleanup() {
        // Destroy any @Dependent beans for each ConverterRegistry
        getAll().forEach(ConverterRegistry::cleanup);
        globalRegistry.cleanup();
    }
}

// Made with Bob
