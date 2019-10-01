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
package com.ibm.ws.annocache.test.info;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;
import javax.xml.bind.annotation.XmlAttachmentRef;
import javax.xml.bind.annotation.XmlList;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.annocache.test.classes.basic.AnnoInherited;
import com.ibm.ws.annocache.test.classes.basic.AnnoSelf;
import com.ibm.ws.annocache.test.classes.basic.TargetInherited;
import com.ibm.ws.annocache.test.classes.basic.TargetSelf;
import com.ibm.ws.annocache.test.classes.basic.TargetSubInherited_1;
import com.ibm.ws.annocache.test.classes.basic.TargetSubInherited_2;
import com.ibm.ws.annocache.test.classes.basic.TestInterface;
import com.ibm.wsspi.annocache.info.AnnotationInfo;
import com.ibm.wsspi.annocache.info.AnnotationValue;
import com.ibm.wsspi.annocache.info.ClassInfo;
import com.ibm.wsspi.annocache.info.FieldInfo;
import com.ibm.wsspi.annocache.info.MethodInfo;

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

public class InfoStore_Test extends InfoStore_TestBase {
    // 'value' @HttpConstraint
    // 'httpMethodConstraint' []
    public static final Class<? extends Annotation> SERVLET_SECURITY_CLASS =
        javax.servlet.annotation.ServletSecurity.class;

    // 'value' javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic.PERMIT
    // 'transportGuarantee' javax.servlet.annotation.ServletSecurity.TransportGuarantee.NONE
    // 'rolesAllowed' []
    public static final Class<? extends Annotation> HTTP_CONSTRAINT_CLASS =
        javax.servlet.annotation.HttpConstraint.class;

    // 'emptyRoleSemantic' javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic.PERMIT
    // 'transportGuarantee' javax.servlet.annotation.ServletSecurity.TransportGuarantee.NONE
    // 'rolesAllowed' []
    public static final Class<? extends Annotation> HTTP_METHOD_CONSTRAINT_CLASS =
        javax.servlet.annotation.HttpMethodConstraint.class;

    //

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

    //

    protected void validateAnnotationDefaultValue(
        ClassInfo classInfo,
        String methodName,
        String annotationClassName) {

        String targetName = classInfo.getName() + "." + methodName;
        System.out.println("Validating [ " + targetName + " ] ...");

        String testBanner;

        testBanner = "Found method";
        System.out.println(testBanner);

        MethodInfo methodInfo = getDeclaredMethod(classInfo, methodName);
        Assert.assertTrue(testBanner, (methodInfo != null));

        testBanner = "Non-null method default value";
        System.out.println(testBanner);

        AnnotationValue annotationValue = methodInfo.getAnnotationDefaultValue();
        Assert.assertTrue(testBanner, (annotationValue != null));

        testBanner = "Annotation typed method default value";
        System.out.println(testBanner);

        AnnotationInfo annotationInfo = annotationValue.getAnnotationValue();
        Assert.assertTrue(testBanner, (annotationInfo != null));

        testBanner = "Expected value type [ " + annotationClassName + " ] matches actual value type [ " + annotationInfo.getAnnotationClassName() + " ]";
        System.out.println(testBanner);

        Assert.assertTrue(testBanner, annotationClassName.equals(annotationInfo.getAnnotationClassName()));
    }

    protected void validateNullDefaultValue(
        ClassInfo classInfo,
        String methodName) {

        String targetName = classInfo.getName() + "." + methodName;
        System.out.println("Validating null default value [ " + targetName + " ] ...");

        String testBanner;

        testBanner = "Found method";
        System.out.println(testBanner);

        MethodInfo methodInfo = getDeclaredMethod(classInfo, methodName);
        Assert.assertTrue(testBanner, (methodInfo != null));

        testBanner = "Null method default value";
        System.out.println(testBanner);

        AnnotationValue annotationValue = methodInfo.getAnnotationDefaultValue();
        Assert.assertTrue(testBanner, (annotationValue == null));

        System.out.println("Validating null default value [ " + targetName + " ] ... done");
    }

