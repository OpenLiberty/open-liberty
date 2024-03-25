/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.concurrent.internal.cdi.metadata;

import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.metadata.extended.DeferredMetaDataFactory;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

/**
 * Factory for dummy ComponentMetaData for a module or application.
 * This metadata is used as the application context for a ManagedThreadFactory.
 */
@Component(service = DeferredMetaDataFactory.class,
           property = { "deferredMetaData=MTF" })
public class MTFDeferredMetaDataFactory implements DeferredMetaDataFactory {

    private final ConcurrentHashMap<String, MTFComponentMetaData> metadatas = new ConcurrentHashMap<>();

    public ComponentMetaData createComponentMetadata(MetaData metadata, ClassLoader classLoader) {
        J2EEName jeeName;
        ModuleMetaData moduleMetadata;

        if (metadata instanceof ModuleMetaData) {
            moduleMetadata = (ModuleMetaData) metadata;
            jeeName = moduleMetadata.getJ2EEName();
        } else if (metadata instanceof ApplicationMetaData) {
            ApplicationMetaData adata = (ApplicationMetaData) metadata;
            jeeName = adata.getJ2EEName();
            moduleMetadata = new MTFModuleMetaData(jeeName, adata);
        } else {
            throw new IllegalArgumentException(metadata == null ? null : metadata.getClass().getName());
        }

        String identifier = "MTF#" + jeeName;

        MTFComponentMetaData componentMetadata = new MTFComponentMetaData(identifier, moduleMetadata, classLoader);
        MTFComponentMetaData existing = metadatas.putIfAbsent(identifier, componentMetadata);
        return existing == null ? componentMetadata : existing;
    }

    @Override
    public ComponentMetaData createComponentMetaData(String identifier) {
        return metadatas.get(identifier);
    }

    @Override
    public ClassLoader getClassLoader(ComponentMetaData metadata) {
        return metadata instanceof MTFComponentMetaData ? ((MTFComponentMetaData) metadata).classLoader : null;
    }

    @Override
    public String getMetaDataIdentifier(String appName, String moduleName, String componentName) {
        StringBuilder b = new StringBuilder("MTF#").append(appName);
        if (moduleName != null)
            b.append('#').append(moduleName);
        if (componentName != null)
            b.append('#').append(componentName);
        return b.toString();
    }

    @Override
    @Trivial
    public void initialize(ComponentMetaData metadata) throws IllegalStateException {
    }
}