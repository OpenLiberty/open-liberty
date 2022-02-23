/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import com.ibm.ws.wssecurity.fat.cxf.wss11enc.CxfWss11EncTests;
import com.ibm.ws.wssecurity.fat.cxf.wss11sig.CxfWss11SigTests;
import com.ibm.ws.wssecurity.fat.cxf.x509migtoken.CxfEndSupTokensAsymTests;
import com.ibm.ws.wssecurity.fat.cxf.x509migtoken.CxfEndSupTokensSymTests;
import com.ibm.ws.wssecurity.fat.cxf.x509migtoken.CxfX509MigSymSha2NegativeTests;
import com.ibm.ws.wssecurity.fat.cxf.x509migtoken.CxfX509MigSymTests;
import com.ibm.ws.wssecurity.fat.cxf.x509migtoken.CxfX509MigTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509ASyncTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509BasicTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509CrlTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509EncTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509ObjectTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509OverRideTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509SigTests;
import com.ibm.ws.wssecurity.fat.cxf.x509token.CxfX509StrTypeTests;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({

                //Lite
                CxfX509MigTests.class,

                //Full TODO: 03/25/22 - once we complete the feature, runtime updates of jaxws-2.2, complete the testing updates
                /*CxfSha2SigTests.class,
                CxfWss11SigTests.class,
                CxfWss11EncTests.class,
                CxfX509BasicTests.class,
                CxfX509EncTests.class,
                CxfX509ObjectTests.class,
                CxfX509OverRideTests.class,
                CxfX509SigTests.class,
                CxfX509ASyncTests.class,
                CxfX509StrTypeTests.class,
                CxfX509CrlTests.class,
                CxfX509MigSymSha2NegativeTests.class,
                CxfX509MigSymTests.class,
                CxfEndSupTokensAsymTests.class,
                CxfEndSupTokensSymTests.class*/

})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */

public class FATSuite {

    // The following runs EE7 and EE8 full fat and EE9 lite fat
    @ClassRule
    //public static RepeatTests r = RepeatTests.with(new EmptyAction().fullFATOnly()).andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().removeFeature("jsp-2.2").removeFeature("jaxws-2.2").removeFeature("servlet-3.1").removeFeature("usr:wsseccbh-1.0").addFeature("jsp-2.3").addFeature("jaxws-2.3").addFeature("servlet-4.0").addFeature("usr:wsseccbh-2.0")).andWith(FeatureReplacementAction.EE9_FEATURES().removeFeature("jsp-2.3").removeFeature("jaxws-2.3").removeFeature("servlet-4.0").removeFeature("appSecurity-3.0").removeFeature("usr:wsseccbh-1.0").addFeature("appSecurity-4.0").addFeature("pages-3.0").addFeature("xmlWS-3.0").addFeature("servlet-5.0").addFeature("usr:wsseccbh-2.0"));
    public static RepeatTests r = RepeatTests.with(new EmptyAction().fullFATOnly()).andWith(FeatureReplacementAction.EE9_FEATURES().removeFeature("jsp-2.2").removeFeature("jaxws-2.2").removeFeature("servlet-3.1").removeFeature("usr:wsseccbh-1.0").addFeature("pages-3.0").addFeature("xmlWS-3.0").addFeature("servlet-5.0").addFeature("usr:wsseccbh-2.0"));

}
