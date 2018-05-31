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
import java.util.ArrayList;

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
     * @return human readable format of current state of ClassLoader held in
     *         this class 1: WeakReference's reference 2: Last Recorded classloader
     *         (string) 3: Resolved value of WeakReference
     */
    public String resolveClassLoaderStateToString() {
        if (appClassLoaderWeakRef == null)
            return "ClassLoader value was never set";
        requestGC();
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
        requestGC();
        return (appClassLoaderWeakRef.get() == null) ? null : appClassLoaderWeakRef.get().toString();
    }

    /**
     * Method to retrieve and evaluate current state of the weakly-referenced
     * ClassLoader and return as String[] of the WeakReference, last-stored string
     * value of a resolvable classloader and the current value of classloader (resolved from Weakreference)
     *
     * @return String array of size 3: [0] WeakRefence's Ref [1] ClassLoader's
     *         StringVal [2] ClassLoader's current value will return all nulls if
     *         weakreference is null
     */
    public String[] resolveClassLoaderStateToArray() {
        if (appClassLoaderWeakRef == null)
            return new String[] { null, null, null };
        requestGC();
        //Evaluate/Resolve actual reference of ClassLoader
        String classLoaderString = (appClassLoaderWeakRef.get() == null) ? null : appClassLoaderWeakRef.get().toString();
        return new String[] { appClassLoaderWeakRef.toString(), appClassLoaderRecordedValue, classLoaderString };
    }

    /**
     * Method to try and force a GC
     *
     * 1:
     * - Creates a string of size 4*(2^7)
     * - Adds string 2001 times into an Arraylist
     *
     * 2: Repeatedly creates 2001 WeakReferences using same obj + WR ref
     *
     * 3: Nulls everything
     *
     * 4: Requests System to GC
     */
    private void requestGC() {

        Object obj;

        StringBuffer sb = new StringBuffer();
        sb.append("asdf");
        for (int i = 0; i <= 7; i++) {
            sb.append(sb.toString() + sb.toString());
        }
        String garbage = sb.toString();

        ArrayList<String> garbageList = new ArrayList<String>();

        WeakReference<Object> weakObj;
        for (int i = 0; i <= 2000; i++) {
            obj = new Object();
            weakObj = new WeakReference<Object>(obj);
            obj = null;
            garbageList.add((garbage + Integer.toString(i)));
        }

        obj = null;
        sb = null;
        weakObj = null;
        garbage = null;
        garbageList = null;

        System.gc();
        Runtime.getRuntime().gc();
        System.runFinalization();
    }

}
