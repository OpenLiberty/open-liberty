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
package com.ibm.ws.container.service.annotations.internal;

import java.util.Set;

import com.ibm.ws.container.service.annotations.SpecificAnnotations;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;

/**
 * Web container scans of explicitly specified classes.
 */
public class SpecificAnnotationsImpl implements SpecificAnnotations {

    protected SpecificAnnotationsImpl(AnnotationTargets_Targets specificTargets) {
        this.specificTargets = specificTargets;
    }

    //

    private final AnnotationTargets_Targets specificTargets;

    @Override
    public AnnotationTargets_Targets getTargets() {
        return specificTargets;
    }

    //

    @Override
    public Set<String> selectAnnotatedClasses(Class<?> annotationClass) {
        String annotationClassName = annotationClass.getName();
        Set<String> selectedClassNames =
            specificTargets.getAnnotatedClasses(
                annotationClassName,
                AnnotationTargets_Targets.POLICY_PARTIAL);
        return selectedClassNames;
    }
}
