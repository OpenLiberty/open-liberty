/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.anno.test.cases;

import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.util.List;

import org.junit.Test;

import com.ibm.wsspi.anno.info.AnnotationInfo;
import com.ibm.wsspi.anno.info.AnnotationValue;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.info.MethodInfo;

/**
 * Test default value assignment.
 * 
 * Servlet security provides a nice set of cases.
 * 
 * Three tests are currently performed. These tests test default value processing
 * on the three Servlet Security related annotation types, ServletSecurity,
 * HttpConstraint, and HttpMethodConstraint.
 * 
 * <code>
 * void testHttpConstraintDefaults()
 * void testHttpMethodConstraintDefaults()
 * void testServletSecurityDefaults()
 * </code>
 * 
 * These use four general validation methods:
 * 
 * void validateAnnotationDefaultValue(ClassInfo classInfo, String methodName, String annotationClassName)
 * void validateArrayDefaultValue(ClassInfo classInfo, String methodName, int expectedSize)
 * void validateNullDefaultValue(ClassInfo classInfo, String methodName)
 * void validateEnumDefaultValue(ClassInfo classInfo, String methodName, String enumClassName, String enumLiteralValue)
 */

public class AnnotationTest_TestDefaultValues extends AnnotationTest_BaseDirectClass {

    public static final Class<? extends Annotation> SERVLET_SECURITY_CLASS =
                    javax.servlet.annotation.ServletSecurity.class;
    // 'value' @HttpConstraint
    // 'httpMethodConstraint' []

    public static final Class<? extends Annotation> HTTP_CONSTRAINT_CLASS =
                    javax.servlet.annotation.HttpConstraint.class;

    // 'value' javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic.PERMIT
    // 'transportGuarantee' javax.servlet.annotation.ServletSecurity.TransportGuarantee.NONE
    // 'rolesAllowed' []

    public static final Class<? extends Annotation> HTTP_METHOD_CONSTRAINT_CLASS =
                    javax.servlet.annotation.HttpMethodConstraint.class;

    // 'emptyRoleSemantic' javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic.PERMIT
    // 'transportGuarantee' javax.servlet.annotation.ServletSecurity.TransportGuarantee.NONE
    // 'rolesAllowed' []

    @Test
    public void testHttpConstraintDefaults() throws Exception {
        ClassInfo httpConstraintClassInfo = getClassInfo(HTTP_CONSTRAINT_CLASS);

        validateEnumDefaultValue(httpConstraintClassInfo, "value",
                                 "javax.servlet.annotation.ServletSecurity$EmptyRoleSemantic", "PERMIT");
        validateEnumDefaultValue(httpConstraintClassInfo, "transportGuarantee",
                                 "javax.servlet.annotation.ServletSecurity$TransportGuarantee", "NONE");
        validateArrayDefaultValue(httpConstraintClassInfo, "rolesAllowed", 0);
    }

    @Test
    public void testHttpMethodConstraintDefaults() throws Exception {
        ClassInfo httpMethodConstraintClassInfo = getClassInfo(HTTP_METHOD_CONSTRAINT_CLASS);

        validateNullDefaultValue(httpMethodConstraintClassInfo, "value");
        validateEnumDefaultValue(httpMethodConstraintClassInfo, "emptyRoleSemantic",
                                 "javax.servlet.annotation.ServletSecurity$EmptyRoleSemantic", "PERMIT");
        validateEnumDefaultValue(httpMethodConstraintClassInfo, "transportGuarantee",
                                 "javax.servlet.annotation.ServletSecurity$TransportGuarantee", "NONE");
        validateArrayDefaultValue(httpMethodConstraintClassInfo, "rolesAllowed", 0);
    }

    @Test
    public void testServletSecurityDefaults() throws Exception {
        System.out.println("Testing default values for [ " + SERVLET_SECURITY_CLASS.getName() + " ] ...");

        ClassInfo servletSecurityClassInfo = getClassInfo(SERVLET_SECURITY_CLASS);

        validateArrayDefaultValue(servletSecurityClassInfo, "httpMethodConstraints", 0);
        validateAnnotationDefaultValue(servletSecurityClassInfo, "value", "javax.servlet.annotation.HttpConstraint");
    }

