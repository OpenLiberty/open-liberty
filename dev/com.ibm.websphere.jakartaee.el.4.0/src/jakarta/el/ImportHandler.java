/*
 * Copyright (c) 2012, 2020 Oracle and/or its affiliates and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package jakarta.el;

import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isInterface;
import static java.lang.reflect.Modifier.isPublic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Handles imports of class names and package names. An imported package name implicitly imports all the classes in the
 * package. A class that has been imported can be used without its package name. The name is resolved to its full
 * (package and class) name at evaluation time.
 */
public class ImportHandler {

    private Map<String, String> classNameMap = new HashMap<>();
    private Map<String, Class<?>> classMap = new HashMap<>();
    private Map<String, String> staticNameMap = new HashMap<>();
    private HashSet<String> notAClass = new HashSet<>();
    private List<String> packages = new ArrayList<>();

    {
        importPackage("java.lang");
    }

    /**
     * Import a static field or method.
     *
     * @param name The static member name, including the full class name, to be imported
     * @throws ELException if the name does not include a ".".
     */
    public void importStatic(String name) throws ELException {
        int i = name.lastIndexOf('.');
        if (i <= 0) {
            throw new ELException("The name " + name + " is not a full static member name");
        }

        String memberName = name.substring(i + 1);
        String className = name.substring(0, i);

        staticNameMap.put(memberName, className);
    }

    /**
     * Import a class.
     *
     * @param name The full class name of the class to be imported
     * @throws ELException if the name does not include a ".".
     */
    public void importClass(String name) throws ELException {
        int i = name.lastIndexOf('.');
        if (i <= 0) {
            throw new ELException("The name " + name + " is not a full class name");
        }

        String className = name.substring(i + 1);

        classNameMap.put(className, name);
    }

    /**
     * Import all the classes in a package.
     *
     * @param packageName The package name to be imported
     */
    public void importPackage(String packageName) {
        packages.add(packageName);
    }

    /**
     * Resolve a class name.
     *
     * @param name The name of the class (without package name) to be resolved.
     * @return If the class has been imported previously, with {@link #importClass} or {@link #importPackage}, then its
     * Class instance. Otherwise <code>null</code>.
     * @throws ELException if the class is abstract or is an interface, or not public.
     */
    public Class<?> resolveClass(String name) {
        String className = classNameMap.get(name);
        if (className != null) {
            return resolveClassFor(className);
        }

        for (String packageName : packages) {
            String fullClassName = packageName + "." + name;
            Class<?> c = resolveClassFor(fullClassName);
            if (c != null) {
                classNameMap.put(name, fullClassName);
                return c;
            }
        }

        return null;
    }

    /**
     * Resolve a static field or method name.
     *
     * @param name The name of the member(without package and class name) to be resolved.
     * @return If the field or method has been imported previously, with {@link #importStatic}, then the class object
     * representing the class that declares the static field or method. Otherwise <code>null</code>.
     * @throws ELException if the class is not public, or is abstract or is an interface.
     */
    public Class<?> resolveStatic(String name) {
        String className = staticNameMap.get(name);
        if (className != null) {
            Class<?> c = resolveClassFor(className);
            if (c != null) {
                return c;
            }
        }

        return null;
    }

    private Class<?> resolveClassFor(String className) {
        Class<?> c = classMap.get(className);
        if (c != null) {
            return c;
        }

        c = getClassFor(className);
        if (c != null) {
            checkModifiers(c.getModifiers());
            classMap.put(className, c);
        }

        return c;
    }

    private Class<?> getClassFor(String className) {
        if (!notAClass.contains(className)) {
            try {
                return Class.forName(className, false, Thread.currentThread().getContextClassLoader());
                // Some operating systems have case-insensitive path names. An example is Windows if className is
                // attempting to be resolved from a wildcard import a java.lang.NoClassDefFoundError may be thrown as
                // the expected case for the type likely doesn't match. See 
                // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=8024775 and 
                // https://bugs.openjdk.java.net/browse/JDK-8133522.
            } catch (ClassNotFoundException | NoClassDefFoundError ex) {
                notAClass.add(className);
            }
        }

        return null;
    }

    private void checkModifiers(int modifiers) {
        if (isAbstract(modifiers) || isInterface(modifiers) || !isPublic((modifiers))) {
            throw new ELException("Imported class must be public, and cannot be abstract or an interface");
        }
    }
}
