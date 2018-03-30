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
package com.ibm.ws.anno.test.cases;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.anno.info.internal.InfoStoreFactoryImpl;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Service;
import com.ibm.ws.anno.targets.internal.AnnotationTargetsImpl_Factory;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.info.AnnotationInfo;
import com.ibm.wsspi.anno.info.AnnotationValue;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.info.InfoStore;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;

/**
 * Info tests which use locally the locally compiled test data.
 * 
 * These tests are distinguished from other tests which use pre-packaged
 * data.
 */
public class RepeatableJava8Test {
	/**
	 * The build directory for test classes.  These info store tests
	 * use class resources which are generated at build time.
	 */
    private static String testClassesDir;
    
    /** A class store for the test classes directory. */
    private static ClassSource_Aggregate testClassSource;
    
    /** Targets for the test class store. */
    private static AnnotationTargets_Targets testAnnotationTargets;
    
    /** An info store for the test class store. */
    private static InfoStore testInfoStore;

    /**
     * Prepare to run tests.
     * 
     * Set the classes directory from system property "test.classesDir".
     * 
     * Create a class source ({@link ClassSource_Aggregate} on the classes directory.
     * 
     * Create a info store ({@link InfoStore} using the class source.
     */
    @BeforeClass
    public static void setup() throws Exception {
        testClassesDir = System.getProperty("test.classesDir", "bin_test");

        AnnotationServiceImpl_Service annoService = new AnnotationServiceImpl_Service();

        ClassSourceImpl_Factory factory = annoService.getClassSourceFactory();
        testClassSource = factory.createAggregateClassSource("AnnoInfoTest");
        // throws ClassSource_Exception
        factory.addDirectoryClassSource(testClassSource, testClassesDir, testClassesDir, ScanPolicy.SEED);
        // throws ClassSourceException
        factory.addClassLoaderClassSource(testClassSource, "AnnoInfoTest.ClassLoader", RepeatableJava8Test.class.getClassLoader());
        // throws ClassSource_Exception

        AnnotationTargetsImpl_Factory targetsFactory = annoService.getAnnotationTargetsFactory();
        testAnnotationTargets = targetsFactory.createTargets();
        // throws AnnotationTargets_Exception
        testAnnotationTargets.scan(testClassSource);
        // throws AnnotationTargets_Exception

        InfoStoreFactoryImpl infoFactory = annoService.getInfoStoreFactory();
        testInfoStore = infoFactory.createInfoStore(testClassSource);
        // throws InfoStoreException
    }

    // Tests for repeatable annotations:
    //
    // Five target patterns are tested:
    //
    // 0 targets               (null case) (0 element, 0 elements)
    // 1 target using a flat list          (1 element, 0 elements)
    // 1 target using a structured list    (0 element, 1 elements of 1)
    // 3 targets using a flat list         (0 element, 1 elements of 3)
    // 3 targets using a structured list   (0 element, 1 elements of 3)

    // public class RE_Target_None
    //
    // @RE_Element( text = "unique" )
    // public class RE_Target_One_Flat
    //
    // @RE_Elements( {
    //   @RE_Element( text = "singleton" )
    // })
    // public class RE_Target_One_Structured
    //
    // @RE_Element( text = "one" )
    // @RE_Element( text = "two" )
    // @RE_Element( text = "three" )
    // public class RE_Target_Three_Flat
    //
    // @RE_Elements( {
    //     @RE_Element( text = "alpha" ),
    //     @RE_Element( text = "beta" ),
    //     @RE_Element( text = "gamma" )
    // })
    // public class RE_Target_Three_Structured

    private static final String RE_ELEMENT_CLASS =
        com.ibm.ws.anno.test.data.repeat.RE_Element.class.getName();
    private static final String RE_ELEMENT_TEXT_ATTRIBUTE =
        "text";

    private static final String RE_ELEMENTS_CLASS =
        com.ibm.ws.anno.test.data.repeat.RE_Elements.class.getName();
    private static final String RE_ELEMENTS_VALUE_ATTRIBUTE =
        "value";

