/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
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
import java.util.Set;

import javax.annotation.Resource;
import javax.persistence.Id;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.anno.targets.internal.AnnotationTargetsImpl_Factory;
import com.ibm.ws.anno.targets.internal.AnnotationTargetsImpl_Targets;
import com.ibm.ws.anno.test.data.BClass;
import com.ibm.ws.anno.test.data.CIntf;
import com.ibm.ws.anno.test.data.DerivedBase;
import com.ibm.ws.anno.test.data.DerivedNoInherit;
import com.ibm.ws.anno.test.data.sub.BaseNoInheritAnno;
import com.ibm.ws.anno.test.data.sub.InheritAnno;
import com.ibm.ws.anno.test.data.sub.SubBase;
import com.ibm.ws.anno.util.internal.UtilImpl_Factory;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Exception;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Factory;

import junit.framework.Assert;
import test.common.SharedOutputManager;

/**
 *
 */
public class AnnotationTargetsTest {
    static AnnotationTargetsImpl_Targets targets;

    SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all").logTo(TestConstants.BUILD_LOGS + this.getClass().getSimpleName());

    @Rule
    public TestRule outputRule = outputMgr;

    @BeforeClass
    public static void setup() throws ClassSource_Exception, AnnotationTargets_Exception {

        UtilImpl_Factory utilFactory = new UtilImpl_Factory();
        ClassSourceImpl_Factory factory = new ClassSourceImpl_Factory(utilFactory);

        ClassSource_Aggregate classSource = factory.createAggregateClassSource("AnnoInfoTest");

        String testClassesDir = System.getProperty("test.classesDir", "bin");
        factory.addDirectoryClassSource(classSource, testClassesDir, testClassesDir, ScanPolicy.SEED);

        //ClassSource test = factory.createDirectoryClassSource(classSource, "test", "test2");

        //ClassSource test = factory.createContainerClassSource(classSource, "testSource", null);

        AnnotationTargets_Factory annoFactory = new AnnotationTargetsImpl_Factory(utilFactory, factory);
        targets = (AnnotationTargetsImpl_Targets) annoFactory.createTargets();
        targets.scan(classSource, true);
    }

    static String getClassName(Class<?> clazz) {
        return targets.internClassName(clazz.getName(), false);
    }

    @Test
    public void testOverrideWithAnnotation() {
        String bClassName = getClassName(BClass.class);
        String annoName = Resource.class.getName();

        Set<String> classes = filter( targets.getClassesWithMethodAnnotation(annoName) );
        Assert.assertEquals(1, classes.size());
        Assert.assertTrue(toString(classes), classes.contains(bClassName));
    }

    private static final String ANNO_PREFIX = "com.ibm.ws.anno.";

    private Set<String> filter(Set<String> classNames) {
        Set<String> filteredClassNames = new HashSet<String>(classNames.size());
        for ( String className : classNames ) {
            if ( className.startsWith(ANNO_PREFIX) ) {
                filteredClassNames.add(className);
            }
        }
        return filteredClassNames;
    }

    private String toString(Set<String> classes) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (String clazz : classes) {
            sb.append(clazz);
            sb.append(", ");
        }
        sb.append(']');

        return sb.toString();
    }

    @Test
    public void testClassAnnotations() {
        String subClassName = getClassName(SubBase.class);
        String bClassName = getClassName(BClass.class);
        String derivedClassName = getClassName(DerivedBase.class);
        String inheritAnno = InheritAnno.class.getName();

        Set<String> classes = targets.getAnnotatedClasses(inheritAnno);
        Assert.assertEquals(1, classes.size());
        Assert.assertTrue(toString(classes), classes.contains(subClassName));

        classes = targets.getAllInheritedAnnotatedClasses(inheritAnno);
        Assert.assertEquals(toString(classes), 3, classes.size());
        Assert.assertTrue(classes.contains(subClassName));
        Assert.assertTrue(classes.contains(bClassName));
        Assert.assertTrue(classes.contains(derivedClassName));

    }

    @Test
    public void testMethodAnnotations() {
        String subClassName = getClassName(SubBase.class);
        String bClassName = getClassName(BClass.class);
        String testAnno = Test.class.getName();
        String resourceAnno = Resource.class.getName();

        Set<String> classes = filter( targets.getClassesWithMethodAnnotation(testAnno) );
        Assert.assertEquals(toString(classes), 31, classes.size());
        Assert.assertTrue(classes.contains(subClassName));

        classes = filter( targets.getClassesWithMethodAnnotation(resourceAnno) );
        Assert.assertEquals(1, classes.size());
        Assert.assertTrue(classes.contains(bClassName));
    }

    @Test
    public void testFieldAnnotations() {
        String subClassName = getClassName(SubBase.class);

        String idAnno = Id.class.getName();

        Set<String> classes = filter( targets.getClassesWithFieldAnnotation(idAnno) );
        Assert.assertEquals(1, classes.size());
        Assert.assertTrue(toString(classes), classes.contains(subClassName));
    }

    @Test
    public void testNoInheritAnnos() {
        String derivedNoInherit = getClassName(DerivedNoInherit.class);
        String resourceAnno = Resource.class.getName();
        Set<String> classes = filter( targets.getAllInheritedAnnotatedClasses(resourceAnno) );
        Assert.assertEquals(toString(classes), 3, classes.size());
        Assert.assertTrue(toString(classes), classes.contains(derivedNoInherit));
    }

    @Test
    public void testInstanceOf() {
        String bClassName = getClassName(BClass.class);
        Assert.assertTrue(targets.isInstanceOf(bClassName, SubBase.class));
        Assert.assertTrue(targets.isInstanceOf(bClassName, CIntf.class));
        Assert.assertFalse(targets.isInstanceOf(bClassName, BaseNoInheritAnno.class));
    }
}
