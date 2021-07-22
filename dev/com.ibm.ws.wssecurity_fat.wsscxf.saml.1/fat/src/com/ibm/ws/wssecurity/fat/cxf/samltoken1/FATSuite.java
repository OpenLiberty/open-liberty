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

package com.ibm.ws.wssecurity.fat.cxf.samltoken1;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.wssecurity.fat.cxf.samltoken1.OneServerTests.CxfSAMLWSSTemplates1ServerTests;
import com.ibm.ws.wssecurity.fat.cxf.samltoken1.TwoServerTests.CxfSAMLWSSTemplates2ServerTests;
import com.ibm.ws.wssecurity.fat.cxf.samltoken1.OneServerTests.CxfSSLSAMLBasic1ServerTests;
import com.ibm.ws.wssecurity.fat.cxf.samltoken1.TwoServerTests.CxfSSLSAMLBasic2ServerTests;


import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({

	   //Lite
	   CxfSSLSAMLBasic2ServerTests.class,
      
       //Full
       CxfSSLSAMLBasic1ServerTests.class,
       CxfSAMLWSSTemplates2ServerTests.class,
       CxfSAMLWSSTemplates1ServerTests.class,
       

       
              
})

/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

    //The following run EE7 and EE8 full fat and EE9 lite fat
    @ClassRule
    public static RepeatTests r = RepeatTests.with(new EmptyAction().fullFATOnly()).andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().removeFeature("jsp-2.2").removeFeature("jaxws-2.2").removeFeature("servlet-3.1").removeFeature("usr:wsseccbh-1.0").addFeature("jsp-2.3").addFeature("jaxws-2.3").addFeature("servlet-4.0").addFeature("usr:wsseccbh-2.0")).andWith(FeatureReplacementAction.EE9_FEATURES().removeFeature("jsp-2.3").removeFeature("jaxws-2.3").removeFeature("servlet-4.0").addFeature("pages-3.0").addFeature("xmlWS-3.0").addFeature("servlet-5.0"));

}
