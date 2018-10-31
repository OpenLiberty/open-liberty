/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.annotations.internal;

import com.ibm.ws.container.service.annotations.CDIContainerAnnotations;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.anno.classsource.ClassSource_Factory;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 * CDI Container annotations adapter code.
 */
public class CDIContainerAnnotationsAdapterImpl
    extends AnnotationsAdapterImpl
    implements ContainerAdapter<CDIContainerAnnotations> {

    @Override
    public CDIContainerAnnotations adapt(
        Container rootContainer,
        OverlayContainer rootOverlayContainer,
        ArtifactContainer rootArtifactContainer,
        Container rootAdaptableContainer) {

        // String adaptPath = rootArtifactContainer.getPath();
        // CDIContainerAnnotations cdiContainerAnnotations =
        //     overlayGet(rootOverlayContainer, adaptPath, CDIContainerAnnotations.class);
        // overlayPut(rootOverlayContainer, adaptPath, CDIContainerAnnotations.class, cdiContainerAnnotations);

        // Do *not* put the annotations in the non-persistent cache:
        //
        // Multiple CDI annotations will be created for the same container, with different
        // class loading contexts for each.  Because the class loading contexts are different,
        // the annotations data cannot be collapsed into a single shared object.

        CDIContainerAnnotations cdiContainerAnnotations =
            new CDIContainerAnnotationsImpl(
                this,
                rootContainer, rootOverlayContainer, rootArtifactContainer, rootAdaptableContainer,
                ClassSource_Factory.UNNAMED_APP,
                ClassSource_Factory.UNNAMED_MOD);

        // The container annotations are ready to be used, but is incomplete:
        //
        // If the annotations are to be cached, the app and mod names must be set.
        // If inheritance APIs are to be used, the class loader must be set.
        // If jandex reads are to be supported, the jandex flag must be set.

        return cdiContainerAnnotations;
    }
}
