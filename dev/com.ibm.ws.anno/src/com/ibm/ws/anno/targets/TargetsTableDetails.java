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

import java.util.Map;
import java.util.logging.Logger;

public interface TargetsTableDetails {

    String getHashText();

    void log(Logger logger);
    void logConsolidated(Logger logger);

    //

    TargetsTable getParentData();

    // package name -> annotation name -> annotation detail
    // class name -> annotation name -> annotation detail
    // class name -> method name -> annotation name -> annotation detail
    // class name -> field name -> annotation name -> annotation detail

    Map<String, String> i_getPackageAnnotationDetails(String packageName);
    String i_getPackageAnnotationDetail(String packageName, String annotationClassName);

    Map<String, String> i_getClassAnnotationDetails(String i_className);
    String i_getClassAnnotationDetail(String i_className, String i_annotationClassName);

    Map<String, Map<String, String>> i_getMethodAnnotationDetails(String i_className);
    Map<String, String> i_getMethodAnnotationDetails(String i_className, String i_methodSignature);
    String i_getMethodAnnotationDetails(String i_className, String i_methodSignature, String i_annotationClassName);

    Map<String, Map<String, String>> i_getFieldAnnotationDetails(String i_className);
    Map<String, String> i_getFieldAnnotationDetails(String i_className, String i_fieldName);
    String i_getFieldAnnotationDetails(String i_className, String i_fieldName, String i_annotationClassName);

    //

    void i_putPackageAnnotation(String i_packageName, String i_annotationClassName, String annotationDetail);
    void i_putClassAnnotation(String i_className, String i_annotationClassName, String annotationDetail);
    void i_putFieldAnnotation(String i_className, String i_fieldName, String i_annotationClassName, String annotationDetail);
    void i_putMethodAnnotation(String i_className, String i_methodSignature, String i_annotationClassName, String annotationDetail);
}
