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
package com.ibm.ws.container.service.annotations;

import java.util.Set;

import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;

/**
 * Annotations data from a scan of specific classes.
 */
// Used by:
//
// com.ibm.ws.webcontainer/src/com/ibm/ws/webcontainer/osgi/container/config/WebAppConfiguratorHelper.java
// -- used to detect WebServlet, WebListener, WebFilter, and MultipartConfig annotations
//    on single specified classes
//

public interface SpecificAnnotations {
    /**
     * Answer the targets table which holds the specific annotation
     * data.
     * 
     * @return The underlying targets table.
     */
    AnnotationTargets_Targets getTargets();

    /**
     * Target helper: Select the classes which are recorded as having
     * the specified annotation as a class annotation.
     * 
     * @param annotationClass The class annotation to use for the selection.
     * 
     * @return The names of classes having the annotation as a class annotation.
     */
    Set<String> selectAnnotatedClasses(Class<?> annotationClass);
}
