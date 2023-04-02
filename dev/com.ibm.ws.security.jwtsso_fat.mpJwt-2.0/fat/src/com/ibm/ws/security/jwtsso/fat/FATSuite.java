/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
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
package com.ibm.ws.security.jwtsso.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.actions.SecurityTestFeatureEE9RepeatAction;
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatConstants;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
        AlwaysPassesTest.class,
        FeatureOnlyTest.class,
        ConfigAttributeTests.class,
        CookieProcessingTests.class,
        ReplayCookieTests.class,
        CookieExpirationTests.class,
        BuilderTests.class,
        SigAlgTests.class,
//                EncryptionTests.class
})
public class FATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests.with(new SecurityTestFeatureEE9RepeatAction(JwtFatConstants.MPJWT_VERSION_20));

}