    protected void validateEnumDefaultValue(
        ClassInfo classInfo,
        String methodName,
        String enumClassName,
        String enumLiteralValue) {

        String targetName = classInfo.getName() + "." + methodName;

        System.out.println("Validating enumeration default value [ " + targetName + " ] ...");

        String testBanner;

        testBanner = "Found method";
        System.out.println(testBanner);

        MethodInfo methodInfo = getDeclaredMethod(classInfo, methodName);
        Assert.assertTrue(testBanner, (methodInfo != null));

        testBanner = "Found method default value";
        System.out.println(testBanner);

        AnnotationValue annotationValue = methodInfo.getAnnotationDefaultValue();
        Assert.assertTrue(testBanner, (annotationValue != null));

        testBanner = "Enumeration type default value";
        System.out.println(testBanner);

        String actualEnumClassName = annotationValue.getEnumClassName();
        String actualEnumLiteralValue = annotationValue.getEnumValue();

        testBanner = "Expected class [ " + enumClassName + " ] matches actual class [ " + actualEnumClassName + " ]";
        System.out.println(testBanner);

        Assert.assertTrue(testBanner, enumClassName.equals(actualEnumClassName));

        testBanner = "Expected literal  [ " + enumLiteralValue + " ] matches actual literal [ " + actualEnumLiteralValue + " ]";
        System.out.println(testBanner);

        Assert.assertTrue(testBanner, enumLiteralValue.equals(actualEnumLiteralValue));

        System.out.println("Validating enumeration default value [ " + targetName + " ] ... done");
    }

    protected void validateArrayDefaultValue(
        ClassInfo classInfo,
        String methodName,
        int expectedSize) {

        String targetName = classInfo.getName() + "." + methodName;

        System.out.println("Validating array default value [ " + targetName + " ] ...");

        String testBanner;

        testBanner = "Found method";
        System.out.println(testBanner);

        MethodInfo methodInfo = getDeclaredMethod(classInfo, methodName);
        Assert.assertTrue(testBanner, (methodInfo != null));

        testBanner = "Found method default value";
        System.out.println(testBanner);

        AnnotationValue annotationValue = methodInfo.getAnnotationDefaultValue();
        Assert.assertTrue(testBanner, (annotationValue != null));

        testBanner = "Array type default value";
        System.out.println(testBanner);

        List<? extends AnnotationValue> arrayDefaultValue = annotationValue.getArrayValue();
        Assert.assertTrue(testBanner, (arrayDefaultValue != null));

        testBanner = "Expected size [ " + expectedSize + " ] matches actual size [ " + arrayDefaultValue.size() + " ]";
        System.out.println(testBanner);

        Assert.assertTrue(testBanner, (expectedSize == arrayDefaultValue.size()));

        System.out.println("Validating array default value [ " + targetName + " ] ... done");
    }

    //

    @Test
    public void testRecursiveClassAnnotation() {
        ClassInfo selfAnnoTargetInfo = getClassInfo(TARGET_SELF_NAME);
        Assert.assertNotNull("Loaded [ " + TARGET_SELF_NAME + " ]", selfAnnoTargetInfo);

        ClassInfo selfAnnoInfo = getClassInfo(ANNO_SELF_NAME);
        Assert.assertNotNull("Loaded [ " + ANNO_SELF_NAME + " ]", selfAnnoInfo);
        Assert.assertTrue("Class [ " + ANNO_SELF_NAME + " ] is an annotation class", selfAnnoInfo.isAnnotationClass());

        Assert.assertTrue("Class [ " + TARGET_SELF_NAME + " ] has annotation [ " + ANNO_SELF_NAME + " ]",
                          selfAnnoTargetInfo.isAnnotationPresent(ANNO_SELF_NAME));

        Assert.assertTrue("Class [ " + ANNO_SELF_NAME + " ] has annotation [ " + ANNO_SELF_NAME + " ]",
                          selfAnnoInfo.isAnnotationPresent(ANNO_SELF_NAME));
    }
    
