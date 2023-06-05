/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt21.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.actions.SecurityTestRepeatAction;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt21FatConstants;
import com.ibm.ws.security.mp.jwt21.fat.featureSupportTests.FeatureSupport_ConfigInAppTests;
import com.ibm.ws.security.mp.jwt21.fat.featureSupportTests.FeatureSupport_ConfigInServerXmlTests;
import com.ibm.ws.security.mp.jwt21.fat.featureSupportTests.FeatureSupport_EnvVars_ClockSkew;
import com.ibm.ws.security.mp.jwt21.fat.featureSupportTests.FeatureSupport_EnvVars_DecryptAlg;
import com.ibm.ws.security.mp.jwt21.fat.featureSupportTests.FeatureSupport_EnvVars_TokenAge;
import com.ibm.ws.security.mp.jwt21.fat.featureSupportTests.FeatureSupport_SystemProperties_ClockSkew;
import com.ibm.ws.security.mp.jwt21.fat.featureSupportTests.FeatureSupport_SystemProperties_DecryptAlg;
import com.ibm.ws.security.mp.jwt21.fat.featureSupportTests.FeatureSupport_SystemProperties_TokenAge;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.RepeatTests;

@SuppressWarnings({ "restriction" })
@RunWith(Suite.class)
@SuiteClasses({
        AlwaysPassesTest.class,

        // Ensure 2.1 function not available with only 1.2 Feature enabled
        FeatureSupport_ConfigInAppTests.class,
        FeatureSupport_ConfigInAppTests.class,
        FeatureSupport_ConfigInServerXmlTests.class,
        FeatureSupport_EnvVars_ClockSkew.class,
        FeatureSupport_EnvVars_TokenAge.class,
        FeatureSupport_EnvVars_DecryptAlg.class,
        FeatureSupport_SystemProperties_ClockSkew.class,
        FeatureSupport_SystemProperties_TokenAge.class,
        FeatureSupport_SystemProperties_DecryptAlg.class,

})

public class FATSuite {

    public static String authHeaderPrefix = MPJwt21FatConstants.TOKEN_TYPE_BEARER;

    /**
     * Run tests that use 2.1 attributes with the 1.2 feature and show that they're ignored
     */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new SecurityTestRepeatAction(MPJwt21FatConstants.MP_JWT_12));

}
