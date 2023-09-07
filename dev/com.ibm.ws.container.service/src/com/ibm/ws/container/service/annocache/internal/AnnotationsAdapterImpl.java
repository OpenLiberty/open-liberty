/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.annocache.internal;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.version.JavaEEVersion;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.annocache.service.AnnotationCacheService_Service;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 * Common annotations adapter code.
 */
public abstract class AnnotationsAdapterImpl {
    public static final TraceComponent tc = Tr.register(AnnotationsAdapterImpl.class);
    // private static final String CLASS_NAME = AnnotationsAdapterImpl.class.getSimpleName();

    /**
     * Jakarta EE version if Jakarta EE 9 or higher. If 0, assume a lesser EE spec version.
     */
    protected volatile int eeVersion;

    /**
     * Tracks the most recently bound EE version service reference. Only use this within the set/unsetEEVersion methods.
     */
    private ServiceReference<JavaEEVersion> eeVersionRef;

    /**
     * Declarative Services method for setting the Jakarta/Java EE version
     *
     * @param ref reference to the service
     */
    @Reference(service = JavaEEVersion.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setEEVersion(ServiceReference<JavaEEVersion> ref) {
        String version = (String) ref.getProperty("version");
        if (version == null) {
            eeVersion = 0;
        } else {
            int dot = version.indexOf('.');
            String major = dot > 0 ? version.substring(0, dot) : version;
            eeVersion = Integer.parseInt(major);
        }
        eeVersionRef = ref;
    }

    /**
     * Declarative Services method for unsetting the Jakarta/Java EE version
     *
     * @param ref reference to the service
     */
    protected void unsetEEVersion(ServiceReference<JavaEEVersion> ref) {
        if (eeVersionRef == ref) {
            eeVersionRef = null;
            eeVersion = 0;
        }
    }

    private ServiceReference<JavaEEVersion> versionRef;
    protected volatile Version platformVersion = JavaEEVersion.DEFAULT_VERSION;

    @Reference(service = JavaEEVersion.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected synchronized void setVersion(ServiceReference<JavaEEVersion> reference) {
        versionRef = reference;
        platformVersion = Version.parseVersion((String) reference.getProperty("version"));
    }

    protected synchronized void unsetVersion(ServiceReference<JavaEEVersion> reference) {
        if (reference == this.versionRef) {
            versionRef = null;
            platformVersion = JavaEEVersion.DEFAULT_VERSION;
        }
    }

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
