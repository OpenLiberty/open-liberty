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
     * Append a condition such as o.myAttribute < ?1 to the JPQL query.
     *
     * @param q            JPQL query to which to append.
     * @param qp           query parameter position (1-based).
     * @param method       the repository method.
     * @param p            method parameter position (0-based).
     * @param o_           entity identifier variable.
     * @param attrName     entity attribute name.
     * @param isCollection whether the entity attribute is a collection.
     * @param annos        method parameter annotations.
     * @return the updated JPQL query.
     */
    StringBuilder appendCondition(StringBuilder q, int qp,
                                  Method method, int p,
                                  String o_, String attrName,
                                  boolean isCollection, Annotation[] annos);

    /**
     * Append conditions for an IdClass attribute such as
     * (o.idClassAttr1 = ?1 AND o.idClassAttr2 = ?2)
     * to the JPQL query.
     *
     * @param q                JPQL query to which to append.
     * @param qp               query parameter position (1-based).
     * @param method           the repository method.
     * @param p                method parameter position (0-based).
     * @param o_               entity identifier variable.
     * @param idClassAttrNames entity attribute names for the IdClass.
     * @param annos            method parameter annotations.
     * @return the updated JPQL query.
     */
    StringBuilder appendConditionsForIdClass(StringBuilder q, int qp,
                                             Method method, int p,
                                             String o_, String[] idClassAttrNames,
                                             Annotation[] annos);

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
     * Null if none of the annotations indicate an update.
     *
     * @param annos annotations on the method parameter. Must not be null.
     * @return operation and entity attribute name. Null if not an update.
     */
    String[] getUpdateAttributeAndOperation(Annotation[] annos);

    /**
     * True if any of the annotations represent Or.
     *
     * @param annos annotations on the method parameter. Must not be null.
     * @return True if any of the annotations represent Or. Otherwise false.
     */
    boolean hasOrAnnotation(Annotation[] annos);
}