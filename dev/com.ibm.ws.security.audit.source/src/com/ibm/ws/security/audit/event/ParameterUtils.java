/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.audit.event;

/**
 *
 */
public class ParameterUtils {

    static protected StringBuffer format(Object param) {
        StringBuffer buf = new StringBuffer();
        if (param != null) {
            if (param.getClass().isArray()) {
                if (param.getClass().getComponentType().isPrimitive()) {
                    buf.append(PrimitiveArrayToString(param));
                } else if (param instanceof Object[]) {
                    buf.append("[");
                    boolean first = true;
                    for (Object element : (Object[]) param) {
                        if (element != null) {
                            if (first) {
                                first = false;
                            } else {
                                buf.append(", ");
                            }
                            if (element.getClass().isArray()) {
                                buf.append(format(element));
                            } else {
                                buf.append(element.toString());
                            }
                        }
                    }
                    buf.append("]");
                }
            } else {
                buf.append(param.toString());
            }
        }
        return buf;
    }

    static private String PrimitiveArrayToString(Object param) {
        if (param instanceof long[]) {
            return java.util.Arrays.toString((long[]) param);
        } else if (param instanceof boolean[]) {
            return java.util.Arrays.toString((boolean[]) param);
        } else if (param instanceof byte[]) {
            return java.util.Arrays.toString((byte[]) param);
        } else if (param instanceof int[]) {
            return java.util.Arrays.toString((int[]) param);
        } else if (param instanceof short[]) {
            return java.util.Arrays.toString((short[]) param);
        } else if (param instanceof char[]) {
            return java.util.Arrays.toString((char[]) param);
        } else if (param instanceof float[]) {
            return java.util.Arrays.toString((float[]) param);
        } else if (param instanceof double[]) {
            return java.util.Arrays.toString((double[]) param);
        }
        return "";
    }
}
