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
package com.ibm.ws.container.service.annocache;

import java.util.Set;

import com.ibm.wsspi.annocache.targets.AnnotationTargets_Targets;

/**
 * Web container specific class scanning.
 * 
 * Web container scans require a capability to scan explicitly specified
 * classes in web module deployment descriptors and in non-excluded fragment
 * deployment descriptors.
 *
 * Classes in non-excluded but metadata-complete *are* scanned.
 *
 * As classes in non-metadata complete regions are processed by overall
 * scanning, only {@link AnnotationTargets_Targets#POLICY_PARTIAL} classes
 * are processed by {@link #selectAnnotatedClasses}.
 *
 * See {@link com.ibm.ws.webcontainer.osgi.container.config.WebAppConfiguratorHelper#configureSpecificClass}
 * for additional details.
 */
// Used by:
//
// com.ibm.ws.webcontainer/src/com/ibm/ws/webcontainer/osgi/container/config/WebAppConfiguratorHelper.java
// -- used to detect WebServlet, WebListener, WebFilter, and MultipartConfig annotations
//    on single specified classes
public interface SpecificAnnotations extends com.ibm.ws.container.service.annotations.SpecificAnnotations {
    /**
     * Answer the targets table which holds the specific annotation
     * data.
     *
     * @return The underlying targets table.
     */
    AnnotationTargets_Targets getTargets();

    /**
     * Select annotated classes from the results.  Answer only annotated
     * classes which are in {@link AnnotationTargets_Targets#POLICY_PARTIAL}
     * regions.
     *
     * @param The annotation class used to select classes.
     *
     * @return The names of classes which have the specified annotation
     *     and which are in {@link AnnotationTargets_Targets#POLICY_PARTIAL}
     *     regions.
     */
    Set<String> selectAnnotatedClasses(Class<?> annotationClass);
}
