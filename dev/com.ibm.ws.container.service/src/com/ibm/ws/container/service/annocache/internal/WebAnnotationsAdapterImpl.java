/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.annocache.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;

import com.ibm.ws.container.service.app.deploy.WebModuleInfo;

import com.ibm.ws.container.service.annocache.WebAnnotations;
import com.ibm.ws.container.service.annocache.WebAnnotationsAdapter;

/*
 * Adapter for web module annotations data.
 */
@Component(
    service = ContainerAdapter.class, immediate = true,
    configurationPolicy = ConfigurationPolicy.IGNORE,
    property = { "service.vendor=IBM", "toType=com.ibm.ws.container.service.annocache.WebAnnotations" })
public class WebAnnotationsAdapterImpl
    extends AnnotationsAdapterImpl
    implements WebAnnotationsAdapter {

    @Override
    public WebAnnotations adapt(
        Container rootContainer,
        OverlayContainer rootOverlayContainer,
        ArtifactContainer rootArtifactContainer,
        Container rootAdaptableContainer) throws UnableToAdaptException {

        String adaptPath = rootArtifactContainer.getPath();

        WebModuleInfo webModuleInfo = overlayGet(rootOverlayContainer, adaptPath, WebModuleInfo.class);
        if ( webModuleInfo == null ) {
            String msg = Tr.formatMessage(tc, "container.is.not.a.module.CWWKM0453E",
                "Container is not a module", rootAdaptableContainer);
            throw new UnableToAdaptException(msg);
        }

        WebAnnotations webAnnotations =
            overlayGet(rootOverlayContainer, adaptPath, WebAnnotations.class);

        if ( webAnnotations == null ) {
            webAnnotations = new WebAnnotationsImpl(
                this,
                rootContainer, rootOverlayContainer,
                rootArtifactContainer, rootAdaptableContainer,
                webModuleInfo);

            overlayPut(rootOverlayContainer, adaptPath, WebAnnotations.class, webAnnotations);
        }

        return webAnnotations;
    }
}
