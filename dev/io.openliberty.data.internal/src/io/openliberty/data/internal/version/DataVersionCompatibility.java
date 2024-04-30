/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.data.internal.version;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Interface for version-dependent capability, available as an OSGi service.
 */
public interface DataVersionCompatibility {
    /**
     * Obtains the Count annotation if present on the method. Otherwise null.
     *
     * @param method repository method. Must not be null.
     * @return Count annotation if present, otherwise null.
     */
    Annotation getCountAnnotation(Method method);

    /**
     * Obtains the Exists annotation if present on the method. Otherwise null.
     *
     * @param method repository method. Must not be null.
     * @return Exists annotation if present, otherwise null.
     */
    Annotation getExistsAnnotation(Method method);

    /**
     * Obtains the start of a function call, including the opening parenthesis
     * and possibly other syntax following the opening parenthesis as needed,
     * but not the value or closing parenthesis.
     *
     * @param functionAnno function annotation. Must not be null.
     * @return the start of the function call. Must not be null.
     */
    String getFunctionCall(Annotation functionAnno);

    /**
     * Obtains the value of the Select annotation if present on the method.
     * Otherwise null.
     *
     * @param method repository method. Must not be null.
     * @return values of the Select annotation indicating the columns to select,
     *         otherwise null.
     */
    String[] getSelections(Method method);

    /**
     * Return a 2-element array where the first element is the entity attribute name
     * and the second element is the operation (=, +, -, *, or /).
     *
     * @param anno Assign, Add, SubtractFrom, Multiply, or Divide annotation. Must not be null.
     * @return operation and entity attribute name.
     */
    String[] getUpdateAttributeAndOperation(Annotation anno);

    /**
     * Return true if the annotation is the Rounded annotation and has
     * direction of NEAREST, otherwise false.
     *
     * @param rounded function annotation. Must not be null.
     * @return true if the annotation is Rounded has direction of NEAREST, otherwise false.
     */
    boolean roundToNearest(Annotation anno);
}