/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.annocache.test.scan.basic;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.annocache.classsource.specification.internal.ClassSourceImpl_Specification_Direct_EJB;
import com.ibm.ws.annocache.test.scan.Test_Base;
import com.ibm.wsspi.annocache.info.AnnotationInfo;
import com.ibm.wsspi.annocache.info.AnnotationValue;
import com.ibm.wsspi.annocache.info.ClassInfo;
import com.ibm.wsspi.annocache.info.InfoStore;

/**
 * Test case for @Repeatable annotation (new in Java 8).
 */
public class Test_AnnoRepeatable_Test extends Test_Base {

    @Override
    public ClassSourceImpl_Specification_Direct_EJB createClassSourceSpecification(ClassSourceImpl_Factory factory) {
        return Test_AnnoRepeatable_Data.createClassSourceSpecification(factory);
    }

    //

    @Override
    public String getAppName() {
        return Test_AnnoRepeatable_Data.EAR_NAME;
    }

    @Override
    public String getAppSimpleName() {
        return Test_AnnoRepeatable_Data.EAR_SIMPLE_NAME;
    }

    @Override
    public String getModName() {
        return Test_AnnoRepeatable_Data.EJBJAR_NAME;
    }

    @Override
    public String getModSimpleName() {
        return Test_AnnoRepeatable_Data.EJBJAR_SIMPLE_NAME;
    }

    //

    @Test
    public void testScan() throws Exception {
        runSuiteTest( getBaseCase() ); // 'runSuiteTest' throws Exception
    }

    public static final String REPEATABLE_CLASS_NAME = "java.lang.annotation.Repeatable";

    public static final String AUTHOR_CLASS_NAME = "com.ibm.ws.annocache.test.classes.repeatable.Author";
    public static final String AUTHORS_CLASS_NAME = "com.ibm.ws.annocache.test.classes.repeatable.Authors";

    public static final String BOOK_DIRECT_CLASS_NAME = "com.ibm.ws.annocache.test.classes.repeatable.Book_Direct";
    public static final String BOOK_INDIRECT_CLASS_NAME = "com.ibm.ws.annocache.test.classes.repeatable.Book_Indirect";

    public static final Set<String> EXPECTED_AUTHOR_NAMES_DIRECT;
    static {
    	Set<String> expectedAuthorNames = new HashSet<String>(2);
    	expectedAuthorNames.add("John");
    	expectedAuthorNames.add("George");
    	EXPECTED_AUTHOR_NAMES_DIRECT = expectedAuthorNames;
    }
    
    public static final Set<String> EXPECTED_AUTHOR_NAMES_INDIRECT;
    static {
    	Set<String> expectedAuthorNames = new HashSet<String>(2);
    	expectedAuthorNames.add("Paul");
    	expectedAuthorNames.add("Ringo");
    	EXPECTED_AUTHOR_NAMES_INDIRECT = expectedAuthorNames;
    }

    // @Test // Disabled - requires java8
    public void testRepeatableTarget() throws Exception {
        boolean authorIsRepeatable = getBaseTargets().i_classHasAnnotation(AUTHOR_CLASS_NAME, REPEATABLE_CLASS_NAME);
        Assert.assertTrue("Found [ " + REPEATABLE_CLASS_NAME + " ] on [ " + REPEATABLE_CLASS_NAME + " ]", authorIsRepeatable); 
    }

    @Test
    public void testRepeatableInfo_Direct() throws Exception {
    	testRepeatableInfo(BOOK_DIRECT_CLASS_NAME, EXPECTED_AUTHOR_NAMES_DIRECT);
    }
    
    // @Test // Disabled - requires java8
    public void testRepeatableInfo_Indirect() throws Exception {
    	testRepeatableInfo(BOOK_INDIRECT_CLASS_NAME, EXPECTED_AUTHOR_NAMES_INDIRECT);
    }

    public void testRepeatableInfo(String targetClassName, Set<String> expectedAuthorNames) {
        InfoStore useStore = getBaseStore();

        ClassInfo classInfo = useStore.getDelayableClassInfo(targetClassName);
        Assert.assertNotNull(
        	"Found class [ " + targetClassName + " ]",
        	classInfo);

        AnnotationInfo authorsAnnotation = classInfo.getDeclaredAnnotation(AUTHORS_CLASS_NAME);
        Assert.assertNotNull(
            "Found annotation [ " + AUTHORS_CLASS_NAME + " ] on class [ " + targetClassName + " ]",
            authorsAnnotation);

        List<? extends AnnotationValue> annotationValues = authorsAnnotation.getArrayValue("value");
        Assert.assertNotNull(
            "Found annotation array value [ " + AUTHORS_CLASS_NAME + " ] on class [ " + targetClassName + " ]",
            authorsAnnotation);

        Set<String> actualAuthorNames = new HashSet<String>(2);
        for ( AnnotationValue annotationValue : annotationValues ) {
            AnnotationInfo authorAnnotation = annotationValue.getAnnotationValue();
            AnnotationValue nameValue = authorAnnotation.getValue("name");
            String name = nameValue.getStringValue();
            actualAuthorNames.add(name);
        }

        Assert.assertEquals("Expected author names matches actual author names",
                            expectedAuthorNames, actualAuthorNames);
    }
}
