/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
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

package com.ibm.ws.wssecurity.fat.cxf;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.wssecurity.fat.cxf.sha2sig.CxfSha2SigTests;
import com.ibm.ws.wssecurity.fat.cxf.x509migtoken.CxfEndSupTokensAsymTests;
import com.ibm.ws.wssecurity.fat.cxf.x509migtoken.CxfEndSupTokensSymTests;
import com.ibm.ws.wssecurity.fat.cxf.x509migtoken.CxfX509MigSymEE7LiteTests;
import com.ibm.ws.wssecurity.fat.cxf.x509migtoken.CxfX509MigSymSha2NegativeTests;
import com.ibm.ws.wssecurity.fat.cxf.x509migtoken.CxfX509MigSymTests;
import com.ibm.ws.wssecurity.fat.cxf.x509migtoken.CxfX509MigTests;
import com.ibm.ws.wssecurity.fat.utils.common.RepeatWithEE7cbh10;
import com.ibm.ws.wssecurity.fat.utils.common.RepeatWithEE7cbh20;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({

                AlwaysPassesTest.class,

                //Lite for EE9, EE10 tests
                //Full mode also runs EE7-wsseccbh-1.0 and EE7-wsseccbh-2.0
                CxfX509MigSymEE7LiteTests.class,
                CxfX509MigTests.class,

                //Full for EE7-wsseccbh-1.0 and EE7-wsseccbh-2.0
                //Full mode also runs Lite tests
                CxfSha2SigTests.class,
                CxfX509MigSymSha2NegativeTests.class,
                CxfX509MigSymTests.class,
                CxfEndSupTokensAsymTests.class,
                CxfEndSupTokensSymTests.class

})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */

public class FATSuite {

    @ClassRule
    //issue 24772
    public static RepeatTests r = RepeatTests.with(new EmptyAction().fullFATOnly()).andWith(new RepeatWithEE7cbh20().fullFATOnly()).andWith(new RepeatWithEE7cbh10().liteFATOnly()).andWith(FeatureReplacementAction.EE9_FEATURES()).andWith(FeatureReplacementAction.EE10_FEATURES());

}
