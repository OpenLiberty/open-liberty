/*******************************************************************************
 * Copyright (c) 2007, 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.injectionengine.InternalInjectionEngine;

/**
 * A utility for collecting all methods of a class, including private but
 * excluding methods defined on a parent class that have been overridden. <p>
 */
public class MethodMap extends HashMap<String, List<Method>>
{
    private static final long serialVersionUID = 4877455330504864506L;
    private static final String CLASS_NAME = MethodMap.class.getName();

    private static final TraceComponent tc = Tr.register(MethodMap.class,
                                                         InjectionConfigConstants.traceString,
                                                         InjectionConfigConstants.messageFile);

    /**
     * Returns true if the modifiers have default access.
     */
    // RTC102289
    static boolean modifierIsDefaultAccess(int modifiers)
    {
        return (modifiers & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE)) == 0;
    }

    /**
     * Returns the package name for a class, or the empty string if the class
     * belongs to the unnamed package.
     */
    // RTC102289
    private static String getPackageName(Class<?> klass)
    {
        String className = klass.getName();
        int index = className.lastIndexOf('.');
        return index == -1 ? "" : className.substring(0, index);
    }

    /**
     * Convenience method for obtaining all of the methods for a class,
     * including private, but excluding methods on parents that have
     * been overridden. Methods are returned in class hierarchy order,
     * with methods on the specified class returned first, followed by
     * the methods of its immediate super class, and so on. <p>
     *
     * If an interface or null is specified, then an empty collection
     * will be returned. Interfaces do not need to be evaluated for
     * injection annotations. <p>
     *
     * @param clazz defining the methods of interest
     * @return all of the methods for the specified class
     * @throws SecurityException if the caller does not have permission
     *             to use reflection or access class loaders
     */
    public static Collection<MethodInfo> getAllDeclaredMethods(Class<?> clazz)
    {
        return getMethods(clazz, true);
    }

    /**
     * Convenience method for obtaining all of the methods for a class,
     * excluding private and methods on parents that have been overridden.
     * Methods are returned in class hierarchy order, with methods on the
     * specified class returned first, followed by the methods of its
     * immediate superclass, and so on. <p>
     *
     * If an interface or null is specified, then an empty collection
     * will be returned. Interfaces do not need to be evaluated for
     * injection annotations. <p>
     *
     * @param clazz defining the methods of interest
     * @return all of the non-overridden methods for the specified class
     * @throws SecurityException if the caller does not have permission
     *             to use reflection or access class loaders
     */
    // d719917
    public static Collection<MethodInfo> getAllNonPrivateMethods(Class<?> clazz)
    {
        return getMethods(clazz, false);
    }

    private static Collection<MethodInfo> getMethods(Class<?> clazz, boolean includePrivate)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getAllDeclaredMethods", clazz);

        // Not expecting null or an interface, but have ignored them in
        // the past, so continue to ignore them now.                       d658626
        if (clazz == null || clazz.isInterface())
        {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getAllDeclaredMethods : None - interface or null.");
            return Collections.emptyList();
        }

        Class<?> thisClass = null;
        Collection<MethodInfo> methodInfos = new LinkedHashSet<MethodInfo>();

        try
        {
            Map<MethodInfo, Set<RuntimePackage>> defaultAccessMethods = null; // RTC102289

            // Find all non-overridden methods, recording any methods with default
            // access modifiers ("package private") that appear to be overridden so
            // they can be handled specially.  Consider the following hierarchy:
            //
            //   package pkg1; class A { void m() { ... } }
            //   package pkg2; class B extends A { public void m() { ... } }
            //
            // In this case, B.m does not override A.m because A.m has default
            // (package private) access, and the runtime package of B does not
            // match the runtime package of A, so we should return [A.m, B.m].
            // We expect this scenario to be rare, so it is not worth the overhead
            // of tracking the data necessary to handle this case in the normal
            // code path.  Instead, we record that such a scenario might have
            // occurred, and then we re-process all the methods in the hierarchy.
            int classDepth = 0;
            for (thisClass = clazz; thisClass != Object.class; thisClass = thisClass.getSuperclass(), classDepth++)
            {
                for (Method method : thisClass.getDeclaredMethods())
                {
                    if (includePrivate || !Modifier.isPrivate(method.getModifiers()))
                    {
                        MethodInfo methodInfo = new MethodInfo(method, classDepth);
                        boolean added = methodInfos.add(methodInfo);

                        if (!added && modifierIsDefaultAccess(method.getModifiers()))
                        {
                            // Some other method has already been added with the same
                            // signature, but it might not actually override this
                            // method if the runtime package does not match.
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "potentially non-overridden " + methodInfo);

                            if (defaultAccessMethods == null)
                            {
                                defaultAccessMethods = new HashMap<MethodInfo, Set<RuntimePackage>>();
                            }

                            if (!defaultAccessMethods.containsKey(methodInfo))
                            {
                                defaultAccessMethods.put(methodInfo, new HashSet<RuntimePackage>());
                            }
                        }
                    }
                }
            }

            // Were there any methods with default access modifiers that were
            // potentially missed?                                        RTC102289
            if (defaultAccessMethods != null)
            {
                List<MethodInfo> newMethodInfos = null;

                classDepth = 0;
                for (thisClass = clazz; thisClass != Object.class; thisClass = thisClass.getSuperclass(), classDepth++)
                {
                    String thisClassPackageName = null;
                    ClassLoader thisClassLoader = null;

                    for (Method method : thisClass.getDeclaredMethods())
                    {
                        if (!Modifier.isPrivate(method.getModifiers()))
                        {
                            MethodInfo methodInfo = new MethodInfo(method, classDepth);
                            Set<RuntimePackage> methodPackages = defaultAccessMethods.get(methodInfo);
                            if (methodPackages != null)
                            {
                                // Lazily initialize class data for the first matching
                                // method in a specific class.
                                if (thisClassPackageName == null)
                                {
                                    thisClassPackageName = getPackageName(thisClass);
                                    thisClassLoader = thisClass.getClassLoader();
                                }

                                boolean first = methodPackages.isEmpty();
                                boolean added = methodPackages.add(new RuntimePackage(thisClassLoader, thisClassPackageName));

                                // If this is the first method with this signature, then
                                // it was added to methodInfos in the first pass.
                                // Otherwise, if this is the first method from this
                                // package and the method has default access, then the
                                // first pass considered this to be an overridden method
                                // even though it is not considered overridden by the
                                // JVM because the runtime packages do not match.
                                //
                                // Example 1 (from above):
                                //   package pkg1; class A { void m() { ... } }
                                //   package pkg2; class B extends A { public void m() { ... } }
                                //
                                //   - B.m: first=true,  added=true -> already in methodInfos
                                //   - A.m: first=false, added=true -> newMethodInfos.add
                                //
                                // Example 2:
                                //   package pkg1; class A { void m() { ... } }
                                //   package pkg1; class A2 extends A { void m() { ... } }
                                //   package pkg2; class B extends A2 { public void m() { ... } }
                                //
                                //   - B.m:  first=true,  added=true  -> already in methodInfos
                                //   - A2.m: first=false, added=true  -> newMethodInfos.add
                                //   - A1.m: first=false, added=false -> skip
                                //
                                // Example 2:
                                //   package pkg1; class A { void m() { ... } }
                                //   package pkg1; class A2 extends A { public void m() { ... } }
                                //   package pkg2; class B extends A2 { public void m() { ... } }
                                //
                                //   - B.m:  first=true,  added=true  -> already in methodInfos
                                //   - A2.m: first=false, added=true  -> skip
                                //   - A1.m: first=false, added=false -> skip
                                //
                                // (Note, we don't make any accommodations for subclass
                                // methods with reduced visibility.  Such classes cannot
                                // be compiled normally using javac.)
                                if (!first && added && modifierIsDefaultAccess(method.getModifiers()))
                                {
                                    if (isTraceOn && tc.isDebugEnabled())
                                        Tr.debug(tc, "non-overridden " + methodInfo);

                                    if (newMethodInfos == null)
                                    {
                                        newMethodInfos = new ArrayList<MethodInfo>();
                                    }
                                    newMethodInfos.add(methodInfo);
                                }
                            }
                        }
                    }
                }

                if (newMethodInfos != null)
                {
                    // New methods can't easily be inserted in class depth order,
                    // so create an array with old+new, sort, and reassign.
                    MethodInfo[] methodInfosArray = new MethodInfo[methodInfos.size() + newMethodInfos.size()];
                    methodInfos.toArray(methodInfosArray);
                    for (int i = 0, out = methodInfos.size(); i < newMethodInfos.size(); i++)
                    {
                        methodInfosArray[out++] = newMethodInfos.get(i);
                    }

                    Collections.sort(newMethodInfos, new Comparator<MethodInfo>()
                    {
                        public int compare(MethodInfo methodInfo1, MethodInfo methodInfo2)
                        {
                            return methodInfo1.ivClassDepth - methodInfo2.ivClassDepth;
                        }
                    });
                    methodInfos = Arrays.asList(methodInfosArray);
                }
            }
        } catch (Throwable ex)
        {
            // The most common 'problem' here is a NoClassDefFoundError because
            // a dependency class (super/field type, etc) could not be found
            // when the class is fully initialized.

            // Since interrogating a class for annotations is new in Java EE 1.5,
            // it is possible this application may have worked in prior versions
            // of WebSphere, if the application never actually used the class.
            // So, rather than just fail the app start, a Warning will be logged
            // indicating the class will not be processed for annotations, and
            // the application will be allowed to start.
            FFDCFilter.processException(ex, CLASS_NAME + ".getAllDeclaredMethods",
                                        "106", new Object[] { thisClass, clazz, methodInfos });

            InternalInjectionEngine injectionEngine = InjectionEngineAccessor.getInternalInstance();
            if (thisClass != clazz)
            {
                Tr.warning(tc, "SUPER_METHOD_ANNOTATIONS_IGNORED_CWNEN0050W",
                           thisClass.getName(), clazz.getName(), ex.toString());
                if (injectionEngine == null || injectionEngine.isValidationFailable(false)) // F743-14449, F50309.6
                {
                    throw new RuntimeException("Resource annotations on the methods of the " + thisClass.getName() +
                                               " class could not be processed. The " + thisClass.getName() +
                                               " class is being processed for annotations because it is" +
                                               " referenced by the " + clazz.getName() + " application class." +
                                               " The annotations could not be obtained because of the exception : " +
                                               ex, ex);
                }
            }
            else
            {
                Tr.warning(tc, "METHOD_ANNOTATIONS_IGNORED_CWNEN0049W", thisClass.getName(), ex.toString());
                if (injectionEngine == null || injectionEngine.isValidationFailable(false)) // F743-14449, F50309.6
                {
                    throw new RuntimeException("Resource annotations on the methods of the " + thisClass.getName() +
                                               " class could not be processed. The annotations could not be obtained" +
                                               " because of the exception : " + ex, ex);
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getAllDeclaredMethods : " + clazz + " : " + methodInfos);
        return methodInfos;
    }

    /**
     * A wrapper for a java.lang.reflect.Method object. The behaviors of the {@link #hashCode} and {@link #equals} methods are undefined.
     */
    public static final class MethodInfo // d666251
    {
        private final Method ivMethod;
        private final Class<?>[] ivParamTypes;
        private final int ivClassDepth;

        private final int ivHashCode;

        MethodInfo(Method method, int classDepth)
        {
            ivMethod = method;
            ivParamTypes = method.getParameterTypes();
            ivClassDepth = classDepth;

            ivHashCode = method.getName().hashCode() * 31 + Arrays.hashCode(ivParamTypes);
        }

        @Override
        public String toString()
        {
            return ivMethod.toString() + ':' + ivClassDepth;
        }

        @Override
        public int hashCode()
        {
            return ivHashCode;
        }

        @Override
        public boolean equals(Object object)
        {
            if (object instanceof MethodInfo)
            {
                MethodInfo info = (MethodInfo) object;
                return ivHashCode == info.ivHashCode &&
                       !Modifier.isPrivate(ivMethod.getModifiers()) &&
                       !Modifier.isPrivate(info.ivMethod.getModifiers()) &&
                       ivMethod.getName().equals(info.ivMethod.getName()) &&
                       Arrays.equals(ivParamTypes, info.ivParamTypes);
            }

            return false;
        }

        /**
         * Returns the method object.
         */
        public Method getMethod()
        {
            return ivMethod;
        }

        /**
         * Returns the number of parameters.
         */
        public int getNumParameters()
        {
            return ivParamTypes.length;
        }

        /**
         * Returns the type of the specified parameter.
         */
        public Class<?> getParameterType(int index)
        {
            return ivParamTypes[index];
        }

        /**
         * Returns the depth of the declaring class in the class hierarchy.
         */
        public int getClassDepth()
        {
            return ivClassDepth;
        }
    }

    /**
     * A runtime package as defined by the Java Virtual Machine specification:
     * tuple of class loader and package name.
     */
    // RTC102289
    private static class RuntimePackage
    {
        private final ClassLoader ivClassLoader;
        private final String ivPackageName;
        private final int ivHashCode;

        private RuntimePackage(ClassLoader classLoader, String packageName)
        {
            ivClassLoader = classLoader;
            ivPackageName = packageName;

            ivHashCode = System.identityHashCode(classLoader) * 31 + packageName.hashCode();
        }

        @Override
        public String toString()
        {
            return super.toString() + '[' + ivClassLoader + ", " + ivPackageName + ']';
        }

        @Override
        public int hashCode()
        {
            return ivHashCode;
        }

        @Override
        public boolean equals(Object object)
        {
            if (object instanceof RuntimePackage)
            {
                RuntimePackage runtimePackage = (RuntimePackage) object;
                return ivHashCode == runtimePackage.ivHashCode &&
                       ivClassLoader == runtimePackage.ivClassLoader &&
                       ivPackageName.equals(runtimePackage.ivPackageName);
            }

            return false;
        }
    }
}
