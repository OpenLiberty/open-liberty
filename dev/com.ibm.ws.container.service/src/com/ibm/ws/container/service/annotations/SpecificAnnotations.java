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

import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 *
 */
public interface SpecificAnnotations {

    /**
     * <p>Target helper: Select the classes which are recorded as having
     * the specified annotation as a class annotation.</p>
     * 
     * @param annotationClass The class annotation to use for the selection.
     * 
     * @return The names of classes having the annotation as a class annotation.
     * 
     * @throws UnableToAdaptException Thrown by an error processing fragment paths.
     */
    Set<String> selectAnnotatedClasses(Class<?> annotationClass) throws UnableToAdaptException;
}
