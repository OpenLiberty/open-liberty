/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.fat.spnego;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.AlwaysRunAndPassTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.spnego.SpnegoCommonSetup;
import com.ibm.ws.security.openidconnect.server.fat.spnego.ep.clientregistration.ClientRegistrationOIDCWebClientCommonStoreTest;
import com.ibm.ws.security.openidconnect.server.fat.spnego.ep.clientregistration.config.ClientRegistrationOIDCWebClientCommonStore_ConfigTest;
import com.ibm.ws.security.openidconnect.server.spnego.app.password.RevokeAppPasswordsWithAccessTokenUsingLocalStoreTests;
import com.ibm.ws.security.openidconnect.server.spnego.app.token.RevokeAppTokensWithAccessTokenUsingLocalStoreTests;


@RunWith(Suite.class)
@SuiteClasses({
        AlwaysRunAndPassTest.class,
        ClientRegistrationOIDCWebClientCommonStoreTest.class,
        ClientRegistrationOIDCWebClientCommonStore_ConfigTest.class,
        RevokeAppTokensWithAccessTokenUsingLocalStoreTests.class,
        RevokeAppPasswordsWithAccessTokenUsingLocalStoreTests.class
})
public class FATSuite extends SpnegoCommonSetup {
}
