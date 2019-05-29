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
package com.ibm.ws.container.service.annocache.internal;

import java.util.Set;

import com.ibm.wsspi.annocache.targets.AnnotationTargets_Targets;

import com.ibm.ws.container.service.annocache.FragmentAnnotations;

/**
 * Added API for accessing fragment annotations.
 */
public class FragmentAnnotationsImpl implements FragmentAnnotations {

    public FragmentAnnotationsImpl(AnnotationTargets_Targets targets, String fragmentName) {
        this.targets = targets;
        this.fragmentName = fragmentName;
    }

    //

    private final AnnotationTargets_Targets targets;

    @Override
    public AnnotationTargets_Targets getTargets() {
        return targets;
    }

    private final String fragmentName;

    @Override
    public String getFragmentName() {
        return fragmentName;
    }

    //

    @Override
    public Set<String> selectAnnotatedClasses(Class<?> annotationClass)  {
        // d95160: Not sure if SEED is the correct value to use here ...
        return targets.getAnnotatedClasses( getFragmentName(), annotationClass.getName() );
    }
}
