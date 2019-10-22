/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.annocache.test.scan.samples.unused;

import org.junit.Test;

import com.ibm.ws.annocache.classsource.specification.internal.ClassSourceImpl_Specification_Direct_Bundle;
import com.ibm.ws.annocache.test.scan.TestOptions_SuiteCase;
import com.ibm.ws.annocache.test.scan.Test_Base;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;

public class JPATest_JPATest_jar_Test extends Test_Base {

    @Override
    public ClassSourceImpl_Specification_Direct_Bundle createClassSourceSpecification(ClassSourceImpl_Factory factory) {
        return JPATest_JPATest_jar_Data.createClassSourceSpecification(factory);
    }

    //

    @Override
    public String getAppName() {
        return JPATest_JPATest_jar_Data.EBA_NAME;
    }

    @Override
    public String getAppSimpleName() {
        return JPATest_JPATest_jar_Data.EBA_SIMPLE_NAME;
    }

    @Override
    public String getModName() {
        return JPATest_JPATest_jar_Data.EBAJAR_NAME;
    }

    @Override
    public String getModSimpleName() {
        return JPATest_JPATest_jar_Data.EBAJAR_SIMPLE_NAME;
    }

    //

    // @Test
    public void testJPATest_JPATest_jar_BASE() throws Exception {
        runSuiteTest( getBaseCase() ); // 'runSuiteTest' throws Exception
    }

    // @Test
    public void testJPATest_JPATest_jar_SINGLE_JANDEX() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.SINGLE_JANDEX); // 'runSuiteTest' throws Exception
    }

    // @Test
    public void testJPATest_JPATest_jar_MULTI() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.MULTI); // 'runSuiteTest' throws Exception
    }

    // @Test
    public void testJPATest_JPATest_jar_MULTI_JANDEX() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.MULTI_JANDEX); // 'runSuiteTest' throws Exception
    }

    // @Test
    public void testJPATest_JPATest_jar_SINGLE_WRITE() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.SINGLE_WRITE); // 'runSuiteTest' throws Exception
    }

    // @Test
    public void testJPATest_JPATest_jar_SINGLE_READ() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.SINGLE_READ); // 'runSuiteTest' throws Exception
    }

    // @Test
    public void testJPATest_JPATest_jar_MULTI_WRITE() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.MULTI_WRITE); // 'runSuiteTest' throws Exception
    }

    // @Test
    public void testJPATest_JPATest_jar_MULTI_READ() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.MULTI_READ); // 'runSuiteTest' throws Exception
    }

    // @Test
    public void testJPATest_JPATest_jar_SINGLE_WRITE_ASYNC() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.SINGLE_WRITE_ASYNC); // 'runSuiteTest' throws Exception
    }

    // @Test
    public void testJPATest_JPATest_jar_MULTI_WRITE_ASYNC() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.MULTI_WRITE_ASYNC); // 'runSuiteTest' throws Exception
    }
}
