/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.targets;

import java.util.List;
import java.util.Set;

public interface TargetsTableClassesMulti extends TargetsTableClasses {

    Set<String> getClassSourceNames();

    Set<String> i_getPackageNames(String classSourceName);
    boolean i_containsPackageName(String classSourceName, String i_packageName);

    Set<String> i_getClassNames(String classSourceName);
    boolean i_containsClassName(String classSourceName, String i_className);

    String i_getClassSourceNameForPackageName(String i_packageName);
    String i_getClassSourceNameForClassName(String i_className);

    //

    Set<String> getClassNames(String classSourceName);
    String getClassSourceNameForPackageName(String packageName);
    String getClassSourceNameForClassName(String className);

    Set<String> getPackageNames(String classSourceName);
    boolean containsPackageName(String classSourceName, String packageName);
    boolean containsClassName(String classSourceName, String className);

    //

    void record(String classSourceName, String i_packageName);

    void record(String classSourceName,
                String i_className,
                String i_superclassName, List<String> i_interfaceNames, int modifiers);
}
