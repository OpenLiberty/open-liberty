/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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

import com.ibm.ws.wssecurity.fat.cxf.caller.CxfCallerUNTCBHPackageTests;
import com.ibm.ws.wssecurity.fat.cxf.caller.CxfCallerUNTTests;
import com.ibm.ws.wssecurity.fat.cxf.usernametoken.CxfSSLUNTBasicTests;
import com.ibm.ws.wssecurity.fat.cxf.usernametoken.CxfSSLUNTNonceTests;
import com.ibm.ws.wssecurity.fat.cxf.usernametoken.CxfSSLUNTNonceTimeOutTests;
import com.ibm.ws.wssecurity.fat.cxf.wsstemplates.CxfWssTemplatesTestsWithExternalPolicy;
import com.ibm.ws.wssecurity.fat.cxf.wsstemplates.CxfWssTemplatesTestsWithWSDL;
import com.ibm.ws.wssecurity.fat.utils.common.RepeatWithEE7cbh20;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({

                //Lite for EE9, EE10 tests
                //Full mode also runs EE7-wsseccbh-1.0 (also old ehcache) and EE7-wsseccbh-2.0 (also new ehcache)
                CxfSSLUNTNonceTests.class,
                CxfCallerUNTTests.class,

                //Full for EE7-wsseccbh-1.0 (also old ehcache) and EE7-wsseccbh-2.0 (also new ehcache)
                //Full mode also runs Lite tests
                CxfCallerUNTCBHPackageTests.class,
                CxfSSLUNTNonceTimeOutTests.class,
                CxfSSLUNTBasicTests.class,
                CxfWssTemplatesTestsWithWSDL.class,
                CxfWssTemplatesTestsWithExternalPolicy.class

})

/**
 * Purpose: This suite collects and runs all known good test suites.
 */

public class FATSuite {

    @ClassRule
    //issue 23060
    public static RepeatTests r = RepeatTests.with(new EmptyAction().fullFATOnly()).andWith(new RepeatWithEE7cbh20().fullFATOnly()).andWith(FeatureReplacementAction.EE9_FEATURES()).andWith(FeatureReplacementAction.EE10_FEATURES());

}
