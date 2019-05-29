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
package com.ibm.ws.container.service.annocache.internal;

import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import com.ibm.wsspi.artifact.overlay.OverlayContainer;

import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

import com.ibm.wsspi.annocache.service.AnnotationCacheService_Service;

/**
 * Common annotations adapter code.
 */
public abstract class AnnotationsAdapterImpl {
    public static final TraceComponent tc = Tr.register(AnnotationsAdapterImpl.class);
    // private static final String CLASS_NAME = AnnotationsAdapterImpl.class.getSimpleName();

    //

    @Reference(service=AnnotationCacheService_Service.class)
    protected AnnotationCacheService_Service annoCacheService;

    public AnnotationCacheService_Service getAnnoCacheService()
        throws UnableToAdaptException {

        if ( annoCacheService == null ) {
            String msg = Tr.formatMessage(tc,
                "annotation.service.not.available.CWWKM0451E",
                "Annotation service not available");
            throw new UnableToAdaptException(msg);
        }

        return annoCacheService;
    }

    //

    @SuppressWarnings("unchecked")
    public <T> T overlayGet(
        OverlayContainer overlayContainer,
        String targetPath, Class<T> targetClass) {

        T targetObject = (T) (overlayContainer.getFromNonPersistentCache(targetPath, targetClass));

        if ( tc.isDebugEnabled() ) {
            String message = getClass().getSimpleName() +
                ": overlayGet [ " + overlayContainer + " ]" +
                " [ " + targetPath + " ] [ " + targetClass + " ]: [ " + targetObject + " ]";
            Tr.debug(tc, message);
        }

        return targetObject;
    }

    public <T> void overlayPut(
        OverlayContainer overlayContainer,
        String targetPath, Class<T> targetClass, T targetObject) {

        if ( tc.isDebugEnabled() ) {
            String message = getClass().getSimpleName() +
                ": overlayPut [ " + overlayContainer + " ]" +
                " [ " + targetPath + " ] [ " + targetClass + " ]: [ " + targetObject + " ]";
            Tr.debug(tc, message);
        }

        overlayContainer.addToNonPersistentCache(targetPath, targetClass, targetObject);
    }
}
