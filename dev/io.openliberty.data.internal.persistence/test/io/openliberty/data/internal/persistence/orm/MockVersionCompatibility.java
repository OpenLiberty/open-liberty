/*******************************************************************************
 * Copyright (c) 2025,2026 IBM Corporation and others.
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
package io.openliberty.data.internal.persistence.orm;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.data.internal.AttributeConstraint;
import io.openliberty.data.internal.QueryType;
import io.openliberty.data.internal.version.DataVersionCompatibility;
import jakarta.data.repository.By;
import jakarta.data.repository.Find;

/**
 * A mock DataVersionCompatibility implementation.
 */
public class MockVersionCompatibility implements DataVersionCompatibility {

    @Override
    @Trivial
    public StringBuilder appendConstraint(StringBuilder q,
                                          String o_,
                                          String attrName,
                                          AttributeConstraint constraint,
                                          int qp,
                                          boolean isCollection,
                                          Annotation[] annos) {
        return q;
    }

    @Override
    @Trivial
    public boolean atLeast(int major, int minor) {
        return false;
    }

    @Override
    public int generateConstraint(StringBuilder q,
                                  String entityVar_,
                                  Object constraint,
                                  int jpqlParamCount,
                                  Set<String> jpqlParamNames,
                                  Map<Object, Object> jpqlParams) {
        throw new UnsupportedOperationException("jakarta.data.constraint.Constraint");
    }

    @Override
    @Trivial
    public int generateRestrictions(StringBuilder q,
                                    String entityVar_,
                                    Object restriction,
                                    int jpqlParamCount,
                                    Set<String> jpqlParamNames,
                                    Map<Object, Object> jpqlParams) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Trivial
    public Annotation getCountAnnotation(Method method) {
        return null;
    }

    @Override
    @Trivial
    public Map<Integer, Object> getDeferredConstraints(boolean alwaysDefer,
                                                       int maxIndex,
                                                       Object[] methodParams) {
        return Collections.emptyMap();
    }

    @Override
    @Trivial
    public Class<?> getEntityClass(Find find) {
        return void.class;
    }

    @Override
    @Trivial
    public Annotation getExistsAnnotation(Method method) {
        return null;
    }

    @Override
    @Trivial
    public String[] getSelections(AnnotatedElement element) {
        return NO_SELECTIONS;
    }

    @Override
    @Trivial
    public String[] getUpdateAttributeAndOperation(Annotation[] annos) {
        return null;
    }

    @Override
    @Trivial
    public int inspectMethodParam(int p,
                                  Class<?> paramType,
                                  Annotation[] paramAnnos,
                                  String[] attrNames,
                                  AttributeConstraint[] constraints,
                                  char[] updateOps,
                                  int qpNext) {
        return qpNext;
    }

    @Override
    @Trivial
    public boolean isRestriction(Object param) {
        return false;
    }

    @Override
    @Trivial
    public boolean isSpecialParamValid(Class<?> paramType,
                                       QueryType queryType) {
        return true;
    }

    @Override
    @Trivial
    public Set<Class<? extends Annotation>> lifeCycleAnnoTypes(boolean stateful) {
        return Set.of();
    }

    @Override
    @Trivial
    public Set<Class<? extends Annotation>> operationAnnoTypes(boolean stateful) {
        return Set.of();
    }

    @Override
    @Trivial
    public String paramAnnosForUpdate() {
        return By.class.getName();
    }

    @Override
    @Trivial
    public String persistenceFeatureName() {
        return "";
    }

    @Override
    @Trivial
    public Set<Class<?>> resourceAccessorTypes(boolean stateful) {
        return Set.of();
    }

    @Override
    @Trivial
    public String specialParamsForFind() {
        return "";
    }

    @Override
    @Trivial
    public String specialParamsForFindAndDelete() {
        return "";
    }

    @Override
    @Trivial
    public Set<Class<?>> specialParamTypes() {
        return Set.of();
    }

    @Override
    @Trivial
    public Object[] toConstraintValues(Object value) {
        return null;
    }

}