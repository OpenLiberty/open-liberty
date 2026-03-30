/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.data.internal.v1_0;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.data.internal.AttributeConstraint;
import io.openliberty.data.internal.QueryInfo;
import io.openliberty.data.internal.QueryType;
import io.openliberty.data.internal.cdi.RepositoryProducer;

/**
 * QueryInfo implementation for Jakarta Data 1.0.
 */
public class QueryInfo_1_0 extends QueryInfo {

    /**
     * Construct partially complete query information.
     *
     * @param repositoryProducer    producer of the repository bean instance.
     * @param repositoryInterface   interface annotated with @Repository.
     * @param method                repository method.
     * @param entityParamType       type of the first parameter if a life cycle method,
     *                                  otherwise null.
     * @param methodType            type of repository method, if known in advance.
     * @param isOptional            indicates if the return type is an Optional.
     * @param returnArrayType       array element type if the repository method returns
     *                                  an array, otherwise null.
     * @param multiType             Data structure type that allows multiple
     *                                  results for this query. Null if the query
     *                                  return type limits to single results.
     * @param singleType            Type of a single result obtained by the query.
     * @param singleTypeElementType Element type of singleType when singleType is an
     *                                  array or collection. Otherwise null.
     */
    @Trivial
    QueryInfo_1_0(RepositoryProducer<?> repositoryProducer,
                  Class<?> repositoryInterface,
                  Method method,
                  QueryType methodType,
                  Class<?> entityParamType,
                  boolean isOptional,
                  Class<?> returnArrayType,
                  Class<?> multiType,
                  Class<?> singleType,
                  Class<?> singleTypeElementType) {
        super(repositoryProducer, //
              repositoryInterface, //
              method, //
              methodType, //
              entityParamType, //
              isOptional, //
              multiType, //
              returnArrayType, //
              singleType, //
              singleTypeElementType);
    }

    /**
     * Appends the equality constraint.
     */
    @Override
    @Trivial
    protected StringBuilder appendConstraint(StringBuilder q,
                                             String o_,
                                             String attrName,
                                             AttributeConstraint constraint,
                                             int prevNumJPQLParams,
                                             boolean isCollection,
                                             Annotation[] annos) {
        if (attrName.charAt(attrName.length() - 1) != ')')
            q.append(o_);
        return q.append(attrName).append("=?").append(prevNumJPQLParams + 1);
    }

    @Override
    protected int generateConstraint(StringBuilder q,
                                     Object constraint,
                                     int jpqlParamCount,
                                     Set<String> jpqlParamNames,
                                     Map<Object, Object> jpqlParams) {
        throw new UnsupportedOperationException("jakarta.data.constraint.Constraint");
    }

    @Override
    protected int generateRestrictions(StringBuilder q,
                                       Object restriction,
                                       int jpqlParamCount,
                                       Set<String> jpqlParamNames,
                                       Map<Object, Object> jpqlParams) {
        throw new UnsupportedOperationException("jakarta.data.restrict.Restriction");
    }

    @Override
    @Trivial
    protected Map<Integer, Object> getDeferredConstraints(boolean alwaysDefer,
                                                          Object[] methodParams) {
        return Collections.emptyMap();
    }

    @Override
    @Trivial
    public int inspectMethodParam(int p,
                                  Class<?> paramType,
                                  Annotation[] paramAnnos,
                                  String[] attrNames,
                                  AttributeConstraint[] constraints,
                                  char[] updateOps,
                                  int prevNumJPQLParams) {
        // In Data 1.0, all constraints are the equality condition
        constraints[p] = AttributeConstraint.Equal;
        return prevNumJPQLParams + 1;
    }

    @Override
    @Trivial
    public Object[] toConstraintValues(Object value) {
        return null;
    }

}