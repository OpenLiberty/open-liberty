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
package com.ibm.ws.jaxrs20.component;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.jaxrs20.metadata.JaxRsModuleInfo;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

@Component(name = "com.ibm.ws.jaxrs20.component.JaxRsModuleInfoAdapter", service = ContainerAdapter.class,
           property = { "service.vendor=IBM",
                       "toType=com.ibm.ws.jaxrs20.metadata.JaxRsModuleInfo" })
public class JaxRsModuleInfoAdapter implements ContainerAdapter<JaxRsModuleInfo> {

    /** {@inheritDoc} */
    @Override
    public JaxRsModuleInfo adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {
        NonPersistentCache overlayCache = containerToAdapt.adapt(NonPersistentCache.class);

        JaxRsModuleInfo jaxRsModuleInfo = (JaxRsModuleInfo) overlayCache.getFromCache(JaxRsModuleInfo.class);

        return jaxRsModuleInfo;
    }
}
