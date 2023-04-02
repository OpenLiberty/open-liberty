/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package com.ibm.ws.security.mp.jwt21.fat.featureSupportTests;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.ws.security.fat.common.actions.SecurityTestRepeatAction;
import com.ibm.ws.security.fat.common.mp.jwt.utils.MP21ConfigSettings;
import com.ibm.ws.security.mp.jwt21.fat.featureSupportTests.sharedTests.OlderMpJwtFeatures_GenericEnvVarsAndSystemPropertiesTests_ClockSkew;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that will verify that we get the correct behavior when we
 * have mp-config clockSkew defined as system properties
 * We'll test with a server.xml that will NOT have a mpJwt config, the app will NOT have mp-config specified
 * Therefore, we'll be able to show that the config is coming from the system properties
 */

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
@SkipForRepeat({ EmptyAction.ID })
public class FeatureSupport_SystemProperties_ClockSkew extends OlderMpJwtFeatures_GenericEnvVarsAndSystemPropertiesTests_ClockSkew {

    protected static Class<?> thisClass = FeatureSupport_SystemProperties_ClockSkew.class;

    @Server("com.ibm.ws.security.mp.jwt.2.1.fat.jvmOptions")
    public static LibertyServer sysPropResourceServer;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(new SecurityTestRepeatAction("sys_prop"));

    @BeforeClass
    public static void setUp() throws Exception {

        resourceServer = sysPropResourceServer;
        commonMpJwt21Setup(resourceServer, "rs_server_AltConfigNotInApp_allowClockSkewOverrideServerXmlConfig.xml", 1, 1, MP21ConfigSettings.DefaultKeyMgmtKeyAlg, MPConfigLocation.SYSTEM_PROP);

    }

}
