/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.classloading.ClassLoadingButler;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 *
 */

@Component(service = ContainerAdapter.class,
           configurationPolicy = ConfigurationPolicy.OPTIONAL,
           immediate = true,
           property = { "service.vendor=IBM", "toType=com.ibm.ws.classloading.ClassLoadingButler" })
public class ClassLoadingButlerAdapter implements ContainerAdapter<ClassLoadingButler> {
    private final static TraceComponent tc = Tr.register(ClassLoadingButlerAdapter.class);

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter#adapt(com.ibm.wsspi.adaptable.module.Container, com.ibm.wsspi.adaptable.module.adapters.OverlayContainer,
     * com.ibm.wsspi.artifact.ArtifactContainer, com.ibm.wsspi.adaptable.module.Container)
     */
    @Override
    public ClassLoadingButler adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {
        ClassLoadingButler butler = (ClassLoadingButler) rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), ClassLoadingButler.class);
        if (butler != null) {
            return butler;
        }

        butler = new ClassLoadingButlerImpl(containerToAdapt);

        rootOverlay.addToNonPersistentCache(artifactContainer.getPath(), ClassLoadingButler.class, butler);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "adapt adding container to map - container = " + containerToAdapt);
        }

        return butler;
    }

}
