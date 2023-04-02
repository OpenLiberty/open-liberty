/*******************************************************************************
 * Copyright (c) 2021,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.cxf.samltoken2;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.wssecurity.fat.cxf.samltoken2.OneServerTests.CxfSAMLCaller1ServerTests;
import com.ibm.ws.wssecurity.fat.cxf.samltoken2.OneServerTests.CxfSAMLCaller1ServerEE7LiteTests;
import com.ibm.ws.wssecurity.fat.cxf.samltoken2.TwoServerTests.CxfSAMLCaller2ServerTests;
import com.ibm.ws.wssecurity.fat.utils.common.RepeatWithEE7cbh20;
import com.ibm.ws.wssecurity.fat.utils.common.RepeatWithEE7cbh10;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({

	    //Lite for EE9, EE10 tests
	    //Full mode also runs some tests with EE7 wsseccbh-1.0 and -2.0 and old/new formats of ehcache
        CxfSAMLCaller1ServerTests.class,
        //issue 24471 for additional Lite test:
        CxfSAMLCaller1ServerEE7LiteTests.class,
        //Full mode runs only EE7 with old format ehcache and couple of tests run with wsseccbh-1.0
        CxfSAMLCaller2ServerTests.class
})

/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

    @ClassRule
    //issue 24471
    public static RepeatTests r = RepeatTests.with(new EmptyAction().fullFATOnly()).andWith(new RepeatWithEE7cbh20().fullFATOnly()).andWith(new RepeatWithEE7cbh10().liteFATOnly()).andWith(FeatureReplacementAction.EE9_FEATURES()).andWith(FeatureReplacementAction.EE10_FEATURES());

}
