/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.annocache.test.scan.samples;

 import org.junit.Test;

import com.ibm.wsspi.annocache.classsource.ClassSource_Exception;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Aggregate;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.annocache.test.scan.TestOptions_SuiteCase;
import com.ibm.ws.annocache.test.scan.Test_Base;
import com.ibm.ws.annocache.test.utils.TestLocalization;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Options;

public class LoginMethod_StaticAnnotationWebXML_war_Test extends Test_Base {
    public static final String EAR_NAME = LoginMethod_ear_Data.EAR_NAME;
    public static final String EAR_SIMPLE_NAME = LoginMethod_ear_Data.EAR_SIMPLE_NAME;
    public static final String WAR_NAME = LoginMethod_ear_Data.WAR_NAME_STATIC_ANNOTATION_WEB_XML;
    public static final String WAR_SIMPLE_NAME = LoginMethod_ear_Data.WAR_SIMPLE_NAME_STATIC_ANNOTATION_WEB_XML;

    @Override
    public ClassSourceImpl_Aggregate createClassSource(
    	ClassSourceImpl_Factory factory,
    	ClassSourceImpl_Options options) throws ClassSource_Exception {

    	String warPath = TestLocalization.putIntoData(EAR_NAME + '/', WAR_NAME) + '/';

        ClassSourceImpl_Aggregate rootClassSource =
            factory.createAggregateClassSource(EAR_SIMPLE_NAME, WAR_SIMPLE_NAME, JAVAEE_MOD_CATEGORY_NAME, options);
        addClassesDirectoryClassSource(rootClassSource, warPath);
        addClassLoaderClassSource(rootClassSource);

        return rootClassSource;
    }

    //

    @Override
    public String getAppName() {
        return EAR_NAME;
    }

    @Override
    public String getAppSimpleName() {
        return EAR_SIMPLE_NAME;
    }

    @Override
    public String getModName() {
        return WAR_NAME;
    }

    @Override
    public String getModSimpleName() {
        return WAR_SIMPLE_NAME;
    }

    //

    @Test
    public void testLoginMethod_StaticAnnotationWebXML_war() throws Exception {
        runSuiteTest( getBaseCase() ); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testLoginMethod_StaticAnnotationWebXML_war_SINGLE_JANDEX() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.SINGLE_JANDEX); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testLoginMethod_StaticAnnotationWebXML_war_MULTI() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.MULTI); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testLoginMethod_StaticAnnotationWebXML_war_MULTI_JANDEX() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.MULTI_JANDEX); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testLoginMethod_StaticAnnotationWebXML_war_SINGLE_WRITE() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.SINGLE_WRITE); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testLoginMethod_StaticAnnotationWebXML_war_SINGLE_READ() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.SINGLE_READ); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testLoginMethod_StaticAnnotationWebXML_war_MULTI_WRITE() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.MULTI_WRITE); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testLoginMethod_StaticAnnotationWebXML_war_MULTI_READ() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.MULTI_READ); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testLoginMethod_StaticAnnotationWebXML_war_SINGLE_WRITE_ASYNC() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.SINGLE_WRITE_ASYNC); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testLoginMethod_StaticAnnotationWebXML_war_MULTI_WRITE_ASYNC() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.MULTI_WRITE_ASYNC); // 'runSuiteTest' throws Exception
    }
}
