/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oidc_social.backchannelLogout.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
        //        AlwaysPassesTest.class,
        //        HttpMethodsTests.class,
        LogoutTokenCreationTests.class,
//        LogoutTokenValidationTests.class,
//        //        ClientConfigTests.class,
//        ServerConfigTests.class

})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

    /*
     * Run EE9 tests in only FULL mode and run EE7/EE8 tests only in LITE mode.
     *
     * This was done to increase coverage of EE9 while not adding a large amount of of test runtime.
     */
    //    @ClassRule
    //    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().liteFATOnly())
    //            .andWith(new SecurityTestRepeatAction().onlyOnWindows().fullFATOnly())
    //            .andWith(new SecurityTestFeatureEE9RepeatAction().alwaysAddFeature("servlet-5.0").notOnWindows().fullFATOnly());

}
