/*******************************************************************************
 * Copyright (c) 2024,2026 IBM Corporation and others.
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
import java.util.Map;
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
     * Append a constraint such as o.myAttribute < ?1 to the JPQL query.
     *
     * @param q                 JPQL query to which to append.
     * @param o_                entity identifier variable.
     * @param attrName          entity attribute name.
     * @param constraint        type of constraint to apply to the entity attribute.
     * @param prevNumJPQLParams count of JQPL query parameters required for repository
     *                              method parameters up to, but not including, the
     *                              repository method parameter for the constraint
     *                              being appended.
     * @param isCollection      whether the entity attribute is a collection.
     * @param annos             method parameter annotations.
     * @return the updated JPQL query.
     */
    StringBuilder appendConstraint(StringBuilder q,
                                   String o_,
                                   String attrName,
                                   AttributeConstraint constraint,
                                   int prevNumJPQLParams,
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
     * Appends JPQL to the partially built query to represent a Constraint.
     *
     * @param q              partially built query to which to append JPQL
     *                           representing the Constraint.
     * @param entityVar_     entity identifier variable name and . character.
     * @param constraint     the Constraint for which to generate JPQL.
     * @param jpqlParamCount number of named or positional parameters identified
     *                           up to this point for the JPQL.
     * @param jpqlParamNames names of named parameters in the partially built
     *                           query. Empty if the query uses positional
     *                           parameeters or has none. If using named parameters,
     *                           this method should add any that are generated.
     * @param jpqlParams     list for this method to populate with the name of
     *                           named parameters or index of positional parameters,
     *                           mapped to value, for each value obtained from the
     *                           processed Restriction(s).
     * @return the new count of named or positional parameters, including any that
     *         were generated for the Constraint.
     */
    int generateConstraint(StringBuilder q,
                           String entityVar_,
                           Object constraint,
                           int jpqlParamCount,
                           Set<String> jpqlParamNames,
                           Map<Object, Object> jpqlParams);

    /**
     * Appends JPQL to the partially built query to implement a Restriction
     * parameter of a repository method.
     *
     * @param q              partially built query ending with the WHERE clause.
     * @param entityVar_     entity identifier variable name and . character.
     * @param restriction    value of Restriction parameter. Otherwise null.
     * @param jpqlParamCount number of named or positional parameters in the
     *                           partially built query.
     * @param jpqlParamNames names of named parameters in the partially bulit
     *                           query. Empty if the query uses positional
     *                           parameeters or has none. If using named parameters,
     *                           this method should add any that are generated for
     *                           the restriction part of the query.
     * @param jpqlParams     list for this method to populate with the name of
     *                           named parameters or index of positional parameters,
     *                           mapped to value, for each value obtained from the
     *                           processed Restriction(s).
     * @return the new count of named or positional parameters, including any that
     *         were generated for the Restriction(s).
     */
    int generateRestrictions(StringBuilder q,
                             String entityVar_,
                             Object restriction,
                             int jpqlParamCount,
                             Set<String> jpqlParamNames,
                             Map<Object, Object> jpqlParams);

    /**
     * Obtains the Count annotation if present on the method. Otherwise null.
     *
     * @param method repository method. Must not be null.
     * @return Count annotation if present, otherwise null.
     */
    Annotation getCountAnnotation(Method method);

    /**
     * Identify Constraint-typed repository method parameters for which
     * processing is deferred. Constraints that operate on non-literal
     * expressions are always deferred until the expression instance is
     * available.
     *
     * @param alwaysDefer  indicates that processing of every Constraint-typed
     *                         method parameter is always deferred.
     * @param maxIndex     method parameters positioned up to this index are
     *                         inspected.
     * @param methodParams repository method parameters.
     * @return map of method parameter index (0-based) to Constraint instance
     *         at that position. The empty map indicates none.
     */
    Map<Integer, Object> getDeferredConstraints(boolean alwaysDefer,
                                                int maxIndex,
                                                Object[] methodParams);

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
     * @param p                 repository method parameter index (0-based).
     * @param paramType         class of the repository method parameter at index p.
     *                              When generating the query upfront, this is from
     *                              the repository method signature. When generating
     *                              the query at invocation time and a Constraint
     *                              subtype is supplied, this is the class of the
     *                              supplied instance.
     * @param paramAnnos        annotations on the repository method parameter at
     *                              index p.
     * @param attrNames         the implementer can update this at position p to
     *                              supply the entity attribute name from the value
     *                              of an assignment annotation.
     * @param constraints       the implementer can update this at position p to
     *                              supply the constraint type indicated by the
     *                              Is annotation or by a Constraint-typed method
     *                              parameter.
     * @param updateOps         the implementer can update this at position p to
     *                              supply the update operation indicated by an
     *                              assignment annotation.
     * @param prevNumJPQLParams count of JQPL query parameters required for
     *                              repository method parameters up to, but not
     *                              including, the current repository method
     *                              parameter being inspected.
     * @return count of JPQL query parameters required for repository method
     *         parameters up to and including the current one. Otherwise returns
     *         an error code: PARAM_ANNO_CONFLICTS_WITH_CONSTRAINT or
     *         PARAM_ANNOS_CONFLICT.
     */
    int inspectMethodParam(int p,
                           Class<?> paramType,
                           Annotation[] paramAnnos,
                           String[] attrNames,
                           AttributeConstraint[] constraints,
                           char[] updateOps,
                           int prevNumJPQLParams);

    /**
     * Determines if the special parameter value is a Restriction.
     *
     * @param param possible special parameter value.
     * @return true if the value is a Restriction. False otherwise.
     */
    boolean isRestriction(Object param);

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
     * Returns the name of the Liberty feature that provides Jakarta Persistence.
     * For example, persistence-3.2.
     *
     * @return the name of the Liberty feature that provides Jakarta Persistence.
     */
    String persistenceFeatureName();

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