/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

// Liberty-created utility class for @Local EJB support
// This class does not exist in RESTEasy 7.0.0.Final upstream

package org.jboss.resteasy.spi.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;

public class Utils {

    public static boolean methodsMatch(Method resourceMethod, Method ifaceMethod) {
        // Name must match
        if (!resourceMethod.getName().equals(ifaceMethod.getName())) {
            return false;
        }
        
        // Parameter types must match exactly
        if (!Arrays.equals(resourceMethod.getParameterTypes(), 
                           ifaceMethod.getParameterTypes())) {
            return false;
        }
        
        // Return type: class method return must be same or subtype (covariant)
        if (!ifaceMethod.getReturnType().isAssignableFrom(resourceMethod.getReturnType())) {
            return false;
        }
        
        return true;
    }
    
    /*
     * Use reflection to get @jakarta.ejb.Local or @javax.ejb.Local interfaces without an EJB dependency.
     */
    public static Class<?>[] getLocalInterfaces(Class<?> clazz) {
        // Use reflection since we don't have an EJB dependency at compile time.
        final Annotation localAnno = getLocalAnnotation(clazz);
        
        Class<?>[] localInterfaces = null;
        if (localAnno != null) {
            try {
                Method m = localAnno.annotationType().getMethod("value");
                localInterfaces = (Class<?>[]) m.invoke(localAnno);
            } catch (Exception e) {} // do nothing - TODO: review, do we need to log a message?

            return localInterfaces;
        }
        return null;
    }
    
    private static Annotation getLocalAnnotation(Class<?> clazz) {
        for (Annotation anno : clazz.getAnnotations()) {
            String annotationName = anno.annotationType().getName();
            if (annotationName.equals("jakarta.ejb.Local") || annotationName.equals("javax.ejb.Local")) {
                return anno;
            }
        }
        return null;
    }

}
