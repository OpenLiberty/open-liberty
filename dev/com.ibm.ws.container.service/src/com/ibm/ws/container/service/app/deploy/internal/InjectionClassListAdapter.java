/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.app.deploy.internal;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.container.service.app.deploy.InjectionClassList;
import com.ibm.ws.container.service.app.deploy.InjectionClassListProvider;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;

/**
 *
 */
public class InjectionClassListAdapter implements ContainerAdapter<InjectionClassList> {

    private final ConcurrentServiceReferenceSet<InjectionClassListProvider> injectionClassListProviders = new ConcurrentServiceReferenceSet<InjectionClassListProvider>("injectionClassListProviders");

    @Override
    public InjectionClassList adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {

        final List<String> listOfClassNames = new ArrayList<String>();

        for (InjectionClassListProvider provider : injectionClassListProviders.services()) {
            listOfClassNames.addAll(provider.getInjectionClasses(containerToAdapt));
        }

        return new InjectionClassList() {
            @Override
            public List<String> getClassNames() {
                return listOfClassNames;
            }
        };
    }

    /**
     * DS method to activate this component.
     */
    public void activate(ComponentContext context) {
        injectionClassListProviders.activate(context);
    }

    /**
     * DS method to deactivate this component.
     */
    public void deactivate(ComponentContext context) {
        injectionClassListProviders.deactivate(context);
    }

    protected void setInjectionClassListProviders(ServiceReference<InjectionClassListProvider> ref) {
        injectionClassListProviders.addReference(ref);
    }

    protected void unsetInjectionClassListProviders(ServiceReference<InjectionClassListProvider> ref) {
        injectionClassListProviders.removeReference(ref);
    }
}
