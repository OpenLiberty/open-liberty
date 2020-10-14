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
 * UnauthorizedWLMNativeServicesMockNative implementation that mocks all native methods. This class is
 * used for testing UnauthorizedWLMNativeServices on non-z/OS platforms.
 *
 * This class extends UnauthorizedWLMNativeServices and overrides only the native methods.
 * All native methods invoked against this object are forwarded to the mock
 * UnauthorizedWLMNativeServices object (UnauthorizedWLMNativeServicesTest.mock).
 *
 * The Mock UnauthorizedWLMNativeServices is created and managed by the test class UnauthorizedWLMNativeServicesTest.
 * UnauthorizedWLMNativeServicesTest sets up the mockery context -- i.e. the Expectations for the Mock
 * UnauthorizedWLMNativeServices -- i.e. the list of native methods that are expected to be invoked
 * for each test.
 *
 */
public class UnauthorizedWLMNativeServicesMockNative extends UnauthorizedWLMNativeServices {

    @Override
    protected int ntv_le_connectAsWorkMgr(String subSystem,
                                          String subSystemName,
                                          String createFunctionName,
                                          String classifyCollectionName) {
        return UnauthorizedWLMNativeServicesTest.mock.ntv_le_connectAsWorkMgr(subSystem,
                                                                              subSystemName,
                                                                              createFunctionName,
                                                                              classifyCollectionName);

    }

    @Override
    protected boolean ntv_le_joinWorkUnit(byte[] token) {
        return UnauthorizedWLMNativeServicesTest.mock.ntv_le_joinWorkUnit(token);
    }

    @Override
    protected boolean ntv_le_leaveWorkUnit(byte[] token) {
        return UnauthorizedWLMNativeServicesTest.mock.ntv_le_leaveWorkUnit(token);
    }

    @Override
    protected byte[] ntv_le_createWorkUnit(int connectToken,
                                           byte[] classificationInfo,
                                           String createFunctionName,
                                           String classifyCollectionName,
                                           long arrivalTime) {
        return UnauthorizedWLMNativeServicesTest.mock.ntv_le_createWorkUnit(connectToken,
                                                                            classificationInfo,
                                                                            createFunctionName,
                                                                            classifyCollectionName,
                                                                            arrivalTime);
    }

    @Override
    protected int ntv_le_deleteWorkUnit(byte[] etoken) {
        return UnauthorizedWLMNativeServicesTest.mock.ntv_le_deleteWorkUnit(etoken);
    }

}
