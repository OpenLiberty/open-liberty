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
package com.ibm.ws.microprofile.faulttolerance.utils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 *
 */
public class FTDebug {

    @Trivial
    public static void debugRelativeTime(TraceComponent tc, String id, String message, long relativePointA) {
        debugRelativeTime(tc, id, message, relativePointA, System.nanoTime());
    }

    @Trivial
    public static void debugRelativeTime(TraceComponent tc, String id, String message, long relativePointA, long relativePointB) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "{0}: {1}: {2}", id, message, relativeSeconds(relativePointA, relativePointB));
        }
    }

    @Trivial
    public static void debugTime(TraceComponent tc, String message, long time) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "{0}: {1}", message, toSeconds(time));
        }
    }

    @Trivial
    public static void debugTime(TraceComponent tc, String id, String message, long time) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "{0}: {1}: {2}", id, message, toSeconds(time));
        }
    }

    /**
     * Format a method for display in a log message
     * <p>
     * Uses the format:
     * <p>
     * {@code com.example.MyClass.InnerClass.myMethod(String, Map, MyBean)}
     * <p>
     * This is a compromise between including enough information to identify the method, while not being too verbose like {@code Method.toString()}
     *
     * @param m the method
     * @return a string representation of the method
     */
    @Trivial
    public static String formatMethod(Method m) {
        if (m == null) {
            return "null";
        }
    
        StringBuilder sb = new StringBuilder();
        sb.append(m.getDeclaringClass().getName());
        sb.append(".");
        sb.append(m.getName());
    
        sb.append("(");
        sb.append(Arrays.stream(m.getParameterTypes()).map(Class::getSimpleName).collect(Collectors.joining(", ")));
        sb.append(")");
    
        return sb.toString();
    }

    //in seconds, how long between two a relative points (nanoTime)
    @Trivial
    public static double relativeSeconds(long relativePointA, long relativePointB) {
        long diff = relativePointB - relativePointA;
        double seconds = toSeconds(diff);
        return seconds;
    }

    //convert from nanos (long) to seconds (double)
    @Trivial
    public static double toSeconds(long nanos) {
        return ((double) nanos / (double) 1000000000);
    }

}
