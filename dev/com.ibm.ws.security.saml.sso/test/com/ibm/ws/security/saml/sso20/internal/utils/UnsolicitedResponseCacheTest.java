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

import java.util.Date;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

/**
 *
 */
public class UnsolicitedResponseCacheTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    public static final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    static UnsolicitedResponseCache instance;
    private static final int entryLimit = 100;
    private static final long timeoutInMilliSeconds = 100;
    private static final long clockSkew = 50;

    private static final long longValue = 909090;
    private static final String key1 = "1";
    private static final String key2 = "2";

    @SuppressWarnings("unused")
    private static final SecurityManager securityManager = mockery.mock(SecurityManager.class);
    private static final Object object = mockery.mock(Object.class, "objectTest");

    @BeforeClass
    public static void setUp() {
        outputMgr.trace("*=all");
        instance = new UnsolicitedResponseCache(entryLimit, timeoutInMilliSeconds, clockSkew);
    }

    @AfterClass
    public static void tearDown() {
        outputMgr.trace("*=all=disabled");
    }

    @Test
    public void sizeTest() {
        instance.size();
    }

    @Test
    public void put() {
        instance.put(key1, (new Date()).getTime());

    }

    @Test
    public void getTest() {
        instance.get(key1);
    }

    @Test
    public void isValid() {
        instance.put(key2, longValue);
        instance.isValid(key2);
    }

    @Test
    public void evictStaleEntries() {
        instance.evictStaleEntries();
    }

    @Test
    public void removeTest() {
        instance.remove(object);
    }

}
