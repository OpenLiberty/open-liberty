/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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
package io.openliberty.data.internal.persistence;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Information about a repository method parameter.
 */
@Trivial
class ParamInfo {
    /**
     * Entity attribute name from the By annotation. Otherwise null.
     */
    String byAttribute;

    /**
     * Comparison annotation (such as LessThan) that is specified on the parameter. Otherwise null.
     */
    Annotation comparisonAnno;

    /**
     * Function annotations (such as AbsoluteValue) that are specified on the parameter. Otherwise null.
     */
    List<Annotation> functionAnnos;

    /**
     * Indicates if the parameter is an IdClass.
     */
    boolean isIdClass;

    /**
     * Indicates if the Or annotation is present.
     */
    boolean or;

    /**
     * Annotation that is specified on the parameter to indicate a type of update (such as Add). Otherwise null.
     */
    Annotation updateAnno;

    void addFunctionAnnotation(Annotation anno) {
        if (functionAnnos == null)
            functionAnnos = new ArrayList<>();
        functionAnnos.add(anno);
    }
}
