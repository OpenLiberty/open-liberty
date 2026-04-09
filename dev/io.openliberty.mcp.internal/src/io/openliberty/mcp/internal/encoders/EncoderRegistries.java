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

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 * Manages encoder registries, one of these is maintained per McpCdiExtension (application instance)
 */
public class EncoderRegistries {

    //map of registries per Module
    private ConcurrentHashMap<J2EEName, EncoderRegistry> encoderRegistries = new ConcurrentHashMap<>();

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

    /**
     * Retrieves the encoder registry for the current thread's module context.
     * Use this in request-handling code where module context is available.
     *
     * @return the encoder registry for the current module
     */
    public EncoderRegistry getCurrent() {
        ComponentMetaData component = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        return getForModule(component.getModuleMetaData().getJ2EEName());
    }

    /**
     * Retrieves the encoder registry for a specific module by name.
     * Use this when you need to access a specific module's registry from outside its context.
     *
     * @param moduleName the J2EE name of the module
     * @return the encoder registry for the specified module
     */
    public EncoderRegistry getForModule(J2EEName moduleName) {
        return encoderRegistries.computeIfAbsent(moduleName, m -> new EncoderRegistry(globalRegistry));
    }

    /**
     * Returns all encoder registries across all modules.
     * Use for administrative operations or cleanup.
     *
     * @return collection of all encoder registries
     */
    public Collection<EncoderRegistry> getAll() {
        return encoderRegistries.values();
    }
}
