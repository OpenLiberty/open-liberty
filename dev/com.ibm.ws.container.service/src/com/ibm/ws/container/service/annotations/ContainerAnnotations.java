/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.annotations;

import java.util.List;
import java.util.Set;

import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;

/**
 * Interface to check if a container has any classes using specific annotations
 */
public interface ContainerAnnotations {

    /**
     * Returns true if the container has any classes directly annotated with the specified annotations.
     * Uses a scan policy of {@link ClassSource_Aggregate.ScanPolicy.SEED}.
     * Inherited annotations are <b>NOT</b> included in the scan results.
     *
     * @param annotationTypeNames the annotation type names
     * @return true if the container has any classes with the specified annotations
     */
    public boolean hasSpecifiedAnnotations(List<String> annotationTypeNames, boolean useJandex);

    /**
     * Returns the names of any classes in the container which have any of the specified annotations.
     * Uses a scan policy of {@link ClassSource_Aggregate.ScanPolicy.SEED}.
     * Inherited annotations are included in the scan results.
     *
     * @param annotationTypeNames the annotation type names
     * @return the names of any classes which have any of the specified annotations (declared or inherited)
     */
    public Set<String> getClassesWithSpecifiedInheritedAnnotations(List<String> annotationTypeNames, boolean useJandex);
}
