/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.opentracing;

import java.lang.reflect.Method;

import org.eclipse.microprofile.opentracing.Traced;

/**
 * MicroProfile OpenTracing static utilities such as getting operation names.
 */
public class OpenTracingService {

    /**
     * Represents a method that is Traced with value = true and the default operation name.
     */
    private static final String OPERATION_NAME_TRACED = OpenTracingService.class.getName() + ".TRACED";

    /**
     * Represents a method that is Traced with value = false (i.e. untraced).
     */
    private static final String OPERATION_NAME_UNTRACED = OpenTracingService.class.getName() + ".UNTRACED";

    /**
     * If the declaring class of {@code method} doesn't have the {@code Traced}
     * annotation, then return {@code null}.
     * Otherwise: If the {@code Traced} value is {@code false}, then return
     * {@code OPERATION_NAME_UNTRACED}. If it's true and {@code operationName}
     * is specified, then return {@code operationName}; otherwise, return
     * {@code OPERATION_NAME_TRACED}.
     *
     * @param method The method to check.
     * @return Operation name or constant.
     */
    public static String getClassOperationName(Method method) {
        return getOperationName(method.getDeclaringClass().getAnnotation(Traced.class));
    }

    /**
     * If the {@code method} doesn't have the {@code Traced}
     * annotation, then return {@code null}.
     * Otherwise: If the {@code Traced} value is {@code false}, then return
     * {@code OPERATION_NAME_UNTRACED}. If it's true and {@code operationName}
     * is specified, then return {@code operationName}; otherwise, return
     * {@code OPERATION_NAME_TRACED}.
     *
     * @param method The method to check.
     * @return Operation name or constant.
     */
    public static String getMethodOperationName(Method method) {
        return getOperationName(method.getAnnotation(Traced.class));
    }

    /**
     * Return true if {@code operationName} is not null (i.e. it represents
     * something that has the {@code Traced} annotation) and if the
     * {@code Traced} annotation was not explicitly set to {@code false}.
     *
     * @param operationName The operation name to check
     * @return See above
     */
    private static boolean isTraced(String operationName) {
        return operationName != null && !OPERATION_NAME_UNTRACED.equals(operationName);
    }

    /**
     * Return true if {@code methodOperationName} is not null (i.e. it represents
     * something that has the {@code Traced} annotation) and if the
     * {@code Traced} annotation was not explicitly set to {@code false}, or return
     * true if {@code classOperationName} is not null (i.e. it represents
     * something that has the {@code Traced} annotation) and the
     * {@code Traced} annotation was not explicitly set to {@code false},
     * and the {@code methodOperationName} is not explicitly set to {@code false}.
     *
     * @param classOperationName The class operation name
     * @param methodOperationName The method operation name
     * @return See above
     */
    public static boolean isTraced(String classOperationName, String methodOperationName) {
        return isTraced(methodOperationName) || (isTraced(classOperationName) && !OPERATION_NAME_UNTRACED.equals(methodOperationName));
    }

    /**
     * Return true if {@code methodOperationName} is not null (i.e. it represents
     * something that has the {@code Traced} annotation) and if the
     * {@code Traced} annotation was explicitly set to {@code false}, or return
     * true if {@code classOperationName} is not null (i.e. it represents
     * something that has the {@code Traced} annotation) and the
     * {@code Traced} annotation was explicitly set to {@code false},
     * and the {@code methodOperationName} is not explicitly set to {@code true}.
     *
     * @param classOperationName The class operation name
     * @param methodOperationName The method operation name
     * @return See above
     */
    public static boolean isNotTraced(String classOperationName, String methodOperationName) {
        return OPERATION_NAME_UNTRACED.equals(methodOperationName) || (OPERATION_NAME_UNTRACED.equals(classOperationName) && !isTraced(methodOperationName));
    }

    /**
     * Return true if {@code operationName} is not {@code null} and not
     * {@code OPERATION_NAME_UNTRACED} and not {@code OPERATION_NAME_UNTRACED}.
     *
     * @param operationName The operation name to check.
     * @return See above
     */
    public static boolean hasExplicitOperationName(String operationName) {
        return operationName != null && !OPERATION_NAME_TRACED.equals(operationName) && !OPERATION_NAME_UNTRACED.equals(operationName);
    }

    /**
     * If {@code traced} has {@code value}
     * set to {@code true}, then return the {@code operationName} set on the
     * annotation, or if it's the default, return {@code OPERATION_NAME_TRACED}.
     * If {@code value} is set to {@code false}, return {@code OPERATION_NAME_UNTRACED}.
     *
     * @param traced The annotation to check
     * @return See above.
     */
    public static String getOperationName(Traced traced) {
        String operationName = null;
        if (traced != null) {
            if (traced.value()) {
                operationName = traced.operationName();
                if (operationName == null || operationName.length() == 0) {
                    operationName = OPERATION_NAME_TRACED;
                }
            } else {
                operationName = OPERATION_NAME_UNTRACED;
            }
        }
        return operationName;
    }
}
