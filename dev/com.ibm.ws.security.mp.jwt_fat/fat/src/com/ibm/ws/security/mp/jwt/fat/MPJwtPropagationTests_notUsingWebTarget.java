/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.fat;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.ws.security.mp.jwt.fat.sharedTests.MPJwtPropagationTests;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is a common test class that will test for the proper behavior of webTarget/authnToken and the
 * client config property, com.ibm.ws.jaxrs.client.mpjwt.sendToken.
 * These tests are run with authnToken=mpjwt, or not set at all and
 * com.ibm.ws.jaxrs.client.mpjwt.sendToken is not set, or set to true, false, "true" or "false"
 *
 * Each test case will invoke an app on the mpjwt.client server. This app will set the client property
 * as requested and then invoke the target App on the mpjwt server.
 *
 * These tests are extended by 2 different classes. One specifies a server that has "webTarget"
 * configured and the other does not. That setting will affect behaviour and the webTargetConfigured
 * variable will be set by those 2 classes to indicate how some tests should behave.
 *
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class MPJwtPropagationTests_notUsingWebTarget extends MPJwtPropagationTests {

    protected static Class<?> thisClass = MPJwtPropagationTests_notUsingWebTarget.class;

    @BeforeClass
    public static void setUp() throws Exception {

        propagationSetUp("rs_server_orig.xml", webTargetConfigured_false);

    }

}