    private static final String RE_TARGET_NONE_CLASS =
        com.ibm.ws.anno.test.data.repeat.RE_Target_None.class.getName();    
    private static final String RE_TARGET_ONE_FLAT_CLASS =
        com.ibm.ws.anno.test.data.repeat.RE_Target_One_Flat.class.getName();
    private static final String RE_TARGET_ONE_STRUCTURED_CLASS =
        com.ibm.ws.anno.test.data.repeat.RE_Target_One_Structured.class.getName();
    private static final String RE_TARGET_THREE_FLAT_CLASS =
        com.ibm.ws.anno.test.data.repeat.RE_Target_Three_Flat.class.getName();
    private static final String RE_TARGET_THREE_STRUCTURED_CLASS =
        com.ibm.ws.anno.test.data.repeat.RE_Target_Three_Structured.class.getName();

    private static final String ONE_FLAT_TAG =
        "unique";
    private static final String ONE_STRUCTURED_TAG =
        "singleton";
    private static final String[] THREE_FLAT_TAGS =
        new String[] { "one", "two", "three" };
    private static final String[] THREE_STRUCTURED_TAGS =
        new String[] { "alpha", "beta", "gamma" };

    /**
     * Verify that the targets of the test annotations are recorded
     * accurately.
     */
    @Test
    public void testRepeatableTargets() throws Exception {
        Set<String> elementTargets = testAnnotationTargets.getAnnotatedClasses(RE_ELEMENT_CLASS);
        Set<String> elementsTargets = testAnnotationTargets.getAnnotatedClasses(RE_ELEMENTS_CLASS);

        // System.out.println("Targets [ " + RE_ELEMENT_CLASS + " ] [ " + Integer.valueOf(elementTargets.size()) + " ]");
        // System.out.println("Targets [ " + RE_ELEMENT_CLASS + " ] [ " + elementTargets + " ]");
        // for ( String className : elementTargets ) {
        //     System.out.println("  [ " + className+ " ]");
        // }

        // System.out.println("Targets [ " + RE_ELEMENTS_CLASS + " ] [ " + Integer.valueOf(elementsTargets.size()) + " ]");
        // System.out.println("Targets [ " + RE_ELEMENTS_CLASS + " ] [ " + elementsTargets + " ]");        
        // for ( String className : elementsTargets ) {
        //     System.out.println("  [ " + className+ " ]");
        // }

        Assert.assertEquals(
            "Expected targets [ " + RE_ELEMENT_CLASS + " ] [ 1 ]",
            (long) (elementTargets.size()),
            1L);
        Assert.assertTrue(
            "Element target [ " + RE_TARGET_ONE_FLAT_CLASS + " ]",
            elementTargets.contains(RE_TARGET_ONE_FLAT_CLASS));

        Assert.assertEquals(
            "Expected targets [ " + RE_ELEMENTS_CLASS + " ] [ 3 ]",
            (long) (elementsTargets.size()),
            3L);

        Assert.assertTrue(
            "Elements target [ " + RE_TARGET_ONE_STRUCTURED_CLASS + " ]",
            elementsTargets.contains(RE_TARGET_ONE_STRUCTURED_CLASS));
        Assert.assertTrue(
            "Elements target [ " + RE_TARGET_THREE_FLAT_CLASS + " ]",
            elementsTargets.contains(RE_TARGET_THREE_FLAT_CLASS));
        Assert.assertTrue(
            "Elements target [ " + RE_TARGET_THREE_STRUCTURED_CLASS + " ]",
            elementsTargets.contains(RE_TARGET_THREE_STRUCTURED_CLASS));
    }

