/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.diagnostics;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractIntrospection implements PlatformIntrospection {
    private final static String[] openJPAEntityManagerFactoryImplClasses = { "org.apache.openjpa.persistence.EntityManagerFactoryImpl",
                                                                             "com.ibm.ws.persistence.EntityManagerFactoryImpl" };
    private final static String[] eclipselinkEntityManagerFactoryImplClasses = { "org.eclipse.persistence.internal.jpa.EntityManagerFactoryImpl" };

    public static boolean isSupportedPlatform(Class jpaProviderImplClass) {
        if (jpaProviderImplClass == null) {
            return false;
        }

        final String className = jpaProviderImplClass.getName();

        if (eclipselinkEntityManagerFactoryImplClasses[0].equals(className)) {
            return true;
        }

        if (openJPAEntityManagerFactoryImplClasses[0].equals(className) || openJPAEntityManagerFactoryImplClasses[1].equals(className)) {
            return true;
        }

        return false;
    }

    public static PlatformIntrospection getPlatformIntrospection(Class jpaProviderImplClass) {
        if (jpaProviderImplClass == null) {
            return null;
        }

        final String className = jpaProviderImplClass.getName();

        if (eclipselinkEntityManagerFactoryImplClasses[0].equals(className)) {
            return new EclipselinkIntrospection();
        }

        if (openJPAEntityManagerFactoryImplClasses[0].equals(className) || openJPAEntityManagerFactoryImplClasses[1].equals(className)) {
            return new OpenJPAIntrospection();
        }

        return null;
    }

    /*
     * Utility methods used for introspecting JPA Persistence Provider Implementation Data Structures
     */
    protected static Object reflectObjValue(Object o, String field) throws Exception {
        final Class<?> c = o.getClass();
        final Field f = findField(c, field); //        c.getField(field);
        if (f == null) {
            return null;
        }
        final boolean accessible = f.isAccessible();
        try {
            f.setAccessible(true);
            return f.get(o);
        } finally {
            f.setAccessible(accessible);
        }
    }

    protected static Field findField(final Class<?> c, String field) {
        if (c == null) {
            return null;
        }

        try {
            return c.getDeclaredField(field);
        } catch (Exception e) {

        }

        if (Object.class.equals(c)) {
            return null;
        }

        return findField(c.getSuperclass(), field);
    }

    protected static Object reflectMethodCall(Object o, Method m) throws Exception {
        if (m == null) {
            return null;
        }

        final boolean accessible = m.isAccessible();
        try {
            m.setAccessible(true);
            return m.invoke(o);
        } finally {
            m.setAccessible(accessible);
        }
    }

    protected static Object reflectMethodCall(Object o, String method) throws Exception {
        final Class<?> c = o.getClass();
        final Method m = findMethod(c, method);

        return reflectMethodCall(o, m);
    }

    protected static Method findMethod(final Class<?> c, String methodName) {
        if (c == null) {
            return null;
        }

        try {
            Method[] methods = c.getMethods();
            if (methods != null) {
                for (Method m : methods) {
                    if (!m.equals(methodName)) {
                        continue;
                    }

                    final Class[] pt = m.getParameterTypes();

                    if (pt != null && pt.length > 0) {
                        // Cannot support calling a method with any arguments
                        continue;
                    }

                    return m;
                }
            }
        } catch (Exception e) {

        }

        if (Object.class.equals(c)) {
            return null;
        }

        return findMethod(c.getSuperclass(), methodName);
    }

    protected static List<Method> getMethodsWithPrefix(final Class<?> c, final String prefix) {
        ArrayList<Method> methodList = new ArrayList<Method>();

        if (c == null) {
            return methodList;
        }

        try {
            Method[] methods = c.getDeclaredMethods();
            if (methods != null) {
                for (Method m : methods) {
                    if (!m.getName().startsWith(prefix)) {
                        continue;
                    }

                    final Class[] pt = m.getParameterTypes();

                    if (pt != null && pt.length > 0) {
                        // Cannot support calling a method with any arguments
                        continue;
                    }

                    methodList.add(m);
                }
            }
        } catch (Exception e) {

        }

        if (!Object.class.equals(c)) {
            methodList.addAll(getMethodsWithPrefix(c.getSuperclass(), prefix));
        }

        return methodList;
    }

    protected static boolean isCastable(String superclassClass, Class<?> c) {
        return isCastable(superclassClass, c, new HashSet<Class<?>>());
    }

    private static boolean isCastable(String superclassClass, Class<?> c, Set<Class<?>> checkedSet) {
        if (c == null || superclassClass == null || superclassClass.trim().isEmpty() || checkedSet.contains(c)) {
            return false;
        }

        if (superclassClass.equals(c.getName())) {
            return true;
        }

        checkedSet.add(c);

        final Class<?>[] iFaces = c.getInterfaces();
        if (iFaces != null && iFaces.length > 0) {
            for (Class<?> iFace : iFaces) {
                if (isCastable(superclassClass, iFace)) {
                    return true;
                }
            }
        }

        if (Object.class != c) {
            return isCastable(superclassClass, c.getSuperclass());
        }

        return false;
    }

    protected static String getObjectAddress(Object o) {
        if (o == null) {
            return "";
        }

        if (o.getClass().isPrimitive()) {
            return ""; // Primitives do not have object addresses
        }

        return "@" + Integer.toHexString(System.identityHashCode(o));
    }

    protected static String poa(Object o, String indent, boolean dumpCollection) {
        return poa(o, indent, dumpCollection, 0);
    }

    protected static String poa(Object o, String indent, boolean dumpCollection, int depth) {
        if (dumpCollection == false || o == null || depth > 5) {
            return poa(o);
        }

        final Class<?> oClass = o.getClass();
        final StringBuilder sb = new StringBuilder();

        if (oClass.isArray()) {
            sb.append(getInstanceClassAndAddress(o));
            final Object[] objarr = (Object[]) o;
            int idx = 0;
            for (Object obj : objarr) {
                sb.append("\n").append(indent).append(idx++).append(" : ");
                sb.append(poa(obj, indent + "   ", true, depth++));
            }
        } else if (isCastable("java.util.Collection", oClass)) { // (Collection.class.isInstance(o)) {
            sb.append(getInstanceClassAndAddress(o));
            sb.append(":");
            Collection<?> c = (Collection<?>) o;
            for (Object obj : c) {
                sb.append("\n").append(indent).append(poa(obj, indent + "   ", true, depth++));
            }
        } else if (isCastable("java.util.Map", oClass)) { // (Collection.class.isInstance(o)) {
            sb.append(getInstanceClassAndAddress(o));
            sb.append(":");
            final Map<?, ?> map = (Map<?, ?>) o;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sb.append("\n").append(indent).append(poa(entry.getKey())).append(" |---> ");
                sb.append(poa(entry.getValue(), indent + "   ", true, depth++));
            }
        } else {
            return poa(o);
        }

        return sb.toString();
    }

    /**
     * Print Object toString() and Address (does not navigate into Collections or Arrays)
     */
    protected static String poa(Object o) {
        if (o == null) {
            return "<<null>>";
        }

        return o.toString() + " " + getObjectAddress(o);
    }

    protected static String getInstanceClassAndAddress(Object o) {
        if (o == null) {
            return "<<null>>";
        }

        final Class<?> oClass = o.getClass();
        if (oClass.isPrimitive()) {
            return oClass.getName();
        }
        return oClass.getName() + getObjectAddress(o);
    }

}
