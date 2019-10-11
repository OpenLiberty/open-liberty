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
import java.util.Set;

import javax.annotation.Resource;
import javax.persistence.Id;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.annocache.classsource.specification.internal.ClassSourceImpl_Specification_Direct_EJB;
import com.ibm.ws.annocache.test.classes.basic.AnnoInherited;
import com.ibm.ws.annocache.test.classes.basic.TargetInherited;
import com.ibm.ws.annocache.test.classes.basic.TargetNonInherited;
import com.ibm.ws.annocache.test.classes.basic.TargetSubInherited_1;
import com.ibm.ws.annocache.test.classes.basic.TargetSubInherited_2;
import com.ibm.ws.annocache.test.classes.basic.TargetSubNonInherited;
import com.ibm.ws.annocache.test.classes.basic.TestInterface;
import com.ibm.ws.annocache.test.scan.Test_Base;

public class Test_AnnoBasic_Test extends Test_Base {

    @Override
    public ClassSourceImpl_Specification_Direct_EJB createClassSourceSpecification(
        ClassSourceImpl_Factory classSourceFactory) {
        return Test_AnnoBasic_Data.createClassSourceSpecification(classSourceFactory);
    }

    //

    @Override
    public String getAppName() {
        return Test_AnnoBasic_Data.EAR_NAME;
    }

    @Override
    public String getAppSimpleName() {
        return Test_AnnoBasic_Data.EAR_SIMPLE_NAME;
    }

    @Override
    public String getModName() {
        return Test_AnnoBasic_Data.EJBJAR_NAME;
    }

    @Override
    public String getModSimpleName() {
        return Test_AnnoBasic_Data.EJBJAR_SIMPLE_NAME;
    }

    //

    @Test
    public void testScan() throws Exception {
        runSuiteTest( getBaseCase() ); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testOverrideWithAnnotation() {
        String resourceAnnoName = Resource.class.getName();

        String i_bClassName = i_getClassName(TargetSubInherited_1.class);

        Set<String> resourceTargetNames = filter( getBaseTargets().getClassesWithMethodAnnotation(resourceAnnoName) );
        Assert.assertEquals(resourceTargetNames.size(), 1);
        Assert.assertTrue(toString(resourceTargetNames), resourceTargetNames.contains(i_bClassName));
    }

    @Test
    public void testClassAnnotations() {
        String inheritAnnoName = AnnoInherited.class.getName();

        String i_subClassName = i_getClassName(TargetInherited.class);
        String i_bClassName = i_getClassName(TargetSubInherited_1.class);
        String i_derivedClassName = i_getClassName(TargetSubInherited_2.class);

        Set<String> immediateTargetNames = getBaseTargets().getAnnotatedClasses(inheritAnnoName);
        Assert.assertEquals(immediateTargetNames.size(), 1);
        Assert.assertTrue(toString(immediateTargetNames), immediateTargetNames.contains(i_subClassName));

        Set<String> inheritTargetNames = getBaseTargets().getAllInheritedAnnotatedClasses(inheritAnnoName);
        Assert.assertEquals(toString(inheritTargetNames), 3, inheritTargetNames.size());
        Assert.assertTrue(inheritTargetNames.contains(i_subClassName));
        Assert.assertTrue(inheritTargetNames.contains(i_bClassName));
        Assert.assertTrue(inheritTargetNames.contains(i_derivedClassName));
    }

    private Set<String> select(Set<String> values, String prefix) {
        Set<String> selected = new HashSet<String>();
        for ( String value : values ) {
                if ( value.startsWith(prefix) ) {
                        selected.add(value);
                }
        }
        return selected;
    }

    @Test
    public void testMethodAnnotations() {
        String testAnnoName = Test.class.getName();
        String resourceAnnoName = Resource.class.getName();

        String i_subClassName = i_getClassName(TargetInherited.class);
        String i_bClassName = i_getClassName(TargetSubInherited_1.class);

        Set<String> testTargetNames = getBaseTargets().getClassesWithMethodAnnotation(testAnnoName);
        testTargetNames = select(testTargetNames, "com.ibm.ws.annocache.test.classes.basic");

        Assert.assertEquals(toString(testTargetNames), 1, testTargetNames.size());
        Assert.assertTrue(testTargetNames.contains(i_subClassName));

        Set<String> resourceTargetNames = filter( getBaseTargets().getClassesWithMethodAnnotation(resourceAnnoName) );
        Assert.assertEquals(1, resourceTargetNames.size());
        Assert.assertTrue(resourceTargetNames.contains(i_bClassName));
    }

    @Test
    public void testFieldAnnotations() {
        String idAnnoName = Id.class.getName();

        String i_subClassName = i_getClassName(TargetInherited.class);

        Set<String> idTargetNames = filter( getBaseTargets().getClassesWithFieldAnnotation(idAnnoName) );
        Assert.assertEquals(1, idTargetNames.size());
        Assert.assertTrue(toString(idTargetNames), idTargetNames.contains(i_subClassName));
    }

    @Test
    public void testNoInheritAnnos() {
        String resourceAnnoName = Resource.class.getName();

        String i_derivedNoInherit = i_getClassName(TargetSubNonInherited.class);

        Set<String> resourceTargetNames = filter( getBaseTargets().getAllInheritedAnnotatedClasses(resourceAnnoName) );
        Assert.assertEquals(toString(resourceTargetNames), 3, resourceTargetNames.size());
        Assert.assertTrue(toString(resourceTargetNames), resourceTargetNames.contains(i_derivedNoInherit));
    }

    @Test
    public void testInstanceOf() {
        String i_bClassName = i_getClassName(TargetSubInherited_1.class);
        Assert.assertTrue(getBaseTargets().isInstanceOf(i_bClassName, TargetInherited.class));
        Assert.assertTrue(getBaseTargets().isInstanceOf(i_bClassName, TestInterface.class));
        Assert.assertFalse(getBaseTargets().isInstanceOf(i_bClassName, TargetNonInherited.class));
    }
}
