/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
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

/**
 * Was OauthClientStoreTest, changed to OauthClientStoreXORTest when OauthClientStoreHashTest was added
 */
public class OauthClientStoreXORTest extends OauthClientStoreCommon {

    public OauthClientStoreXORTest() {
        clientRegistrationHelper = new ClientRegistrationHelper(false);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        oauthClientStore = new OauthClientStore(componentId, oauthStore);
        oauthClientStore.initialize();
    }

}
