/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.security.oidcclientcore.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class SessionBasedStorageTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    private final HttpSession session = mockery.mock(HttpSession.class);

    private final String testAttributeName = "testName";
    private final String testAttributeValue = "testValue";

    private SessionBasedStorage storage;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        storage = new SessionBasedStorage(request);
    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_store() {
        mockery.checking(new Expectations() {
            {
                one(request).getSession();
                will(returnValue(session));
                one(session).setAttribute(testAttributeName, testAttributeValue);
            }
        });

        storage.store(testAttributeName, testAttributeValue);
    }

    @Test
    public void test_store_withStorageProperties() {
        // should be identical to test_store(), since properties can't be set in http session attributes
        mockery.checking(new Expectations() {
            {
                one(request).getSession();
                will(returnValue(session));
                one(session).setAttribute(testAttributeName, testAttributeValue);
            }
        });

        storage.store(testAttributeName, testAttributeValue, new StorageProperties());
    }

    @Test
    public void test_get() {
        mockery.checking(new Expectations() {
            {
                one(request).getSession();
                will(returnValue(session));
                one(session).getAttribute(testAttributeName);
                will(returnValue(testAttributeValue));
            }
        });

        String value = storage.get(testAttributeName);

        assertEquals("Expected the values to be equal.", testAttributeValue, value);
    }

    @Test
    public void test_get_doesNotExist() {
        String testAttributeName = "doesNotExist";
        mockery.checking(new Expectations() {
            {
                one(request).getSession();
                will(returnValue(session));
                one(session).getAttribute(testAttributeName);
                will(returnValue(null));
            }
        });

        String value = storage.get(testAttributeName);

        assertNull("Expected the value to be null if the attribute name does not exist in the session.", value);
    }

    @Test
    public void test_remove() {
        mockery.checking(new Expectations() {
            {
                one(request).getSession();
                will(returnValue(session));
                one(session).removeAttribute(testAttributeName);
            }
        });

        storage.remove(testAttributeName);
    }

}
