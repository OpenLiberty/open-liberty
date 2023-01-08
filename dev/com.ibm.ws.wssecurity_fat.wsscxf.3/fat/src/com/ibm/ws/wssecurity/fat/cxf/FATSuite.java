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

import com.ibm.ws.wssecurity.fat.cxf.wss11enc.CxfWss11EncTests;
import com.ibm.ws.wssecurity.fat.cxf.wss11sig.CxfWss11SigTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509ASyncTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509BasicTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509CrlTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509EncTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509ObjectTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509OverRideTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509SigTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509StrTypeTests;
import com.ibm.ws.wssecurity.fat.utils.common.RepeatWithEE7cbh20;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({

                //Lite for EE9, EE10 tests
                //Full mode also runs EE7-wsseccbh-1.0 and EE7-wsseccbh-2.0
                CxfX509SigTests.class,

                //Full for EE7-wsseccbh-1.0 and EE7-wsseccbh-2.0
                //Full mode also runs Lite tests
                CxfWss11SigTests.class,
                CxfWss11EncTests.class,
                CxfX509BasicTests.class,
                CxfX509EncTests.class,
                CxfX509ObjectTests.class,
                CxfX509OverRideTests.class,
                CxfX509ASyncTests.class,
                CxfX509StrTypeTests.class,
                CxfX509CrlTests.class

})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */

public class FATSuite {

    @ClassRule
    //issue 23060
    public static RepeatTests r = RepeatTests.with(new EmptyAction().fullFATOnly()).andWith(new RepeatWithEE7cbh20().fullFATOnly()).andWith(FeatureReplacementAction.EE9_FEATURES()).andWith(FeatureReplacementAction.EE10_FEATURES());

}
