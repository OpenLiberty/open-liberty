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
package com.ibm.ws.container.service.annotations.internal;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.anno.service.AnnotationService_Service;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 *
 */
public class WebAnnotationsAdapter implements ContainerAdapter<WebAnnotations> {

    private static final TraceComponent tc = Tr.register(WebAnnotationsAdapter.class);

    private final AtomicServiceReference<AnnotationService_Service> annoServiceSRRef =
                    new AtomicServiceReference<AnnotationService_Service>("annoService");

    //

    public void activate(ComponentContext context) {
        annoServiceSRRef.activate(context);
    }

    public void deactivate(ComponentContext context) {
        annoServiceSRRef.deactivate(context);
    }

    protected void setAnnoService(ServiceReference<AnnotationService_Service> ref) {
        annoServiceSRRef.setReference(ref);
    }

    protected void unsetAnnoService(ServiceReference<AnnotationService_Service> ref) {
        annoServiceSRRef.unsetReference(ref);
    }

    //

    @Override
    public WebAnnotations adapt(Container root,
                                OverlayContainer rootOverlay,
                                ArtifactContainer artifactContainer,
                                Container containerToAdapt) throws UnableToAdaptException {

        AnnotationService_Service annotationService = annoServiceSRRef.getService();
        if (annotationService == null) {
            String msg = Tr.formatMessage(tc, "annotation.service.not.available.CWWKM0463E", "Annotation service not available", containerToAdapt);
            throw new UnableToAdaptException(msg);
        }

        Object webModuleInfo = rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), WebModuleInfo.class);
        if (webModuleInfo == null) {
            String msg = Tr.formatMessage(tc, "container.is.not.a.web.module.CWWKM0464E", "Container is not a web module", containerToAdapt);
            throw new UnableToAdaptException(msg);
        }
        else {
            if ( com.ibm.ws.container.service.annocache.AnnotationsBetaHelper.getLibertyBeta() ) {
                Tr.warning(tc, "Unconverted adapt to web annotations");
                (new Throwable("Unconverted adapt to web annotations")).printStackTrace(System.out);
            }

            return new WebAnnotationsImpl(root,
                            rootOverlay,
                            artifactContainer,
                            containerToAdapt,
                            annotationService);
        }

    }
}
