/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Set;

import jakarta.data.repository.Find;

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
     * Indicates whether the enabled version of Jakarta Data is at the requested
     * level or higher.
     *
     * @param major major version of Jakarta Data specification. Must be >= 1.
     * @param minor minor version of Jakarta Data specification. Must be >= 0.
     * @return true if at the requested level of Jakarta Data or higher,
     *         otherwise false.
     */
    boolean atLeast(int major, int minor);

    /**
     * Obtains the Count annotation if present on the method. Otherwise null.
     *
     * @param method repository method. Must not be null.
     * @return Count annotation if present, otherwise null.
     */
    Annotation getCountAnnotation(Method method);

    /**
     * Obtains the entity class from the Find annotation value, if present.
     *
     * @param find Find annotation.
     * @return entity class if the Find annotation value is present. Otherwise void.class.
     */
    Class<?> getEntityClass(Find find);

    /**
     * Obtains the Exists annotation if present on the method. Otherwise null.
     *
     * @param method repository method. Must not be null.
     * @return Exists annotation if present, otherwise null.
     */
    Annotation getExistsAnnotation(Method method);

    /**
     * Obtains the value of the Select annotation if present on the method
     * or record component. Otherwise null.
     *
     * @param element repository method or record component. Must not be null.
     * @return values of the Select annotation indicating the columns to select,
     *         otherwise null.
     */
    String[] getSelections(AnnotatedElement element);

    /**
     * Return a 2-element array where the first element is the entity attribute name
     * and the second element is the operation (=, +, -, *, or /).
     * Null if none of the annotations indicate an update
     * of if the version used does not support parameter-based update.
     *
     * @param annos annotations on the method parameter. Must not be null.
     * @return operation and entity attribute name. Null if not an update
     *         of if the version used does not support parameter-based update.
     */
    String[] getUpdateAttributeAndOperation(Annotation[] annos);

    /**
     * True if any of the annotations represent Or.
     *
     * @param annos annotations on the method parameter. Must not be null.
     * @return True if any of the annotations represent Or. Otherwise false.
     */
    boolean hasOrAnnotation(Annotation[] annos);

    /**
     * Returns the names of special parameter types that are valid for repository
     * find operations.
     *
     * @return names of valid special parameter types.
     */
    String specialParamsForFind();

    /**
     * Returns the names of special parameter types that are valid for repository
     * find-and-delete operations.
     *
     * @return names of valid special parameter types.
     */
    String specialParamsForFindAndDelete();

    /**
     * Returns the Jakarta Data defined parameter types with special meaning
     * that can be used on repository methods after the query parameters.
     *
     * @return the Jakarta Data defined special parameter types.
     */
    Set<Class<?>> specialParamTypes();
}