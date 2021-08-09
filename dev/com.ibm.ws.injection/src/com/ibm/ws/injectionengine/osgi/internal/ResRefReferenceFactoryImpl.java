/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
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

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.javaee.dd.common.ResourceRef;
import com.ibm.ws.resource.ResourceRefConfig;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionScope;
import com.ibm.wsspi.injectionengine.factory.ResRefReferenceFactory;

public class ResRefReferenceFactoryImpl implements ResRefReferenceFactory {
    private final ResourceBindingListenerManager resourceBindingListenerManager;

    public ResRefReferenceFactoryImpl(ResourceBindingListenerManager resourceBindingListenerManager) {
        this.resourceBindingListenerManager = resourceBindingListenerManager;
    }

    @Trivial
    @Override
    public Reference createResRefJndiLookup(ComponentNameSpaceConfiguration compNSConfig, InjectionScope scope, ResourceRefInfo resRef) throws InjectionException {
        return createResRefLookupReference(resRef.getName(), (ResourceRefConfig) resRef, false);
    }

    public Reference createResRefLookupReference(String refName, ResourceRefConfig resRef, boolean defaultBinding) {
        String bindingName = resRef.getJNDIName();
        String bindingListenerName = null;
        String type = resRef.getType();

        if (type != null) {
            ResourceBindingImpl binding = resourceBindingListenerManager.binding(refName, bindingName, type, null);
            if (binding != null) {
                bindingName = binding.getBindingName();
                bindingListenerName = binding.getBindingListenerName();
                defaultBinding = false;

                // The binding was set programmatically, so auth-type=Application
                // and login information (including authentication-alias, which is
                // represented as a login property) are no longer meaningful.
                resRef.setResAuthType(ResourceRef.AUTH_CONTAINER);
                resRef.setLoginConfigurationName(null);
                resRef.clearLoginProperties();
            }
        }

        return new IndirectReference(refName, bindingName, resRef.getType(), resRef, bindingListenerName, defaultBinding);
    }
}
