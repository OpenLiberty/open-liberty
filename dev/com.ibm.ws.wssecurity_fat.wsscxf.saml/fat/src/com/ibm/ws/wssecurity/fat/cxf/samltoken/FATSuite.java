/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.cxf.samltoken;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.wssecurity.fat.cxf.samltoken.OneServerTests.CxfSAMLAsymSignEnc1ServerTests;
import com.ibm.ws.wssecurity.fat.cxf.samltoken.OneServerTests.CxfSAMLBasic1ServerTests;
import com.ibm.ws.wssecurity.fat.cxf.samltoken.OneServerTests.CxfSAMLSymSignEnc1ServerTests;
import com.ibm.ws.wssecurity.fat.cxf.samltoken.OneServerTests.CxfSAMLWSSTemplates1ServerTests;
import com.ibm.ws.wssecurity.fat.cxf.samltoken.OneServerTests.CxfSSLSAMLBasic1ServerTests;
import com.ibm.ws.wssecurity.fat.cxf.samltoken.TwoServerTests.CxfSAMLAsymSignEnc2ServerTests;
import com.ibm.ws.wssecurity.fat.cxf.samltoken.TwoServerTests.CxfSAMLBasic2ServerTests;
import com.ibm.ws.wssecurity.fat.cxf.samltoken.TwoServerTests.CxfSAMLSymSignEnc2ServerTests;
import com.ibm.ws.wssecurity.fat.cxf.samltoken.TwoServerTests.CxfSAMLWSSTemplates2ServerTests;
import com.ibm.ws.wssecurity.fat.cxf.samltoken.TwoServerTests.CxfSAMLWSSTemplatesWithExternalPolicy2ServerTests;
import com.ibm.ws.wssecurity.fat.cxf.samltoken.TwoServerTests.CxfSSLSAMLBasic2ServerTests;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({

	   //Lite
	   CxfSAMLBasic2ServerTests.class,
       CxfSSLSAMLBasic2ServerTests.class,
      
       //Full
       CxfSAMLSymSignEnc2ServerTests.class,
       CxfSAMLAsymSignEnc2ServerTests.class,
       CxfSAMLWSSTemplates2ServerTests.class,
       CxfSAMLBasic1ServerTests.class,  
       CxfSSLSAMLBasic1ServerTests.class,
       CxfSAMLSymSignEnc1ServerTests.class,
       CxfSAMLAsymSignEnc1ServerTests.class,
       CxfSAMLWSSTemplates1ServerTests.class,
       CxfSAMLWSSTemplatesWithExternalPolicy2ServerTests.class
       
       //Note:
       //In OL, FATSuiteLite.java is no longer used;
       //instead, using FATSuite.java to combine with FULL and LITE
	
       //The first 2 tests are run as LITE FAT bucket where total 3 test cases @test are specified for LITE with @Mode(TestMode.LITE)
	   //It takes about 5 minutes to run the 3 test cases when commenting out the following FULL test classes
       
       //The rest of 9 tests are run as FULL FAT bucket.
       //Please note due to all test classes (LITE and FULL) extend to *cxf.samltoken.common classes, e.g., CxfSAMLBasicTest.java are extended
       //by CxfSAMLBasic2ServerTests.class (LITE) as well as by CxfSAMLBasic1ServerTests.class (FULL), when running the above LITE, 
       //the test result will contain additional test cases run by some of the following FULL test classes.
       //So the total time running LITE can take up to 11 minutes
       
})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

	//The following repeats run Lite only for local testing
    //@ClassRule
    //public static RepeatTests r = RepeatTests.withoutModification().andWith(FeatureReplacementAction.EE8_FEATURES().removeFeature("jsp-2.2").removeFeature("jaxws-2.2").removeFeature("servlet-3.1").removeFeature("usr:wsseccbh-1.0").addFeature("jsp-2.3").addFeature("jaxws-2.3").addFeature("servlet-4.0").addFeature("usr:wsseccbh-2.0"));

    //The following repeats run both Full and Lite buckets.
    //To run Full only single test class locally,  using the command option "-Dfat.test.class.name";
    //To run Full only entire bucket locally, comment out the Lite bucket above
    @ClassRule
    public static RepeatTests r = RepeatTests.with(new EmptyAction().fullFATOnly()).andWith(FeatureReplacementAction.EE8_FEATURES().removeFeature("jsp-2.2").removeFeature("jaxws-2.2").removeFeature("servlet-3.1").removeFeature("usr:wsseccbh-1.0").addFeature("jsp-2.3").addFeature("jaxws-2.3").addFeature("servlet-4.0").addFeature("usr:wsseccbh-2.0"));

}
