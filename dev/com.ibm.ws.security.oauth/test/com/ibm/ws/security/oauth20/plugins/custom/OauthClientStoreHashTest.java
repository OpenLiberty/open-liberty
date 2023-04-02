/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins.custom;

import org.junit.Before;

import com.ibm.ws.security.oauth.test.ClientRegistrationHelper;
import com.ibm.ws.security.oauth20.util.HashSecretUtils;

/**
 * Copied from OauthClientStoreXORTest/OauthClientStoreTest, added common unit test for both
 */
public class OauthClientStoreHashTest extends OauthClientStoreCommon {

    public OauthClientStoreHashTest() {
        clientRegistrationHelper = new ClientRegistrationHelper(true);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        oauthClientStore = new OauthClientStore(componentId, oauthStore, HashSecretUtils.PBKDF2WithHmacSHA512);
        oauthClientStore.initialize();
    }
}
