/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.osgi.internal;

import javax.naming.Reference;

import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.factory.IndirectJndiLookupReferenceFactory;

public class IndirectJndiLookupReferenceFactoryImpl implements IndirectJndiLookupReferenceFactory {
    private final ResourceBindingListenerManager resourceBindingListenerManager;

    public IndirectJndiLookupReferenceFactoryImpl(ResourceBindingListenerManager resourceBindingListenerManager) {
        this.resourceBindingListenerManager = resourceBindingListenerManager;
    }

    @Override
    public String toString() {
        return super.toString() + '[' + resourceBindingListenerManager + ']';
    }

    @Override
    public Reference createIndirectJndiLookup(String refName, String bindingName, String type) throws InjectionException {
        if (resourceBindingListenerManager != null && type != null) {
            ResourceBindingImpl binding = resourceBindingListenerManager.binding(refName, bindingName, type, null);
            if (binding != null) {
                return new IndirectReference(refName, binding.getBindingName(), type, null, binding.getBindingListenerName(), false);
            }
        }

        return new IndirectReference(refName, bindingName, type, null, null, false);
    }

    @Override
    public Reference createIndirectJndiLookupInConsumerContext(String refName, String bindingName, String type) throws InjectionException {
        if (resourceBindingListenerManager != null && type != null) {
            ResourceBindingImpl binding = resourceBindingListenerManager.binding(refName, bindingName, type, null);
            if (binding != null) {
                return new IndirectReference(refName, binding.getBindingName(), type, null, binding.getBindingListenerName(), false);
            }
        }

        // Note that defaultBinding = true to allow auto-link where supported
        return new IndirectReference(refName, bindingName, type, null, null, true);
    }
}
