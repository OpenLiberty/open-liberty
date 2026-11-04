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
package com.ibm.ws.zos.wlm.internal;

/**
 * AuthorizedWLMNativeServices implementation that mocks all native methods. This class is
 * used for testing AuthorizedWLMNativeServices on non-z/OS platforms.
 *
 * This class extends AuthorizedWLMNativeServices and overrides only the native methods.
 * All native methods invoked against this object are forwarded to the mock
 * AuthorizedWLMNativeServices object (AuthorizedWLMNativeServicesTest.mock).
 *
 * The Mock AuthorizedWLMNativeServices is created and managed by the test class AuthorizedWLMNativeServicesTest.
 * AuthorizedWLMNativeServicesTest sets up the mockery context -- i.e. the Expectations for the Mock
 * AuthorizedWLMNativeServices -- i.e. the list of native methods that are expected to be invoked
 * for each test.
 *
 */
public class AuthorizedWLMNativeServicesMockNative extends AuthorizedWLMNativeServices {

    @Override
    protected int ntv_connectAsWorkMgr(String subSystem,
                                       String subSystemName,
                                       String createFunctionName,
                                       String classifyCollectionName) {
        return AuthorizedWLMNativeServicesTest.mock.ntv_connectAsWorkMgr(subSystem,
                                                                         subSystemName,
                                                                         createFunctionName,
                                                                         classifyCollectionName);

    }

    @Override
    protected int ntv_disconnectAsWorkMgr(int connectToken) {
        return AuthorizedWLMNativeServicesTest.mock.ntv_disconnectAsWorkMgr(connectToken);
    }

    @Override
    protected boolean ntv_joinWorkUnit(byte[] token) {
        return AuthorizedWLMNativeServicesTest.mock.ntv_joinWorkUnit(token);
    }

    @Override
    protected boolean ntv_leaveWorkUnit(byte[] token) {
        return AuthorizedWLMNativeServicesTest.mock.ntv_leaveWorkUnit(token);
    }

    @Override
    protected byte[] ntv_createWorkUnit(int connectToken,
                                        byte[] classificationInfo,
                                        String createFunctionName,
                                        String classifyCollectionName,
                                        long arrivalTime,
                                        int serviceClassToken,
                                        byte[] outputServiceClassToken) {
        return AuthorizedWLMNativeServicesTest.mock.ntv_createWorkUnit(connectToken,
                                                                       classificationInfo,
                                                                       createFunctionName,
                                                                       classifyCollectionName,
                                                                       arrivalTime,
                                                                       serviceClassToken,
                                                                       outputServiceClassToken);
    }

    @Override
    protected int ntv_deleteWorkUnit(byte[] etoken, byte[] deleteData) {
        return AuthorizedWLMNativeServicesTest.mock.ntv_deleteWorkUnit(etoken, deleteData);
    }

}
