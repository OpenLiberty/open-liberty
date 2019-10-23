/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.openapi31;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 *
 */
@Component(service = ContainerAdapter.class, property = { "service.vendor=IBM", "toType=com.ibm.ws.openapi31.OpenAPIWebProvider" })
public class WebModuleAdapter implements ContainerAdapter<OpenAPIWebProvider> {

    public ComponentContext ccontext = null;

    @Activate
    protected void activate(ComponentContext context) {
        ccontext = context;
    }

    /** {@inheritDoc} */
    @Override
    public OpenAPIWebProvider adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {
        final WebModuleInfo moduleInfo = (WebModuleInfo) rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), WebModuleInfo.class);
        return new OpenAPIWebProvider(ccontext, containerToAdapt, moduleInfo.getClassLoader());
    }

}
