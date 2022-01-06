/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwtsso.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.actions.SecurityTestRepeatAction;
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatConstants;

import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
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
    public static RepeatTests r = RepeatTests.with(new SecurityTestRepeatAction(JwtFatConstants.MPJWT_VERSION_12));

}
