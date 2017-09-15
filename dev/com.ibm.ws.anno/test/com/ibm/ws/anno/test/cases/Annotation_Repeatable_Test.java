/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.anno.test.cases;

import java.io.PrintWriter;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Direct_EJB;
import com.ibm.ws.anno.targets.internal.AnnotationTargetsImpl_Targets;
import com.ibm.ws.anno.test.data.Annotation_Repeatable_Data;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.info.AnnotationInfo;
import com.ibm.wsspi.anno.info.AnnotationValue;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.info.InfoStore;

/**
 * Test case for @Repeatable annotation (new in Java 8).
 */
public class Annotation_Repeatable_Test extends AnnotationTest_BaseClass {

    @Override
    public ClassSource_Specification_Direct_EJB createClassSourceSpecification() {
        return Annotation_Repeatable_Data.createClassSourceSpecification(getClassSourceFactory(),
                                                                         getProjectPath(), //publish/files/data/anno_tests
                                                                         getDataPath()); // RepeatableAnnotation_Test.unpacked
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp(); // throws Exception

        setDataPath(Annotation_Repeatable_Data.EAR_NAME);
    }

    //

    @Override
    public String getTargetName() {
        return Annotation_Repeatable_Data.EJBJAR_NAME;
    }

    @Override
    public int getIterations() {
        return 5;
    }

    // Flag used to set the baseline results.  When set, the first run will store the
    // annotation targets to the specified storage location.
    //
    // Set this once to compute and store a baseline.  Then copy the baseline into the main
    // publish location, and change the value back to false.

    //    @Override
    //    public boolean getSeedStorage() {
    //        return true;
    //    }

    //

    @Test
    public void testCanScanJava8CompiledClasses() throws Exception {
        runScanTest(DETAIL_IS_NOT_ENABLED,
                    getStoragePath(COMMON_TEMP_STORAGE_PATH), // publish/files/data/anno_tests/temp/SubclassMapTest.ear.unpacked/SubclassMapTest.jar
                    STORAGE_NAME_DETAIL, // annoTargets_detail.txt
                    getSeedStorage(), // false
                    getStoragePath(COMMON_STORAGE_PATH), // publish/files/data/anno_tests/cache/SubclassMapTest.ear.unpacked/SubclassMapTest.jar
                    STORAGE_NAME_DETAIL, // annoTargets_detail.txt
                    new PrintWriter(System.out, true));
    }

    /**
     * Java 8 adds has a new annotation, @Repeatable.
     * Test that we can find it.
     * 
     * @throws Exception
     */
    @Test
    public void testCanDetectRepeatableAnnotation() throws Exception {
        ClassSource_Aggregate classSource = createClassSource(); // throws ClassSource_Exception
        AnnotationTargetsImpl_Targets annotationTargets = createAnnotationTargets(true); // throws AnnotationTargets_Exception

        annotationTargets.scan(classSource); // throws AnnotationTargets_Exception
        InfoStore infoStore = createInfoStore(classSource); // throws InfoStoreException
        infoStore.open(); // throws InfoStoreException

        // **** Author class has an @Repeatable.  Assert that we found it. ****
        //package com.ibm.ws.anno.test.data.repeatable;
        //import java.lang.annotation.Repeatable;
        //@Repeatable(Authors.class)
        //public @interface Author {
        //    String name() default "DEFAULT!";
        //}
        Assert.assertTrue("Did not find @Repeatable on the Author class.",
                          classHasAnnotation(annotationTargets, "com.ibm.ws.anno.test.data.repeatable.Author", "java.lang.annotation.Repeatable"));

        // **** Book class original source ****
        //package com.ibm.ws.anno.test.data.uses.repeatable;
        //import com.ibm.ws.anno.test.data.repeatable.Author;
        //@Author(name = "John")
        //@Author(name = "George")
        //public class Book {
        //}        

        // **** The compiler should convert the original source to: ****
        //@Authors({@com.ibm.ws.anno.test.data.repeatable.Author(name="John"), 
        //          @com.ibm.ws.anno.test.data.repeatable.Author(name="George")})
        //public class Book {
        //}
        ClassInfo classInfo = infoStore.getDelayableClassInfo("com.ibm.ws.anno.test.data.uses.repeatable.Book");
        Assert.assertTrue("Did not find Book class.", classInfo != null);

        AnnotationInfo annotation = classInfo.getDeclaredAnnotation("com.ibm.ws.anno.test.data.repeatable.Authors");
        Assert.assertTrue("Did not find Authors annotation on Book class.", annotation != null);

        List<? extends AnnotationValue> avList = annotation.getArrayValue("value");
        Assert.assertTrue("Could not get value (an array) for Authors anno.", avList != null);

        for (AnnotationValue av : avList) {
            Assert.assertTrue("Did not find Author annotation in Authors annotation on Book class.",
                              (av.getStringValue()).contains("com.ibm.ws.anno.test.data.repeatable.Author"));
        }
    }
}
