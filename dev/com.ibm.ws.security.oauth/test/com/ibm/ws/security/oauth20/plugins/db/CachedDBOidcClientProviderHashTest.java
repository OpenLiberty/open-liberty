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

import com.ibm.ws.security.oauth20.util.HashSecretUtils;

import test.common.SharedOutputManager;

/**
 * This unit test is running with Hash enabled for the client secret
 */
public class CachedDBOidcClientProviderHashTest extends CachedDBOidcClientProviderTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        setHash(true);
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();

        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InitialContextFactoryMock.class.getName());

        SAMPLE_CLIENTS = getsampleOidcBaseClients(5, PROVIDER_NAME);
    }

    @Override
    @Before
    public void setupBefore() {
        _testName = testName.getMethodName();
        System.out.println("Entering test: " + _testName);
        CachedDBOidcClientProvider oidcBaseClientProvider = invokeConstructorAndInitialize();

        instantiateMockProvider();
        try {
            deleteAllClientsInDB(oidcBaseClientProvider);
            insertSampleClientsToDb(oidcBaseClientProvider);

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
