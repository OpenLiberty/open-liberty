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
package io.openliberty.concurrent.internal.cdi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Proxy that serves as an instance of a qualifier annotation.
 */
@Trivial
public class QualifierProxy implements InvocationHandler {
    /**
     * Qualifier annotation class.
     */
    private final Class<?> qualifierClass;

    /**
     * Create a invocation handler for the specified qualifier annotation class.
     *
     * @param qualifierClass qualifier annotation class.
     */
    QualifierProxy(Class<?> qualifierClass) {
        this.qualifierClass = qualifierClass;
    }

    /**
     * Implements the 4 methods of java.lang.Annotation:
     * hashCode(), toString(), equals(other), and annotationType().
     *
     * TODO implement other methods from the annotation class by returning the default value
     * and otherwise raising an error.
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        int numParams = method.getParameterCount();

        if (numParams == 0 && "hashCode".equals(methodName)) {
            return qualifierClass.hashCode();
        } else if (numParams == 0 && "toString".equals(methodName)) {
            return new StringBuilder(qualifierClass.getName()) //
                            .append('@').append(Integer.toHexString(qualifierClass.hashCode())) //
                            .append("(Proxy)") //
                            .toString();
        } else if (numParams == 0 && "annotationType".equals(methodName)) {
            return qualifierClass;
        } else if (numParams == 1 && "equals".equals(methodName)) {
            if (qualifierClass.isInstance(args[0]))
                if (qualifierClass.getMethods().length == 4)
                    return true;
                else // TODO For a proper comparison, would need to invoke additional methods
                     // and compare value from the other instance against with default values from this instance
                    throw new UnsupportedOperationException();
            else
                return false;
        } else {
            // This can be implemented to return the default value if there is one,
            // but otherwise it is an error to use the annotation as a qualifier for Concurrency resources.
            throw new UnsupportedOperationException(); // TODO implementation and better error message
        }
    }
}