/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.registry.saf.internal;

import java.util.List;

import com.ibm.ws.security.credentials.saf.SAFCredentialsService;
import com.ibm.ws.zos.jni.NativeMethodManager;

/**
 * SAFAuthorizedRegistry implementation that mocks all native methods. This class is
 * used for testing SAFAuthorizedRegistry on non-z/OS platforms.
 *
 * This class extends SAFAuthorizedRegistry and overrides only the native methods.
 * All native methods invoked against this object are forwarded to the mock
 * SAFAuthorizedRegistry object (SAFAuthorizedRegistryTest._mockSAFRegistry).
 *
 * The Mock SAFAuthorizedRegistry is created and managed by the test class SAFAuthorizedRegistryTest.
 * SAFAuthorizedRegistryTest sets up the mockery context -- i.e. the Expectations for the Mock
 * SAFAuthorizedRegistry -- i.e. the list of native methods that are expected to be invoked
 * for each test.
 *
 */
public class SAFAuthorizedRegistryMockNative extends SAFAuthorizedRegistry {

    public SAFAuthorizedRegistryMockNative(SAFRegistryConfig config, NativeMethodManager nativeMethodManager, SAFCredentialsService safCredentialsService) {
        super(config, nativeMethodManager, safCredentialsService);
    }

    @Override
    protected boolean ntv_checkPassword(byte[] user, byte[] pwd, String applid, byte[] passwdResult) {
        return SAFRegistryTest.mock.ntv_checkPassword(user, pwd, applid, passwdResult);
    }

    @Override
    protected byte[] ntv_getRealm() {
        return SAFRegistryTest.mock.ntv_getRealm();
    }

    @Override
    protected boolean ntv_isValidUser(byte[] userSecurityName) {
        return SAFRegistryTest.mock.ntv_isValidUser(userSecurityName);
    }

    @Override
    protected boolean ntv_isValidGroup(byte[] groupSecurityName) {
        return SAFRegistryTest.mock.ntv_isValidGroup(groupSecurityName);
    }

    @Override
    protected byte[] ntv_mapCertificate(byte[] cert, int certLength) {
        return SAFRegistryTest.mock.ntv_mapCertificate(cert, certLength);
    }

    @Override
    protected List<byte[]> ntv_getGroupsForUser(byte[] userName, List<byte[]> list) {
        return SAFRegistryTest.mock.ntv_getGroupsForUser(userName, list);
    }

    @Override
    protected boolean ntv_resetGroupsCursor() {
        return SAFRegistryTest.mock.ntv_resetGroupsCursor();
    }

    @Override
    protected byte[] ntv_getNextGroup() {
        return SAFRegistryTest.mock.ntv_getNextGroup();
    }

    @Override
    protected boolean ntv_closeGroupsDB() {
        return SAFRegistryTest.mock.ntv_closeGroupsDB();
    }

    @Override
    protected boolean ntv_resetUsersCursor() {
        return SAFRegistryTest.mock.ntv_resetUsersCursor();
    }

    @Override
    protected byte[] ntv_getNextUser() {
        return SAFRegistryTest.mock.ntv_getNextUser();
    }

    @Override
    protected boolean ntv_closeUsersDB() {
        return SAFRegistryTest.mock.ntv_closeUsersDB();
    }

    @Override
    protected byte[] ntv_getPlexName() {
        return SAFRegistryTest.mock.ntv_getPlexName();
    }

    @Override
    protected boolean ntv_isValidGroupAuthorized(byte[] groupSecurityName,
                                                 byte[] applname,
                                                 byte[] safServiceResults) {
        return SAFRegistryTest.mock.ntv_isValidGroupAuthorized(groupSecurityName, applname, safServiceResults);
    }
}
