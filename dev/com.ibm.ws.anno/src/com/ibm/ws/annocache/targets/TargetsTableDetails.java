/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.targets;

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
