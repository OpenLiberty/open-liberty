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
package com.ibm.ws.microprofile.metrics.classloader.utility;

import java.lang.ref.WeakReference;

public class ClassLoaderUtils {

    private static ClassLoaderUtils instance = new ClassLoaderUtils();
    private WeakReference<ClassLoader> appClassLoaderWeakRef;
    private String appClassLoaderRecordedValue = null;

    public static ClassLoaderUtils getInstance() {
        return instance;
    }

    public WeakReference<ClassLoader> getClassLoaderWeakRef() {
        return appClassLoaderWeakRef;
    }

    /**
     * Set a weakly referenced ClassLoader to this class
     *
     * @param classLoader
     *            weakly referenced ClassLoader
     */
    public void setClassLoaderWeakRef(WeakReference<ClassLoader> classLoader) {
        if (classLoader == null)
            return;
        appClassLoaderWeakRef = classLoader;
        String classLoaderString = (classLoader.get() == null) ? null : classLoader.get().toString();
        appClassLoaderRecordedValue = classLoaderString;
    }

    /**
     * Method to retrieve and evaluate current state of the weakly-referenced
     * ClassLoader and return as human readable format
     *
     * @return String array of size 3: [1] WeakRefence's Ref [2] ClassLoader's
     *         StringVal [3] ClassLoader's current value
     */
    public String resolveClassLoaderStateToString() {
        if (appClassLoaderWeakRef == null)
            return "ClassLoader value was never set";

        //Evaluate/Resolve actual reference of ClassLoader
        String classLoaderString = (appClassLoaderWeakRef.get() == null) ? null : appClassLoaderWeakRef.get().toString();

        return "Application ClassLoader WeakRefernce: " + appClassLoaderWeakRef.toString()
               + "\nApplication ClassLoader's recorded String value: " + appClassLoaderRecordedValue
               + "\nApplication ClassLoader's current resolved value: " + classLoaderString
               + "\n";
    }

    /**
     *
     * @return returns value from resolving the classloader weak reference
     */
    public String resolveClassLoaderState() {
        if (appClassLoaderWeakRef == null)
            return "N/A";
        return (appClassLoaderWeakRef.get() == null) ? null : appClassLoaderWeakRef.get().toString();
    }

    /**
     * Method to retrieve and evaluate current state of the weakly-referenced
     * ClassLoader and return as String[] of the WeakReference, last-stored string
     * value of a resolvable classloader, and the current value of classloader
     *
     * @return String array of size 3: [0] WeakRefence's Ref [1] ClassLoader's
     *         StringVal [2] ClassLoader's current value will return all nulls if
     *         weakreference is null
     */
    public String[] resolveClassLoaderStateToArray() {
        if (appClassLoaderWeakRef == null)
            return new String[] { null, null, null };

        //Evaluate/Resolve actual reference of ClassLoader
        String classLoaderString = (appClassLoaderWeakRef.get() == null) ? null : appClassLoaderWeakRef.get().toString();
        return new String[] { appClassLoaderWeakRef.toString(), appClassLoaderRecordedValue, classLoaderString };
    }

}
