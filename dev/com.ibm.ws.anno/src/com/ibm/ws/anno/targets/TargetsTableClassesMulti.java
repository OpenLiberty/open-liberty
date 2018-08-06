/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.targets;

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
                String i_superclassName, String[] i_interfaceNames, int modifiers);
}