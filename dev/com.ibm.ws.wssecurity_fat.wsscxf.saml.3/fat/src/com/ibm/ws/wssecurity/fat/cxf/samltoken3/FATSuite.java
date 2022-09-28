/*******************************************************************************
 * Copyright (c) 2021,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.cxf.samltoken3;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.wssecurity.fat.cxf.samltoken3.LiteAlwaysRunTest.AlwaysRunAndPassTest;
import com.ibm.ws.wssecurity.fat.cxf.samltoken3.TwoServerTests.CxfSAMLCaller2ServerTests;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({

	    //Lite
        AlwaysRunAndPassTest.class,
        //Full 
        CxfSAMLCaller2ServerTests.class
})

/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {
	
	//The following run EE7 and EE8 full fat and no EE9 lite fat
    @ClassRule
    public static RepeatTests r = RepeatTests.with(new EmptyAction().fullFATOnly()).andWith(FeatureReplacementAction.EE9_FEATURES().removeFeature("usr:wsseccbh-1.0").addFeature("usr:wsseccbh-2.0")).andWith(FeatureReplacementAction.EE10_FEATURES().removeFeature("usr:wsseccbh-1.0").addFeature("usr:wsseccbh-2.0"));
    
}
