/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.moduleScope;

import java.lang.annotation.Annotation;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

/**
 * Implements {@link ModuleScoped}
 * <p>
 * Holds a separate {@link InstanceStore} for each module and uses them to create and destroys beans.
 * <p>
 * Always uses the {@link InstanceStore} for the current module according to {@link ComponentMetaDataAccessorImpl}.
 */
public class ModuleContext implements AlterableContext {

    private record ModuleData(ComponentMetaData metadata, InstanceStore store) {};

    private final ConcurrentMap<J2EEName, ModuleData> moduleData;

    public ModuleContext() {
        moduleData = new ConcurrentHashMap<>();
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        return getInstanceStore().get(contextual, null);
    }

    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        return getInstanceStore().get(contextual, creationalContext);
    }

    @Override
    public void destroy(Contextual<?> contextual) {
        getInstanceStore().destroy(contextual);
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ModuleScoped.class;
    }

    @Override
    public boolean isActive() {
        ComponentMetaData component = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        return component != null;
    }

    public void shutdown() {
        var componentMetaDataAccessor = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
        for (ModuleData module : moduleData.values()) {
            componentMetaDataAccessor.beginContext(module.metadata());
            try {
                module.store().destroyAll();
            } finally {
                componentMetaDataAccessor.endContext();
            }
        }
    }

    private InstanceStore getInstanceStore() {
        ComponentMetaData component = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (component == null) {
            throw new ContextNotActiveException();
        }
        ModuleMetaData module = component.getModuleMetaData();
        return moduleData.computeIfAbsent(module.getJ2EEName(), m -> new ModuleData(component, new InstanceStore())).store();
    }

}