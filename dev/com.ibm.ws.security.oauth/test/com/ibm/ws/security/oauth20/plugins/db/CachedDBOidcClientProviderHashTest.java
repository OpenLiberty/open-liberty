/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins.db;

import javax.naming.Context;

import org.junit.Before;
import org.junit.BeforeClass;

import com.ibm.ws.security.oauth.test.ClientRegistrationHelper;
import com.ibm.ws.security.oauth20.util.HashSecretUtils;

import test.common.SharedOutputManager;

/**
 * This unit test is running with Hash enabled for the client secret
 */
public class CachedDBOidcClientProviderHashTest extends CachedDBOidcClientProviderTest {

    public CachedDBOidcClientProviderHashTest() {
        clientRegistrationHelper = new ClientRegistrationHelper(true);
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();

        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InitialContextFactoryMock.class.getName());
    }

    @Override
    @Before
    public void setupBefore() {
        _testName = testName.getMethodName();
        System.out.println("Entering test: " + _testName);
        SAMPLE_CLIENTS = clientRegistrationHelper.getsampleOidcBaseClients(5, PROVIDER_NAME);
        CachedDBOidcClientProvider oidcBaseClientProvider = invokeConstructorAndInitialize();

        instantiateMockProvider();
        try {
            deleteAllClientsInDB(oidcBaseClientProvider);
            insertSampleClientsToDb(oidcBaseClientProvider, clientRegistrationHelper.isHash());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(_testName, t);
        }
    }

    @Override
    protected CachedDBOidcClientProvider invokeConstructorAndInitialize() {
        CachedDBOidcClientProvider oidcBaseClientProvider = new CachedDBOidcClientProvider(PROVIDER_NAME, InitialContextFactoryMock.dsMock, SCHEMA_TABLE_NAME, null, null, EMPTY_STRING_ARR, HashSecretUtils.PBKDF2WithHmacSHA512);
        oidcBaseClientProvider.initialize();

        return oidcBaseClientProvider;
    }

}
