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

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.service.AnnotationService_Service;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Common annotations adapter code.
 */
public abstract class AnnotationsAdapterImpl {
    public static final TraceComponent tc = Tr.register(AnnotationsAdapterImpl.class);
    // private static final String CLASS_NAME = AnnotationsAdapterImpl.class.getSimpleName();

    //

    /**
     * Reference to the annotations service.  The container annotations adapter
     * uses annotation services to perform annotation scans, which generates class
     * and annotation data, against which the container annotations API drives
     * queries to satisfy the API.
     */
    private final AtomicServiceReference<AnnotationService_Service> annoServiceRef =
        new AtomicServiceReference<AnnotationService_Service>("annoService");

    // Required service APIs ...

    public void activate(ComponentContext context) {
        annoServiceRef.activate(context);
    }

    public void deactivate(ComponentContext context) {
        annoServiceRef.deactivate(context);
    }

    protected void setAnnoService(ServiceReference<AnnotationService_Service> ref) {
        annoServiceRef.setReference(ref);
    }

    protected void unsetAnnoService(ServiceReference<AnnotationService_Service> ref) {
        annoServiceRef.unsetReference(ref);
    }

    //

    public AnnotationService_Service getAnnotationService(Container targetContainer)
        throws UnableToAdaptException {
        AnnotationService_Service annotationService = annoServiceRef.getService();
        if ( annotationService == null ) {
            String msg = Tr.formatMessage(tc, "annotation.service.not.available.CWWKM0451E",
                "Annotation service not available", targetContainer);
            throw new UnableToAdaptException(msg);
        }
        return annotationService;
    }

    //

    @SuppressWarnings("unchecked")
    public <T> T overlayGet(
        OverlayContainer overlayContainer,
        String targetPath, Class<T> targetClass) {

        T targetObject = (T) (overlayContainer.getFromNonPersistentCache(targetPath, targetClass));

        // TODO: Temporary for annotation caching testing.
        String message = getClass().getSimpleName() +
            ": overlayGet [ " + overlayContainer + " ]" +
            " [ " + targetPath + " ] [ " + targetClass + " ]: [ " + targetObject + " ]";
        Tr.info(tc, message);

        return targetObject;
    }

    public <T> void overlayPut(
        OverlayContainer overlayContainer,
        String targetPath, Class<T> targetClass, T targetObject) {

        // TODO: Temporary for annotation caching testing.
        String message = getClass().getSimpleName() +
            ": overlayPut [ " + overlayContainer + " ]" +
            " [ " + targetPath + " ] [ " + targetClass + " ]: [ " + targetObject + " ]";
        Tr.info(tc, message);

        overlayContainer.addToNonPersistentCache(targetPath, targetClass, targetObject);
    }
}
