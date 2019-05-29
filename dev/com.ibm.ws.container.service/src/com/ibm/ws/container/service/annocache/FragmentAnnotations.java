/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
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
 * Annotations data for a single web fragment.
 */

// Used by:
//
// com.ibm.ws.webcontainer/src/com/ibm/ws/webcontainer/osgi/container/config/WebAppConfiguratorHelper.java
// -- Used to query annotations for a single fragment.
// com.ibm.ws.webcontainer.security/src/com/ibm/ws/webcontainer/security/metadata/SecurityServletConfiguratorHelper.java
// -- Used to query annotations for a single fragment.
// com.ibm.ws.webcontainer.security/test/com/ibm/ws/webcontainer/security/metadata/SecurityServletConfiguratorHelperTest.java
// com.ibm.ws.webcontainer.servlet.3.1.factories/test/com/ibm/ws/webcontainer/webapp/config/ServletConfigMock.java
//
// Fragment annotations are helpers which wrap web annotations.  Fragment annotations
// introduce no persistence issues.

/**
 * Helper type for performing queries on a single web fragment.  Always obtained relative
 * to web module annotations.
 */
public interface FragmentAnnotations extends com.ibm.ws.container.service.annotations.FragmentAnnotations {
    /**
     * Answer the annotation targets which store the overall web
     * module data.  The fragment data is managed within the
     * web module data.
     *
     * @return The annotation targets of the overall web module. 
     */
    AnnotationTargets_Targets getTargets();

    /**
     * Answer the name of the fragment of this data.
     * 
     * @return The name of the fragment of this data.
     */
    String getFragmentName();

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