    protected static final String TARGET_SELF_NAME = TargetSelf.class.getName();
    protected static final String ANNO_SELF_NAME = AnnoSelf.class.getName();

    // TargetInherited:
    //   public void publicMethod();
    //   public Number publicMethod(int n);
    //   protected Map<String, String> protectedMethod();
    //   int packageMethod();
	//   private void privateMethod();
    //   protected void annoMethod(int a, String b, List<?> c, long d);

    // TargetSubInherited_1
    //   public void publicMethod();
    //   public Integer publicMethod(int n);
    //   public Integer n();
    //   private void privateMethod();

    // Overlaid:
    //   super public void publicMethod(); [masked]
    //   sub public void publicMethod();
    //   super public Number publicMethod(int n); [masked]
    //   sub public Integer publicMethod(int n);
    //   sub public Integer n();
    //
    //   super protected Map<String, String> protectedMethod();
    //   super int packageMethod();
    //
	//   super private void privateMethod(); [hidden]
    //   sub private void privateMethod();
    //
    //   super protected void annoMethod(int a, String b, List<?> c, long d);

    @Test
    public void testMethods() {
        ClassInfo superInfo = getClassInfo(TargetInherited.class.getName());
        Collection<MethodInfo> superMethods = new ArrayList<MethodInfo>(superInfo.getMethods());
        removeMethod(superMethods, superInfo, "publicMethod", "()V");
        removeMethod(superMethods, superInfo, "publicMethod", "(I)Ljava/lang/Number;");
        removeMethod(superMethods, superInfo, "protectedMethod", "()Ljava/util/Map;");
        removeMethod(superMethods, superInfo, "packageMethod", "()I");
        removeMethod(superMethods, superInfo, "privateMethod", "()V");
        removeMethod(superMethods, superInfo, "annoMethod", "(ILjava/lang/String;Ljava/util/List;J)V");

        for ( MethodInfo methodInfo : superMethods ) {
        	String declaringName = methodInfo.getDeclaringClass().getName();
        	if ( declaringName.equals("java.lang.Object") ) {
        		continue;
        	}
        	Assert.assertEquals("Declaring class is of super method object", declaringName, "java.lang.Object");
        }

        ClassInfo subInfo = getClassInfo(TargetSubInherited_1.class.getName());
        Collection<MethodInfo> subMethods = new ArrayList<MethodInfo>(subInfo.getMethods());
        
        for ( MethodInfo subMethod : subMethods ) {
        	String declaringName = subMethod.getDeclaringClass().getName();
        	if ( declaringName.equals("java.lang.Object") ) {
        		continue;
        	}
        	System.out.println("Initial Sub [ " + subMethod + " ]");
        }

        // removeMethod(subMethods, superInfo,   "publicMethod",    "()V"); [masked]
        removeMethod(subMethods, subInfo,   "publicMethod",    "()V");
        removeMethod(subMethods, subInfo, "publicMethod",    "(I)Ljava/lang/Number;"); // Bridge to superclass method
        removeMethod(subMethods, subInfo,   "publicMethod",    "(I)Ljava/lang/Integer;");
        removeMethod(subMethods, subInfo,   "n",               "()Ljava/lang/Integer;");
        removeMethod(subMethods, subInfo,   "n",               "()Ljava/lang/Number;"); // Bridge to interface method
        removeMethod(subMethods, superInfo, "protectedMethod", "()Ljava/util/Map;");
        removeMethod(subMethods, superInfo, "packageMethod",   "()I");
        // removeMethod(subMethods, superInfo, "privateMethod",   "()V"); [hidden]
        removeMethod(subMethods, subInfo,   "privateMethod",   "()V");
        removeMethod(subMethods, superInfo, "annoMethod",      "(ILjava/lang/String;Ljava/util/List;J)V");

        //   super public void publicMethod(); [masked]
        //   sub public void publicMethod();
        //   super public Number publicMethod(int n); [masked, but re-added as bridge]
        //   sub public Integer publicMethod(int n);
        //   interface  public Number n();
        //   sub public Integer n();
        //
        //   super protected Map<String, String> protectedMethod();
        //   super int packageMethod();
        //
    	//   super private void privateMethod(); [hidden]
        //   sub private void privateMethod();
        //
        //   super protected void annoMethod(int a, String b, List<?> c, long d);

        for ( MethodInfo subMethod : subMethods ) {
        	String declaringName = subMethod.getDeclaringClass().getName();
        	if ( declaringName.equals("java.lang.Object") ) {
        		continue;
        	}
        	System.out.println("Final Sub [ " + subMethod + " ]");
        }

        for ( MethodInfo methodInfo : subMethods ) {
        	String declaringName = methodInfo.getDeclaringClass().getName();
        	Assert.assertEquals("Incorrect declaring class of sub method [ " + methodInfo + " ]",
        		"java.lang.Object", declaringName);
        }
    }

