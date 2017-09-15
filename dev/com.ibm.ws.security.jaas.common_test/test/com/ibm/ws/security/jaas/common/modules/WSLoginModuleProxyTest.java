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
package com.ibm.ws.security.jaas.common.modules;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.security.authentication.jaas.modules.WSLoginModuleImpl;

/**
 *
 */
@SuppressWarnings("unchecked")
public class WSLoginModuleProxyTest {

    private static SharedOutputManager outputMgr;

    private final Class wsLoginClass = WSLoginModuleImpl.class;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
//        mockery.checking(new Expectations() {
//            {
//                allowing(cc).locateService(JAASLoginModuleConfig.KEY_CLASSLOADING_SVC, classLoadingServiceRef);
//                will(returnValue(classLoadingService));
//                allowing(classLoadingService).getSharedLibraryClassLoader(sharedLibrary);
//                will(returnValue(classLoader));
//                allowing(classLoader).loadClass(delegateClassName);
//                will(returnValue(wsLoginClass));
//            }
//        });
//
//        jaasLoginModuleConfig.setClassLoadingSvc(classLoadingServiceRef);
//        jaasLoginModuleConfig.activate(cc, null);
    }

    /**
     * The cleanup of the JAASServiceImpl is necessary as the
     * references it holds are static, and if not cleaned up, will spill
     * over into the next test.
     * 
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
//        jaasLoginModuleConfig.deactivate(cc);
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInitializeWithNullOptions() throws Throwable {
        WSLoginModuleProxy module = new WSLoginModuleProxy();
        Subject subject = new Subject();
        CallbackHandler callbackHandler = null;
        Map sharedState = new HashMap();
        Map options = null;
        module.initialize(subject, callbackHandler, sharedState, options);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInitializeWithNoDelegate() throws Throwable {
        WSLoginModuleProxy module = new WSLoginModuleProxy();
        Subject subject = new Subject();
        CallbackHandler callbackHandler = null;
        Map sharedState = new HashMap();
        Map<String, Object> options = new HashMap<String, Object>();
        module.initialize(subject, callbackHandler, sharedState, options);
    }

    public void testInitializeCustomLoginModule() {
        WSLoginModuleProxy module = new WSLoginModuleProxy();
        Subject subject = new Subject();
        CallbackHandler callbackHandler = null;
        Map sharedState = new HashMap();
        Map<String, Object> options = new HashMap<String, Object>();
        module.initialize(subject, callbackHandler, sharedState, options);
    }

    @Test(expected = RuntimeException.class)
    public void testInitializeCustomLoginModuleWithoutSharedLib() {
        WSLoginModuleProxy module = new WSLoginModuleProxy();
        Subject subject = new Subject();
        CallbackHandler callbackHandler = null;
        Map sharedState = new HashMap();
        Map<String, Object> options = new HashMap<String, Object>();
        module.initialize(subject, callbackHandler, sharedState, options);
    }

    @Test
    public void testInitializeWSloginModuleImpl() throws Throwable {
        WSLoginModuleProxy module = new WSLoginModuleProxy();
        Subject subject = new Subject();
        CallbackHandler callbackHandler = null;
        Map sharedState = new HashMap();
        Map<String, Object> options = new HashMap<String, Object>();
        options.put("delegate", wsLoginClass);
        module.initialize(subject, callbackHandler, sharedState, options);
    }
}
