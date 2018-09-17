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

import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets.AnnotationCategory;
import com.ibm.wsspi.anno.util.Util_BidirectionalMap;

public interface TargetsTableAnnotations {
    String getHashText();

    void log(Logger logger);

    //

    Set<String> i_getAnnotatedTargets(AnnotationCategory category);
    Set<String> i_getAnnotatedTargets(AnnotationCategory category, String i_annotationName);
    Set<String> i_getAnnotationNames(AnnotationCategory category);
    Set<String> i_getAnnotations(AnnotationCategory category, String i_classOrPackageName);

    Set<String> getAnnotatedTargets(AnnotationCategory category);
    Set<String> getAnnotatedTargets(AnnotationCategory category, String annotationName);
    Set<String> getAnnotations(AnnotationCategory category);
    Set<String> getAnnotations(AnnotationCategory category, String classOrPackageName);

    //

    Util_BidirectionalMap i_getAnnotations(AnnotationCategory category);

    //

    Util_BidirectionalMap i_getPackageAnnotations();
    Util_BidirectionalMap i_getClassAnnotations();
    Util_BidirectionalMap i_getFieldAnnotations();
    Util_BidirectionalMap i_getMethodAnnotations();

    //

    Set<String> i_getPackagesWithAnnotations();
    Set<String> i_getPackagesWithAnnotation(String i_annotationName);
    Set<String> i_getPackageAnnotationNames();
    Set<String> i_getPackageAnnotations(String i_packageName);

    Set<String> i_getClassesWithClassAnnotations();
    Set<String> i_getClassesWithClassAnnotation(String i_annotationName);
    Set<String> i_getClassAnnotationNames();
    Set<String> i_getClassAnnotations(String i_className);

    Set<String> i_getClassesWithFieldAnnotations();
    Set<String> i_getClassesWithFieldAnnotation(String i_annotationName);
    Set<String> i_getFieldAnnotationNames();
    Set<String> i_getFieldAnnotations(String i_className);

    Set<String> i_getClassesWithMethodAnnotations();
    Set<String> i_getClassesWithMethodAnnotation(String i_annotationName);
    Set<String> i_getMethodAnnotationNames();
    Set<String> i_getMethodAnnotations(String i_className);

    //

    Set<String> getPackagesWithAnnotations();
    Set<String> getPackagesWithAnnotation(String annotationName);
    Set<String> getPackageAnnotations();
    Set<String> getPackageAnnotations(String packageName);

    Set<String> getClassesWithClassAnnotations();
    Set<String> getClassesWithClassAnnotation(String annotationName);
    Set<String> getClassAnnotations();
    Set<String> getClassAnnotations(String className);

    Set<String> getClassesWithFieldAnnotations();
    Set<String> getClassesWithFieldAnnotation(String annotationName);
    Set<String> getFieldAnnotations();
    Set<String> getFieldAnnotations(String className);

    Set<String> getClassesWithMethodAnnotations();
    Set<String> getClassesWithMethodAnnotation(String annotationName);
    Set<String> getMethodAnnotations();
    Set<String> getMethodAnnotations(String className);

    //

    String internClassName(String className, boolean doForce);
    String internClassName(String className);

    void recordPackageAnnotation(String i_packageName, String i_annotationClassName);
    void recordClassAnnotation(String i_className, String i_annotationClassName);
    void recordFieldAnnotation(String i_className, String i_annotationClassName);
    void recordMethodAnnotation(String i_className, String i_annotationClassName);
}
