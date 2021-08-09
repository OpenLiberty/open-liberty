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

import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.annocache.classsource.ClassSource_Exception;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Aggregate;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.annocache.test.scan.TestOptions_SuiteCase;
import com.ibm.ws.annocache.test.scan.Test_Base;
import com.ibm.ws.annocache.test.utils.TestLocalization;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Options;

public class SCITest_SCIAbsolute_war_Test extends Test_Base {
    public static final String EAR_NAME = SCITest_Data.EAR_NAME;
    public static final String EAR_SIMPLE_NAME = SCITest_Data.EAR_SIMPLE_NAME;
    public static final String WAR_NAME = SCITest_Data.WAR_NAME_ABSOLUTE;
    public static final String WAR_SIMPLE_NAME = SCITest_Data.WAR_SIMPLE_NAME_ABSOLUTE;

    @Override
    public ClassSourceImpl_Aggregate createClassSource(
    	ClassSourceImpl_Factory factory,
    	ClassSourceImpl_Options options) throws ClassSource_Exception {

        String warPath = TestLocalization.putIntoData(EAR_NAME + '/', WAR_NAME) + '/';

        ClassSourceImpl_Aggregate rootClassSource =
            factory.createAggregateClassSource(EAR_SIMPLE_NAME, WAR_SIMPLE_NAME, JAVAEE_MOD_CATEGORY_NAME, options);

        addClassesDirectoryClassSource(rootClassSource, warPath);

        addLibJarClassSources(rootClassSource, warPath,
            ScanPolicy.SEED,
            SCITest_Data.JAR_NAME_SCI4,
            SCITest_Data.JAR_NAME_SCI_WITH_LISTENER);

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
    public void testSCI_SCIAbsolute_BASE() throws Exception {
        runSuiteTest( getBaseCase() ); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testSCI_SCIAbsolute_SINGLE_JANDEX() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.SINGLE_JANDEX); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testSCI_SCIAbsolute_MULTI() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.MULTI); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testSCI_SCIAbsolute_MULTI_JANDEX() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.MULTI_JANDEX); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testSCI_SCIAbsolute_SINGLE_WRITE() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.SINGLE_WRITE); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testSCI_SCIAbsolute_SINGLE_READ() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.SINGLE_READ); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testSCI_SCIAbsolute_MULTI_WRITE() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.MULTI_WRITE); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testSCI_SCIAbsolute_MULTI_READ() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.MULTI_READ); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testSCI_SCIAbsolute_SINGLE_WRITE_ASYNC() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.SINGLE_WRITE_ASYNC); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testSCI_SCIAbsolute_MULTI_WRITE_ASYNC() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.MULTI_WRITE_ASYNC); // 'runSuiteTest' throws Exception
    }
}
