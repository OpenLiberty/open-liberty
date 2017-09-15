/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.osgi.internal;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import com.ibm.wsspi.injectionengine.InjectionScope;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;
import com.ibm.wsspi.kernel.service.utils.ServiceAndServiceReferencePair;
import com.ibm.wsspi.resource.ResourceBindingListener;

public class ResourceBindingListenerManager {
    private final ConcurrentServiceReferenceSet<ResourceBindingListener> listeners;

    ResourceBindingListenerManager(ConcurrentServiceReferenceSet<ResourceBindingListener> listeners) {
        this.listeners = listeners;
    }

    /**
     * Send notifications that a binding is occurring, and allow binding
     * listeners to override the specified binding. If a binding listener
     * overrides the specified binding, then a non-null ResourceBindingImpl
     * will be returned, {@link ResourceBindingImpl#getBindingName} will
     * return non-null (regardless of whether {@code origBindingName} was
     * non-null), and {@link ResourceBindingImpl#getBindingListenerName} can
     * be called.
     * 
     * @return non-null if a binding listener has overridden and binding
     */
    public ResourceBindingImpl binding(String refName, String origBindingName, String type, Map<String, Object> properties) {
        ResourceBindingImpl binding = null;
        for (Iterator<ServiceAndServiceReferencePair<ResourceBindingListener>> it = listeners.getServicesWithReferences(); it.hasNext();) {
            if (binding == null) {
                if (properties == null) {
                    properties = Collections.emptyMap();
                }
                binding = new ResourceBindingImpl(InjectionScope.denormalize(refName), origBindingName, type, properties);
            }
            binding.notify(it.next());
        }

        return binding == null || binding.bindingListener == null ? null : binding;
    }
}
