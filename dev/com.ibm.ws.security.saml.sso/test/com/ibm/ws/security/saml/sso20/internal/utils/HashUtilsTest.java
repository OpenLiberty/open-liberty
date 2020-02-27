/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal.utils;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

public class HashUtilsTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    public static final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static final String TEXT_TO_HASH = "IBMTestingTest";
    private static final String DEFAULT_ALGORITHM = "SHA-256";
    private static final String WRONG_ALGORITHM = "SHA-255";
    private static final String WRONG_CHARSET = "TFU-8";

    @BeforeClass
    public static void setUp() {
        outputMgr.trace("*=all");
    }

    @AfterClass
    public static void tearDown() {
        mockery.assertIsSatisfied();
        outputMgr.trace("*=all=disabled");
    }

    @Test
    public void digestTest() {
        HashUtils.digest(TEXT_TO_HASH);
    }

    @Test(expected = java.lang.RuntimeException.class)
    public void digestNoSuchAlgorithmTest() {
        HashUtils.digest(TEXT_TO_HASH, WRONG_ALGORITHM);
    }

    @Test(expected = java.lang.RuntimeException.class)
    public void digestUnsupportedEncodingException() {
        HashUtils.digest(TEXT_TO_HASH, DEFAULT_ALGORITHM, WRONG_CHARSET);
    }

}
