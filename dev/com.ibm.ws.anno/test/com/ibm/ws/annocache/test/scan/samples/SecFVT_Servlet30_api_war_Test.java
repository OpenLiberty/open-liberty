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

import com.ibm.ws.annocache.classsource.specification.internal.ClassSourceImpl_Specification_Direct_WAR;
import com.ibm.ws.annocache.test.scan.TestOptions_SuiteCase;
import com.ibm.ws.annocache.test.scan.Test_Base;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;

public class SecFVT_Servlet30_api_war_Test extends Test_Base {

    @Override
    public ClassSourceImpl_Specification_Direct_WAR createClassSourceSpecification(ClassSourceImpl_Factory factory) {
        return SecFVT_Servlet30_Data.createClassSourceSpecification_WAR(
            factory,
            SecFVT_Servlet30_Data.WAR_API_SIMPLE_NAME,
            SecFVT_Servlet30_Data.WAR_API_NAME);
    }

    //

    @Override
    public String getAppName() {
        return SecFVT_Servlet30_Data.EAR_NAME;
    }

    @Override
    public String getAppSimpleName() {
        return SecFVT_Servlet30_Data.EAR_SIMPLE_NAME;
    }

    @Override
    public String getModName() {
        return SecFVT_Servlet30_Data.WAR_API_NAME;
    }
    
    @Override
    public String getModSimpleName() {
        return SecFVT_Servlet30_Data.WAR_API_SIMPLE_NAME;
    }

    //

    @Test
    public void testSecFVT_Servlet30_api_war_BASE() throws Exception {
        runSuiteTest( getBaseCase() ); // 'runSuiteTest' throws Exception
    }
    

    @Test
    public void testSecFVT_Servlet30_api_SINGLE_JANDEX() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.SINGLE_JANDEX); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testSecFVT_Servlet30_api_MULTI() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.MULTI); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testSecFVT_Servlet30_api_MULTI_JANDEX() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.MULTI_JANDEX); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testSecFVT_Servlet30_api_SINGLE_WRITE() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.SINGLE_WRITE); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testSecFVT_Servlet30_api_SINGLE_READ() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.SINGLE_READ); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testSecFVT_Servlet30_api_MULTI_WRITE() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.MULTI_WRITE); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testSecFVT_Servlet30_api_MULTI_READ() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.MULTI_READ); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testSecFVT_Servlet30_api_SINGLE_WRITE_ASYNC() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.SINGLE_WRITE_ASYNC); // 'runSuiteTest' throws Exception
    }

    @Test
    public void testSecFVT_Servlet30_api_MULTI_WRITE_ASYNC() throws Exception {
        runSuiteTest(TestOptions_SuiteCase.MULTI_WRITE_ASYNC); // 'runSuiteTest' throws Exception
    }
}