    protected void validateAnnotationDefaultValue(ClassInfo classInfo, String methodName, String annotationClassName) {
        String targetName = classInfo.getName() + "." + methodName;
        System.out.println("Validating [ " + targetName + " ] ...");

        String testBanner;

        testBanner = "Found method";
        System.out.println(testBanner);

        MethodInfo methodInfo = getDeclaredMethod(classInfo, methodName);
        assertTrue(testBanner, (methodInfo != null));

        testBanner = "Non-null method default value";
        System.out.println(testBanner);

        AnnotationValue annotationValue = methodInfo.getAnnotationDefaultValue();
        assertTrue(testBanner, (annotationValue != null));

        testBanner = "Annotation typed method default value";
        System.out.println(testBanner);

        AnnotationInfo annotationInfo = annotationValue.getAnnotationValue();
        assertTrue(testBanner, (annotationInfo != null));

        testBanner = "Expected value type [ " + annotationClassName + " ] matches actual value type [ " + annotationInfo.getAnnotationClassName() + " ]";
        System.out.println(testBanner);

        assertTrue(testBanner, annotationClassName.equals(annotationInfo.getAnnotationClassName()));
    }

    protected void validateNullDefaultValue(ClassInfo classInfo, String methodName) {
        String targetName = classInfo.getName() + "." + methodName;
        System.out.println("Validating null default value [ " + targetName + " ] ...");

        String testBanner;

        testBanner = "Found method";
        System.out.println(testBanner);

        MethodInfo methodInfo = getDeclaredMethod(classInfo, methodName);
        assertTrue(testBanner, (methodInfo != null));

        testBanner = "Null method default value";
        System.out.println(testBanner);

        AnnotationValue annotationValue = methodInfo.getAnnotationDefaultValue();
        assertTrue(testBanner, (annotationValue == null));

        System.out.println("Validating null default value [ " + targetName + " ] ... done");
    }

    protected void validateEnumDefaultValue(ClassInfo classInfo, String methodName,
                                            String enumClassName, String enumLiteralValue) {

        String targetName = classInfo.getName() + "." + methodName;

        System.out.println("Validating enumeration default value [ " + targetName + " ] ...");

        String testBanner;

        testBanner = "Found method";
        System.out.println(testBanner);

        MethodInfo methodInfo = getDeclaredMethod(classInfo, methodName);
        assertTrue(testBanner, (methodInfo != null));

        testBanner = "Found method default value";
        System.out.println(testBanner);

        AnnotationValue annotationValue = methodInfo.getAnnotationDefaultValue();
        assertTrue(testBanner, (annotationValue != null));

        testBanner = "Enumeration type default value";
        System.out.println(testBanner);

        String actualEnumClassName = annotationValue.getEnumClassName();
        String actualEnumLiteralValue = annotationValue.getEnumValue();

        testBanner = "Expected class [ " + enumClassName + " ] matches actual class [ " + actualEnumClassName + " ]";
        System.out.println(testBanner);

        assertTrue(testBanner, enumClassName.equals(actualEnumClassName));

        testBanner = "Expected literal  [ " + enumLiteralValue + " ] matches actual literal [ " + actualEnumLiteralValue + " ]";
        System.out.println(testBanner);

        assertTrue(testBanner, enumLiteralValue.equals(actualEnumLiteralValue));

        System.out.println("Validating enumeration default value [ " + targetName + " ] ... done");
    }

    protected void validateArrayDefaultValue(ClassInfo classInfo, String methodName, int expectedSize) {
        String targetName = classInfo.getName() + "." + methodName;

        System.out.println("Validating array default value [ " + targetName + " ] ...");

        String testBanner;

        testBanner = "Found method";
        System.out.println(testBanner);

        MethodInfo methodInfo = getDeclaredMethod(classInfo, methodName);
        assertTrue(testBanner, (methodInfo != null));

        testBanner = "Found method default value";
        System.out.println(testBanner);

        AnnotationValue annotationValue = methodInfo.getAnnotationDefaultValue();
        assertTrue(testBanner, (annotationValue != null));

        testBanner = "Array type default value";
        System.out.println(testBanner);

        List<? extends AnnotationValue> arrayDefaultValue = annotationValue.getArrayValue();
        assertTrue(testBanner, (arrayDefaultValue != null));

        testBanner = "Expected size [ " + expectedSize + " ] matches actual size [ " + arrayDefaultValue.size() + " ]";
        System.out.println(testBanner);

        assertTrue(testBanner, (expectedSize == arrayDefaultValue.size()));

        System.out.println("Validating array default value [ " + targetName + " ] ... done");
    }

    /**
     * @param classInfo
     * @param methodName
     * @return
     */
    private MethodInfo getDeclaredMethod(ClassInfo classInfo, String methodName) {
        for (MethodInfo m : classInfo.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                return m;
            }
        }

        return null;
    }
}