    /**
     * Verify that {ClassInfo#isRepeatable} is recorded accurately.
     */
    @Test
    public void testRepeatableSetting() throws Exception {
        ClassInfo elementClassInfo = testInfoStore.getDelayableClassInfo(RE_ELEMENT_CLASS);
        Assert.assertNotNull(
            "Loaded [ " + RE_ELEMENT_CLASS + " ]",
            elementClassInfo);
        Assert.assertTrue(
            "Class [ " + RE_ELEMENT_CLASS + " ] is repeatable",
            elementClassInfo.isRepeatable());

        ClassInfo elementsClassInfo = testInfoStore.getDelayableClassInfo(RE_ELEMENTS_CLASS);
        Assert.assertNotNull(
            "Loaded [ " + RE_ELEMENTS_CLASS + " ]",
            elementsClassInfo);
        Assert.assertFalse(
            "Class [ " + RE_ELEMENTS_CLASS + " ] is not repeatable",
            elementsClassInfo.isRepeatable());
    }

    /**
     * Verify that the annotation targets are correctly associated with their
     * target classes.
     * 
     * Five cases are tests:
     * 
     * <ul><li>A class with none of the test annotations.</li>
     *     <li>A class with one of the test annotations placed
     *         directly on the class.</li>
     *     <li>A class with one of the test annotations placed
     *         through an explicit repeatable annotation.</li>
     *     <li>A class with three test annotations placed directly
     *         on the class.</li>
     *     <li>A class with three test annotations placed through
     *         an explicit repeatable annotation.</li>
     * </ul>
     */
    @Test
    public void testRepeatableInfo() throws Exception {
        ClassInfo noneClassInfo = testInfoStore.getDelayableClassInfo(RE_TARGET_NONE_CLASS);
        AnnotationInfo noneElementAnnotation = noneClassInfo.getAnnotation(RE_ELEMENT_CLASS);
        Assert.assertNull(
            "[ " + RE_ELEMENT_CLASS + " ] on [ " + RE_TARGET_NONE_CLASS + " ]",
            noneElementAnnotation);
        AnnotationInfo noneElementsAnnotation = noneClassInfo.getAnnotation(RE_ELEMENTS_CLASS);
        Assert.assertNull(
            "[ " + RE_ELEMENTS_CLASS + " ] on [ " + RE_TARGET_NONE_CLASS + " ]",
            noneElementsAnnotation);

        ClassInfo oneFlatClassInfo = testInfoStore.getDelayableClassInfo(RE_TARGET_ONE_FLAT_CLASS);
        AnnotationInfo oneFlatElementAnnotation = oneFlatClassInfo.getAnnotation(RE_ELEMENT_CLASS);
        Assert.assertNotNull(
            "[ " + RE_ELEMENT_CLASS + " ] on [ " + RE_TARGET_ONE_FLAT_CLASS + " ]",
            oneFlatElementAnnotation);
        String oneFlatTag =
            oneFlatElementAnnotation.getValue(RE_ELEMENT_TEXT_ATTRIBUTE).getStringValue();
        Assert.assertEquals("Expected tag [ " + ONE_FLAT_TAG + " ]",
            oneFlatTag,
            ONE_FLAT_TAG);
        AnnotationInfo oneFlatElementsAnnotation = oneFlatClassInfo.getAnnotation(RE_ELEMENTS_CLASS);
        Assert.assertNull(
            "[ " + RE_ELEMENTS_CLASS + " ] on [ " + RE_TARGET_ONE_FLAT_CLASS + " ]",
            oneFlatElementsAnnotation);

        ClassInfo oneStructuredClassInfo = testInfoStore.getDelayableClassInfo(RE_TARGET_ONE_STRUCTURED_CLASS);
        AnnotationInfo oneStructuredElementAnnotation = oneStructuredClassInfo.getAnnotation(RE_ELEMENT_CLASS);
        Assert.assertNull(
            "[ " + RE_ELEMENT_CLASS + " ] on [ " + RE_TARGET_ONE_STRUCTURED_CLASS + " ]",
            oneStructuredElementAnnotation);
        AnnotationInfo oneStructuredElementsAnnotation = oneStructuredClassInfo.getAnnotation(RE_ELEMENTS_CLASS);
        Assert.assertNotNull(
            "[ " + RE_ELEMENTS_CLASS + " ] on [ " + RE_TARGET_ONE_STRUCTURED_CLASS + " ]",
            oneStructuredElementsAnnotation);
        List<? extends AnnotationValue> oneStructuredValues =
            oneStructuredElementsAnnotation.getArrayValue(RE_ELEMENTS_VALUE_ATTRIBUTE);
        Assert.assertEquals(
            "Expected values [ 1 ]",
            (long) oneStructuredValues.size(),
            1L);
        String oneStructuredTag =
            oneStructuredValues.get(0).getAnnotationValue().getValue(RE_ELEMENT_TEXT_ATTRIBUTE).getStringValue();
        Assert.assertEquals(
            "Expected tag [ " + ONE_STRUCTURED_TAG + " ]",
            oneStructuredTag,
            ONE_STRUCTURED_TAG);

        ClassInfo threeFlatClassInfo = testInfoStore.getDelayableClassInfo(RE_TARGET_THREE_FLAT_CLASS);
        AnnotationInfo threeFlatElementAnnotation = noneClassInfo.getAnnotation(RE_ELEMENT_CLASS);
        Assert.assertNull(
            "[ " + RE_ELEMENT_CLASS + " ] on [ " + RE_TARGET_THREE_FLAT_CLASS + " ]",
            threeFlatElementAnnotation);
        AnnotationInfo threeFlatElementsAnnotation = threeFlatClassInfo.getAnnotation(RE_ELEMENTS_CLASS);
        Assert.assertNotNull(
            "[ " + RE_ELEMENTS_CLASS + " ] on [ " + RE_TARGET_THREE_FLAT_CLASS + " ]",
            threeFlatElementsAnnotation);
        List<? extends AnnotationValue> threeFlatValues =
            threeFlatElementsAnnotation.getArrayValue(RE_ELEMENTS_VALUE_ATTRIBUTE);
        Assert.assertEquals(
            "Expected values [ 3 ]",
            (long) threeFlatValues.size(),
            3L);
        Set<String> threeFlatTags = getRETags(threeFlatValues);
        for ( String tag : THREE_FLAT_TAGS ) {
            Assert.assertTrue(
                "Expected tag [ " + tag + " ]",
                threeFlatTags.contains(tag));
        }

        ClassInfo threeStructuredClassInfo = testInfoStore.getDelayableClassInfo(RE_TARGET_THREE_STRUCTURED_CLASS);
        AnnotationInfo threeStructuredElementAnnotation = noneClassInfo.getAnnotation(RE_ELEMENT_CLASS);
        Assert.assertNull(
            "[ " + RE_ELEMENT_CLASS + " ] on [ " + RE_TARGET_THREE_STRUCTURED_CLASS + " ]",
            threeStructuredElementAnnotation);
        AnnotationInfo threeStructuredElementsAnnotation = threeStructuredClassInfo.getAnnotation(RE_ELEMENTS_CLASS);
        Assert.assertNotNull(
            "[ " + RE_ELEMENTS_CLASS + " ] on [ " + RE_TARGET_THREE_STRUCTURED_CLASS + " ]",
            threeStructuredElementsAnnotation);
        List<? extends AnnotationValue> threeStructuredValues =
            threeStructuredElementsAnnotation.getArrayValue(RE_ELEMENTS_VALUE_ATTRIBUTE);
        Assert.assertEquals(
            "Expected values [ 3 ]",
            (long) threeStructuredValues.size(),
            3L);
        Set<String> threeStructuredTags = getRETags(threeStructuredValues);
        for ( String tag : THREE_STRUCTURED_TAGS ) {
            Assert.assertTrue(
                "Expected tag [ " + tag + " ]",
                threeStructuredTags.contains(tag));
        }
    }

    private static Set<String> getRETags(List<? extends AnnotationValue> reArrayElements) {
        int numElements =  reArrayElements.size();

        Set<String> tags = new HashSet<String>(numElements);

        for ( int elementNo = 0; elementNo < numElements; elementNo++ ) {
            AnnotationInfo elementAsInfo = reArrayElements.get(elementNo).getAnnotationValue();
            String tag = elementAsInfo.getValue(RE_ELEMENT_TEXT_ATTRIBUTE).getStringValue();
            
            tags.add(tag);
        }

        return tags;
    }
}
