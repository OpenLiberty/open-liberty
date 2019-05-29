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

import com.ibm.ws.container.service.app.deploy.ModuleInfo;

import com.ibm.ws.container.service.annocache.ModuleAnnotationsAdapter;
import com.ibm.ws.container.service.annocache.ModuleAnnotations;

/**
 * Module annotations adapter code.
 */
@Component(
    service = ContainerAdapter.class, immediate = true,
    configurationPolicy = ConfigurationPolicy.IGNORE,
    property = { "service.vendor=IBM", "toType=com.ibm.ws.container.service.annocache.ModuleAnnotations" })
public class ModuleAnnotationsAdapterImpl
    extends AnnotationsAdapterImpl
    implements ModuleAnnotationsAdapter {

    @Override
    public ModuleAnnotations adapt(
        Container rootContainer,
        OverlayContainer rootOverlayContainer,
        ArtifactContainer rootArtifactContainer,
        Container rootAdaptableContainer) throws UnableToAdaptException {

        String adaptPath = rootArtifactContainer.getPath();

        ModuleInfo moduleInfo = overlayGet(rootOverlayContainer, adaptPath, ModuleInfo.class);
        if ( moduleInfo == null ) {
            String msg = Tr.formatMessage(tc, "container.is.not.a.module.CWWKM0453E",
                "Container is not a module", rootAdaptableContainer);
            throw new UnableToAdaptException(msg);
        }

        ModuleAnnotations moduleAnnotations = overlayGet(rootOverlayContainer, adaptPath, ModuleAnnotations.class);

        if ( moduleAnnotations == null ) {
            moduleAnnotations = new ModuleAnnotationsImpl(
                this,
                rootContainer, rootOverlayContainer,
                rootArtifactContainer, rootAdaptableContainer,
                moduleInfo); // throws UnableToAdaptException

            overlayPut(rootOverlayContainer, adaptPath, ModuleAnnotations.class, moduleAnnotations);
        }

        return moduleAnnotations;
    }
}
