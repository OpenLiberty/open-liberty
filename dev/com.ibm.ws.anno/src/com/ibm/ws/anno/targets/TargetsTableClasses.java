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
import java.util.logging.Logger;

public interface TargetsTableClasses {

    String getHashText();
    void log(Logger logger);

    //

    String getClassSourceName();

    //

    Set<String> i_getPackageNames();
    boolean i_containsPackageName(String i_packageName);

    //

    Set<String> i_getClassNames();
    boolean i_containsClassName(String i_className);
    String i_getSuperclassName(String i_subclassName);
    String[] i_getInterfaceNames(String i_classOrInterfaceName);

    //

    Set<String> i_getAllImplementorsOf(String i_interfaceName);
    Set<String> i_getSubclassNames(String i_superclassName);
    boolean i_isInstanceOf(String i_candidateClassName, String i_targetName, boolean targetIsInterface);

    //

    String internClassName(String className);
    void record(String i_packageName);
    void record(String i_className, String i_superclassName, String[] i_interfaceNames, int modifiers);

    //

    Set<String> getPackageNames();
    boolean containsPackageName(String packageName);

    Set<String> getClassNames();
    boolean containsClassName(String className);

    String getSuperclassName(String subclassName);
    String[] getInterfaceNames(String classOrInterfaceName);

    Set<String> getSubclassNames(String superclassName);

    Integer getModifiers(String classOrInterfaceName);
    int getModifiersValue(String classOrInterfaceName);

    Integer i_getModifiers(String i_classOrInterfaceName);
    int i_getModifiersValue(String i_classOrInterfaceName);
}
