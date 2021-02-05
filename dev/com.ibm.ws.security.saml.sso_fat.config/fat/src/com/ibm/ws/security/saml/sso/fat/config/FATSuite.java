/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.saml.sso.fat.config;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.saml20.fat.commonTest.actions.JakartaEE9SAMLRepeatAction;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({

        SAMLMisc1ConfigTests.class,
        SAMLMisc2ConfigTests.class,
        SAMLSSLConfigTests.class

})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
            .andWith(new JakartaEE9SAMLRepeatAction().liteFATOnly());

}
