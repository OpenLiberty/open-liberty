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

import io.openliberty.data.internal.AttributeConstraint;
import io.openliberty.data.internal.QueryType;
import jakarta.data.repository.Find;

/**
 * Interface for version-dependent capability, available as an OSGi service.
 */
public interface DataVersionCompatibility {
    /**
     * Size 0 array indicating no Select annotations are present.
     */
    final String[] NO_SELECTIONS = new String[0];

    /**
     * Error condition returned by inspectMethodParam indicating that an annotation
     * of the method parameter conflicts with the constraint type of the method
     * parameter.
     */
    final int PARAM_ANNO_CONFLICTS_WITH_CONSTRAINT = -1;

    /**
     * Error condition returned by inspectMethodParam indicating that two or more
     * annotations on the method parameter conflict with each other.
     */
    final int PARAM_ANNOS_CONFLICT = -2;

    /**
     * Return code for inspectMethodParam that indicates that the Constraint subtype
     * is not determined until method invocation.
     */
    final int PARAM_CONSTRAINT_DEFERRED = -3;

    /**
     * Append a constraint such as o.myAttribute < ?1 to the JPQL query.
     *
     * @param q            JPQL query to which to append.
     * @param o_           entity identifier variable.
     * @param attrName     entity attribute name.
     * @param constraint   type of constraint to apply to the entity attribute.
     * @param qp           query parameter position (1-based).
     * @param isCollection whether the entity attribute is a collection.
     * @param annos        method parameter annotations.
     * @return the updated JPQL query.
     */
    StringBuilder appendConstraint(StringBuilder q,
                                   String o_,
                                   String attrName,
                                   AttributeConstraint constraint,
                                   int qp,
                                   boolean isCollection,
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
     * Obtains the values of Select annotations if present on the method
     * or record component. The order for values is the same as the order in
     * which the annotations are listed. Otherwise a size 0 array.
     *
     * @param element repository method or record component. Must not be null.
     * @return values of the Select annotations indicating the columns to select,
     *         otherwise a size 0 array to indicate no Select annotation is present.
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
     * Inspects the type and annotations of a method parameter to a parameter-based
     * Find/Delete/Update method to determine its meaning. Based on the meaning,
     * updates one or more of (attrNames, constraints, updateOps) at position p.
     *
     * @param p           repository method parameter index (0-based).
     * @param paramType   class of the repository method parameter at index p.
     *                        When generating the query upfront, this comes from
     *                        the repository method signature. When generating the
     *                        query at invocation time and a Constraint subtype is
     *                        supplied, this is the class of the supplied instance.
     *                        The latter should be done in response to receiving a
     *                        PARAM_CONSTRAINT_DEFERRED return code during the
     *                        attempt at upfront query generation.
     * @param paramAnnos  annotations on the repository method parameter at index p.
     * @param attrNames   the implementer can update this at position p to supply
     *                        the entity attribute name from the value of an
     *                        assignment annotation.
     * @param constraints the implementer can update this at position p to supply
     *                        the constraint type indicated by the Is annotation or
     *                        a constraint type method parameter.
     * @param updateOps   the implementer can update this at position p to supply
     *                        the update operation indicated by an assignment
     *                        annotation.
     * @param qpNext      the next JQPL query parameter index to use (1-based)
     *                        for the current repository method parameter.
     * @return the next JPQL query parameter index to use (1-based) for the
     *         subsequent repository method parameter. Otherwise returns an
     *         error code: PARAM_ANNO_CONFLICTS_WITH_CONSTRAINT or
     *         PARAM_ANNOS_CONFLICT or PARAM_CONSTRAINT_DEFERRED.
     */
    int inspectMethodParam(int p,
                           Class<?> paramType,
                           Annotation[] paramAnnos,
                           String[] attrNames,
                           AttributeConstraint[] constraints,
                           char[] updateOps,
                           int qpNext);

    /**
     * Determines if the special parameter type is valid for the type of
     * repository method.
     *
     * @param paramType type of special parameter.
     * @param queryType type of repository method.
     * @return true if valid. False if not valid.
     */
    boolean isSpecialParamValid(Class<?> paramType,
                                QueryType queryType);

    /**
     * Returns the repository method annotations that represent life cycle
     * operations (such as Delete and Insert) for either a stateful or
     * stateless repository, depending on the parameter.
     *
     * @param stateful true for a stateful repository; false for stateless.
     * @return the annotation classes.
     */
    Set<Class<? extends Annotation>> lifeCycleAnnoTypes(boolean stateful);

    /**
     * Returns the repository method annotations that represent operations
     * (such as Find and Delete, but not OrderBy) for either a stateful or
     * stateless repository, depending on the parameter.
     *
     * @param stateful true for a stateful repository; false for stateless.
     * @return the annotation classes.
     */
    Set<Class<? extends Annotation>> operationAnnoTypes(boolean stateful);

    /**
     * Returns the names of annotations that are valid on the parameters of a
     * parameter-based update method.
     *
     * @return the annotation names.
     */
    String paramAnnosForUpdate();

    /**
     * List of valid return types for resource accessor methods.
     *
     * @param stateful true for a stateful repository; false for stateless.
     * @return valid return types.
     */
    Set<Class<?>> resourceAccessorTypes(boolean stateful);

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

    /**
     * Temporary method that obtains the literal value(s) from a constraint if the
     * supplied value is a constraint for a literal expression.
     *
     * @param constraintOrValue a jakarta.data.constraint.Constraint subtype or a
     *                              literal value.
     * @return array of literal values obtained from the constraint.
     *         Null if not a constraint.
     */
    Object[] toConstraintValues(Object constraintOrValue);
}