/*******************************************************************************
 * Copyright (c) 2014, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
//                CustomFormHttpAuthenticationMechanismTest.class,
                HttpAuthenticationMechanismTest.class,
                RememberMeTest.class,
                AutoApplySessionTest.class,
                FormHttpAuthenticationMechanismTest.class,
                LdapIdentityStoreDeferredSettingsTest.class,
                MultipleIdentityStoreBasicTest.class,
                MultipleIdentityStoreApplCustomTest.class,
                MultipleIdentityStoreFormRedirectTest.class,
                MultipleIdentityStoreFormForwardTest.class,
                MultipleIdentityStoreCustomFormRedirectTest.class,
                MultipleIdentityStoreCustomFormForwardTest.class,
                NoJavaEESecFormTest.class
})

/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

}