    @Test
    public void testPrivateMethods() {
        ClassInfo superInfo = getClassInfo(TargetInherited.class.getName());
        Assert.assertNotNull("Located super [ " + TargetInherited.class + " ]", superInfo);

        List<MethodInfo> superPrivateMethods = getMethods(superInfo.getMethods(), "privateMethod");
        Assert.assertEquals("Found [ 1 ] private method on [ " + superInfo.getName() + " ]",
            1, superPrivateMethods.size());

        for ( MethodInfo privateMethod : superPrivateMethods ) {
            Assert.assertEquals("Method [ " + privateMethod + " ] has declaring class  [ " + superInfo + " ]",
            	privateMethod.getDeclaringClass().getName(), superInfo.getName()); 
        }

        ClassInfo subInfo = getClassInfo(TargetSubInherited_1.class.getName());
        Assert.assertNotNull("Located sub [ " + TargetSubInherited_1.class + " ]", subInfo);

        Assert.assertEquals("Sub [ " + subInfo + " ] has super [ " + superInfo + " ]",
        	subInfo.getSuperclass().getName(), superInfo.getName()); 

        List<MethodInfo> subPrivateMethods = getMethods(subInfo.getMethods(), "privateMethod");
        Assert.assertEquals(1, subPrivateMethods.size());

        Assert.assertEquals("Found [ 1 ] private method on [ " + subInfo.getName() + " ]",
        	1, subPrivateMethods.size());

        for ( MethodInfo privateMethod : subPrivateMethods ) {
            Assert.assertEquals("Method [ " + privateMethod + " ] has declaring class  [ " + subInfo + " ]",
            	privateMethod.getDeclaringClass().getName(), subInfo.getName()); 
        }
    }

    @Test
    public void testParameterAndAnnoOrder() {
        ClassInfo subInfo = getClassInfo(TargetInherited.class.getName());
        List<MethodInfo> methodInfos = getMethods(subInfo.getMethods(), "annoMethod");
        Assert.assertEquals(1, methodInfos.size());

        MethodInfo mi = methodInfos.get(0);
        Collection<String> parmTypes = mi.getParameterTypeNames();
        Assert.assertEquals(4, parmTypes.size());
        Assert.assertEquals(Arrays.asList("int", String.class.getName(), List.class.getName(), "long"), parmTypes);

        @SuppressWarnings("deprecation")
        List<List<? extends com.ibm.wsspi.anno.info.AnnotationInfo>> parameterAnnos = mi.getParameterAnnotations();
        Assert.assertEquals(4, parameterAnnos.size());

        List<? extends List<? extends AnnotationInfo>> parmAnnos = mi.getParmAnnotations();
        Assert.assertEquals(4, parmAnnos.size());

        Assert.assertEquals(0, parmAnnos.get(0).size());
        Assert.assertEquals(0, parmAnnos.get(1).size());

        List<? extends AnnotationInfo> annos = parmAnnos.get(2);
        Assert.assertEquals(1, annos.size());
        Assert.assertEquals(XmlList.class.getName(), annos.get(0).getAnnotationClassName());

        annos = parmAnnos.get(3);
        Assert.assertEquals(1, annos.size());
        Assert.assertEquals(XmlAttachmentRef.class.getName(), annos.get(0).getAnnotationClassName());

    }

