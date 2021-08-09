/*******************************************************************************
 * Copyright (c) 2013, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.annocache.internal;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;

import com.ibm.wsspi.annocache.classsource.ClassSource_Factory;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.container.service.annocache.ContainerAnnotations;
import com.ibm.ws.container.service.annocache.ContainerAnnotationsAdapter;

/**
 * Container annotations adapter code.
 */
@Component(
    service = ContainerAdapter.class, immediate = true,
    configurationPolicy = ConfigurationPolicy.IGNORE,
    property = { "service.vendor=IBM", "toType=com.ibm.ws.container.service.annocache.ContainerAnnotations" })
public class ContainerAnnotationsAdapterImpl
    extends AnnotationsAdapterImpl
    implements ContainerAnnotationsAdapter {

    @Override
    public ContainerAnnotations adapt(
        Container rootContainer,
        OverlayContainer rootOverlayContainer,
        ArtifactContainer rootArtifactContainer,
        Container rootAdaptableContainer) {

        // Do not put container annotations in the non-persistent cache:
        //
        // They are a special case from: 
        // com.ibm.ws.app.manager.ear.internal.EARDeployedAppInfo
        //     hasAnnotations(Container, Collection<String>)
        // And are used to perform a simple query of whether a
        // particular EJB jar has EJB related annotations.
        //
        // The use is one time per EJB jar per application startup:

        // String adaptPath = rootArtifactContainer.getPath();
        //
        // ContainerAnnotations containerAnnotations =
        //     overlayGet(rootOverlayContainer, adaptPath, ContainerAnnotations.class);
        // 
        // if ( containerAnnotations == null ) {

        ContainerAnnotations containerAnnotations = new ContainerAnnotationsImpl(
            this,
            rootContainer, rootOverlayContainer, rootArtifactContainer, rootAdaptableContainer,
            ClassSource_Factory.UNNAMED_APP,
            ClassSource_Factory.IS_UNNAMED_MOD,
            ClassSource_Factory.UNNAMED_MOD,
            ClassSource_Factory.UNSET_CATEGORY_NAME);

        // The container annotations are ready to be used, but is incomplete:
        //
        // If the annotations are to be cached, the app, mod, and mod cat names must be set.
        // If inheritance APIs are to be used, the class loader must be set.
        // If jandex reads are to be supported, the jandex flag must be set.

        // overlayPut(rootOverlayContainer, adaptPath, ContainerAnnotations.class, containerAnnotations);
        // }

        return containerAnnotations;
    }
}
