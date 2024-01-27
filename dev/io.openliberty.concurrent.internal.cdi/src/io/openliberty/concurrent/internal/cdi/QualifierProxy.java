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
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Proxy that serves as an instance of a qualifier annotation.
 */
@Trivial
public class QualifierProxy implements InvocationHandler {
    private static final TraceComponent tc = Tr.register(QualifierProxy.class);

    /**
     * The hash code value for the qualifier annotation.
     */
    private final int hashCode;

    /**
     * Accessor methods for annotation fields.
     */
    private final List<Method> methods;

    /**
     * Qualifier annotation class.
     */
    private final Class<?> qualifierClass;

    /**
     * The toString value for the qualifier annotation.
     */
    private final String stringValue;

    /**
     * Create a invocation handler for the specified qualifier annotation class.
     *
     * @param qualifierClass qualifier annotation class.
     */
    QualifierProxy(Class<?> qualifierClass) {
        this.qualifierClass = qualifierClass;
        String qualifierClassName = qualifierClass.getName();

        Method[] m = qualifierClass.getMethods();
        methods = new ArrayList<>(m.length - 4); // leaving out annotationType, equals, hashCode, toString
        int hash = 0;
        StringBuilder s = new StringBuilder(m.length * 30 + qualifierClassName.length()) //
                        .append('@').append(qualifierClassName).append('(');

        boolean first = true;
        for (Method method : m)
            if (method.getParameterCount() == 0) {
                String name = method.getName();
                if (!"annotationType".equals(name)
                    && !"hashCode".equals(name)
                    && !"toString".equals(name)) {
                    methods.add(method);

                    Object value = method.getDefaultValue();

                    if (first)
                        first = false;
                    else
                        s.append(", ");

                    s.append(name).append('=');

                    int h;
                    if (value instanceof Object[] array) {
                        h = Arrays.hashCode(array);
                        s.append(Arrays.toString(array));
                    } else if (value instanceof int[] array) {
                        h = Arrays.hashCode(array);
                        s.append(Arrays.toString(array));
                    } else if (value instanceof long[] array) {
                        h = Arrays.hashCode(array);
                        s.append(Arrays.toString(array));
                    } else if (value instanceof boolean[] array) {
                        h = Arrays.hashCode(array);
                        s.append(Arrays.toString(array));
                    } else if (value instanceof double[] array) {
                        h = Arrays.hashCode(array);
                        s.append(Arrays.toString(array));
                    } else if (value instanceof float[] array) {
                        h = Arrays.hashCode(array);
                        s.append(Arrays.toString(array));
                    } else if (value instanceof short[] array) {
                        h = Arrays.hashCode(array);
                        s.append(Arrays.toString(array));
                    } else if (value instanceof byte[] array) {
                        h = Arrays.hashCode(array);
                        s.append(Arrays.toString(array));
                    } else if (value instanceof char[] array) {
                        h = Arrays.hashCode(array);
                        s.append(Arrays.toString(array));
                    } else {
                        h = value.hashCode();
                        s.append(value);
                    }

                    // JavaDoc for Annotation requires the hash code to be the sum of
                    // 127 times each member's name xor with the hash code of its value
                    hash += (127 * name.hashCode()) ^ h;
                }
            }

        stringValue = s.append(")[QualifierProxy]").toString();
        hashCode = hash;
    }

    /**
     * Compare a proxy qualifier with another instance.
     *
     * @param proxy qualifier that is implemented by QualifierProxy.
     * @param other other object that is possibly a matching qualifier instance.
     * @return true if both represent the same qualifier, otherwise false.
     */
    private boolean equals(Object proxy, Object other) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "equals",
                     proxy == null ? null : proxy.toString(),
                     other == null ? null : other.toString());

        boolean equal;
        if (proxy == other) {
            equal = true;
        } else if (qualifierClass.isInstance(other)) {
            try {
                boolean isProxy = Proxy.isProxyClass(other.getClass());
                InvocationHandler otherHandler = isProxy ? Proxy.getInvocationHandler(other) : null;
                if (otherHandler instanceof QualifierProxy) {
                    equal = ((QualifierProxy) otherHandler).qualifierClass.equals(qualifierClass);
                } else {
                    // The other instance is not a QualifierProxy, but it might still match.
                    if (methods.size() == 0) {
                        equal = true;
                    } else {
                        // For a proper comparison, meeting the requirements of the JavaDoc for Annotation.equals
                        // we need to invoke all methods (except for annotationType/equals/hashCode/toString)
                        // on both instances and compare the values.
                        equal = true;
                        for (Method method : methods) {
                            Object value1 = method.getDefaultValue();
                            Object value2 = method.invoke(other);
                            if (trace && tc.isDebugEnabled())
                                Tr.debug(this, tc, "comparing " + method.getName(), value1, value2);

                            equal = value1 instanceof Object[] array1 ? Arrays.equals(array1, (Object[]) value2) :
                            /*   */ value1 instanceof int[] array1 ? Arrays.equals(array1, (int[]) value2) :
                            /*   */ value1 instanceof long[] array1 ? Arrays.equals(array1, (long[]) value2) :
                            /*   */ value1 instanceof boolean[] array1 ? Arrays.equals(array1, (boolean[]) value2) :
                            /*   */ value1 instanceof double[] array1 ? Arrays.equals(array1, (double[]) value2) :
                            /*   */ value1 instanceof float[] array1 ? Arrays.equals(array1, (float[]) value2) :
                            /*   */ value1 instanceof short[] array1 ? Arrays.equals(array1, (short[]) value2) :
                            /*   */ value1 instanceof byte[] array1 ? Arrays.equals(array1, (byte[]) value2) :
                            /*   */ value1 instanceof char[] array1 ? Arrays.equals(array1, (char[]) value2) :
                            /*   */ Objects.equals(value1, value2);

                            if (!equal)
                                break;
                        }
                    }
                }
            } catch (RuntimeException x) {
                throw x;
            } catch (Exception x) {
                throw new RuntimeException(x);
            }
        } else {
            equal = false;
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "equals", equal);
        return equal;
    }

    /**
     * Implements the 4 methods of java.lang.Annotation:
     * hashCode(), toString(), equals(other), and annotationType().
     *
     * Handles annotation fields with default values by delegating to the default value.
     *
     * @throws UnsupportedOperationException for all other methods.
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        int numParams = method.getParameterCount();

        if (numParams == 0 && "hashCode".equals(methodName)) {
            return hashCode;
        } else if (numParams == 0 && "toString".equals(methodName)) {
            return stringValue;
        } else if (numParams == 0 && "annotationType".equals(methodName)) {
            return qualifierClass;
        } else if (numParams == 1 && "equals".equals(methodName)) {
            return equals(proxy, args[0]);
        } else {
            return method.getDefaultValue();
        }
    }
}