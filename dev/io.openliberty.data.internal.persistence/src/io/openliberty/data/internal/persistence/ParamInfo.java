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

import io.openliberty.data.repository.function.Not;

/**
 * Information about a repository method parameter.
 */
@Trivial
class ParamInfo {
    /**
     * Entity attribute name. Empty string to determine from the parameter name.
     */
    String attributeName;

    /**
     * Comparison annotation (such as LessThan) that is specified on the parameter. Otherwise null.
     */
    Annotation comparisonAnno;

    /**
     * Function annotations (such as AbsoluteValue) that are specified on the parameter. Otherwise null.
     * The Not function is omitted from this list and tracked separately as the negate field.
     */
    List<Annotation> functionAnnos;

    /**
     * Indicates if the Not annotation is present.
     */
    boolean negate;

    /**
     * Indicates if the Or annotation is present.
     */
    boolean or;

    /**
     * Annotation that is specified on the parameter to indicate a type of update (such as Add). Otherwise null.
     */
    Annotation updateAnno;

    void addFunctionAnnotation(Annotation anno) {
        if (anno instanceof Not) {
            negate = true;
        } else {
            if (functionAnnos == null)
                functionAnnos = new ArrayList<>();
            functionAnnos.add(anno);
        }
    }

    boolean hasDataAnnotation() {
        return or || negate || attributeName != null || comparisonAnno != null || functionAnnos != null || updateAnno != null;
    }
}