    @Test
    public void testOverrideWithAnnotation() {
        ClassInfo cInfo = getClassInfo(TestInterface.class.getName());
        ClassInfo bInfo = getClassInfo(TargetSubInherited_1.class.getName());
        ClassInfo integerClassInfo = getClassInfo(Integer.class.getName());
        Assert.assertNotNull("CIntf", cInfo);
        Assert.assertNotNull("BClass", bInfo);
        Assert.assertNotNull("Integer", integerClassInfo);

        List<MethodInfo> methodInfos = getMethods(bInfo.getMethods(), "n");
        Assert.assertEquals(2, methodInfos.size());

        MethodInfo nMethod = methodInfos.get(0);
        Assert.assertEquals(bInfo.getQualifiedName(), nMethod.getDeclaringClass().getQualifiedName());
        Assert.assertEquals(integerClassInfo, nMethod.getReturnType());
        Assert.assertEquals(1, nMethod.getAnnotations().size());
    }

    @Test
    public void testClassAnnotations() {
        ClassInfo info = getClassInfo(TargetInherited.class.getName());
        Assert.assertEquals(1, info.getDeclaredAnnotations().size());
        Assert.assertEquals(1, info.getAnnotations().size());
        Assert.assertTrue(info.isAnnotationPresent(AnnoInherited.class.getName()));
        Assert.assertTrue(info.getAnnotation(AnnoInherited.class).isInherited());
        Assert.assertFalse(info.isAnnotationPresent("javax.annotation.Resource"));
        AnnotationInfo annoInfo = info.getAnnotation(AnnoInherited.class);
        List<? extends AnnotationValue> values = annoInfo.getArrayValue("value");
        Assert.assertEquals(2, values.size());
        Assert.assertEquals("a", values.get(0).getStringValue());
        Assert.assertEquals("b", values.get(1).getStringValue());

        info = getClassInfo(TargetSubInherited_1.class.getName());
        Assert.assertEquals(1, info.getDeclaredAnnotations().size());
        Assert.assertEquals(2, info.getAnnotations().size());
        Assert.assertTrue(info.isAnnotationPresent(AnnoInherited.class.getName()));
        Assert.assertTrue(info.getAnnotation(AnnoInherited.class).isInherited());
        Assert.assertTrue(info.isAnnotationPresent("javax.annotation.Resource"));

        annoInfo = info.getAnnotation(Resource.class);
        Assert.assertEquals("/B", annoInfo.getValue("name").getStringValue());
        AnnotationValue annoValue = annoInfo.getValue("authenticationType");
        Assert.assertEquals(AuthenticationType.class.getName(), annoValue.getEnumClassName());
        Assert.assertEquals(AuthenticationType.APPLICATION.name(), annoValue.getStringValue());
    }

