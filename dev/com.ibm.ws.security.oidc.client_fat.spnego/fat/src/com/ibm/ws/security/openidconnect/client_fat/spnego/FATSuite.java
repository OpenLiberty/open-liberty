/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client_fat.spnego;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.AlwaysRunAndPassTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.spnego.SpnegoCommonSetup;
import com.ibm.ws.security.openidconnect.client_fat.spnego.config.SpnegoOidcClientConsent_ConfigTests;
import com.ibm.ws.security.openidconnect.client_fat.spnego.ep.authz.SpnegoOidcClientConsentTests;

@RunWith(Suite.class)
@SuiteClasses({
                SpnegoOidcClientConsent_ConfigTests.class,
                SpnegoOidcClientConsentTests.class,
                AlwaysRunAndPassTest.class

})
public class FATSuite extends SpnegoCommonSetup {

}