    @Test
    public void testMethodAnnotations() {
        ClassInfo info = getClassInfo(TargetInherited.class.getName());
        List<? extends MethodInfo> declaredMethods = info.getMethods();
        List<MethodInfo> methods = getMethods(declaredMethods, "publicMethod");
        Assert.assertEquals(2, methods.size());
        methods = getMethods(declaredMethods, "publicMethod", new String[0]);
        Assert.assertEquals(1, methods.size());
        MethodInfo method = methods.get(0);
        Collection<? extends AnnotationInfo> annos = method.getAnnotations();
        Assert.assertEquals(1, annos.size());
        annos = method.getDeclaredAnnotations();
        Assert.assertEquals(1, annos.size());
        Assert.assertTrue(method.isAnnotationPresent("org.junit.Test"));
        AnnotationInfo annoInfo = method.getAnnotation(Test.class);
        Assert.assertEquals(10000, annoInfo.getValue("timeout").getIntValue());

        info = getClassInfo(TargetSubInherited_1.class.getName());
        methods = getMethods(info.getMethods(), "publicMethod");
        Assert.assertEquals(3, methods.size()); // Integer & Number
        method = methods.get(0);
        if (!method.getReturnTypeName().equals("java.lang.Integer")) {
            method = methods.get(1);
        }
        annos = method.getAnnotations();
        Assert.assertEquals(0, annos.size());
        annos = method.getDeclaredAnnotations();
        Assert.assertEquals(0, annos.size());
        Assert.assertFalse(method.isAnnotationPresent("org.junit.Test"));

        // other test above in OverrideWithAnnotation
    }

    @Test
    public void testFieldAnnotations() {
        ClassInfo info = getClassInfo(TargetInherited.class.getName());
        FieldInfo field = getField(info.getDeclaredFields(), "public1");
        Assert.assertNotNull(field);
        Collection<? extends AnnotationInfo> annos = field.getAnnotations();
        Assert.assertEquals(1, annos.size());
        annos = field.getDeclaredAnnotations();
        Assert.assertEquals(1, annos.size());
        Assert.assertTrue(field.isAnnotationPresent("javax.persistence.Id"));

        info = getClassInfo(TargetSubInherited_2.class.getName());
        field = getField(info.getDeclaredFields(), "public1");
        Assert.assertNotNull(field);
        annos = field.getAnnotations();
        Assert.assertEquals(0, annos.size());
        annos = field.getDeclaredAnnotations();
        Assert.assertEquals(0, annos.size());
        Assert.assertFalse(field.isAnnotationPresent("javax.persistence.Id"));
    }

    @Test
    public void testDeclaredFields() {
        ClassInfo subInfo = getClassInfo(TargetInherited.class.getName());
        ClassInfo derivedInfo = getClassInfo(TargetSubInherited_2.class.getName());

        Collection<FieldInfo> subFields = new ArrayList<FieldInfo>(subInfo.getDeclaredFields());
        removeField(subFields, subInfo, "public1");
        removeField(subFields, subInfo, "public2");
        removeField(subFields, subInfo, "protected1");
        removeField(subFields, subInfo, "protected2");
        removeField(subFields, subInfo, "package1");
        removeField(subFields, subInfo, "package2");
        removeField(subFields, subInfo, "private1");
        Assert.assertEquals(subFields.toString(), 0, subFields.size());

        Collection<FieldInfo> derivedFields = new ArrayList<FieldInfo>(derivedInfo.getDeclaredFields());
        removeField(derivedFields, derivedInfo, "public1");
        removeField(derivedFields, derivedInfo, "protected2");
        removeField(derivedFields, derivedInfo, "package1");
        removeField(derivedFields, derivedInfo, "private2");
        Assert.assertEquals(subFields.toString(), 0, derivedFields.size());
    }

    @Test
    public void testDefaultMethod() {
        ClassInfo info = getClassInfo(AnnoInherited.class.getName());
        List<MethodInfo> methods = getMethods(info.getMethods(), "defaultValue");
        Assert.assertEquals(1, methods.size());
        Assert.assertEquals("abc123", methods.get(0).getAnnotationDefaultValue().getStringValue());

        methods = getMethods(info.getMethods(), "value");
        Assert.assertEquals(1, methods.size());
        Assert.assertNull(methods.get(0).getAnnotationDefaultValue());
    }
    
}
