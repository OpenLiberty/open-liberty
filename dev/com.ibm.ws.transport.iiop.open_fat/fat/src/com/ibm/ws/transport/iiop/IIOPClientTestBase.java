/*******************************************************************************
 * Copyright (c) 2015-2023 IBM Corporation and others.
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
package com.ibm.ws.transport.iiop;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import componenttest.annotation.Server;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.iiop.common.MarshallingOperations;

//@RunWith(FATRunner.class)
public abstract class IIOPClientTestBase extends FATServletClient implements MarshallingOperations {

	@Server("buckyball")
	public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
	server.installSystemBundle("test.user.feature");
		server.installSystemFeature("test.user.feature-1.0");
		server.installSystemBundle("test.iiop");
		server.installSystemFeature("test.iiop-1.0");
		server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
	server.stopServer();
    }


    protected abstract String getServletName();

    @Rule
    public final TestName testName = new TestName();

    @Override
    @Test
    public void intToInt() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void intToInteger() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void integerToInteger() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void stringToString() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void intToObject() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void stringToObject() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void dateToObject() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void stubToObject() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void testClassToObject() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void userFeatureToObject() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void intArrToObject() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void stringArrToObject() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void dateArrToObject() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void stubArrToObject() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void testClassArrToObject() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void userFeatureArrToObject() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void intToSerializable() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void stringToSerializable() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void dateToSerializable() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void stubToSerializable() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void testClassToSerializable() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void userFeatureToSerializable() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void intArrToSerializable() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void stringArrToSerializable() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void dateArrToSerializable() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void stubArrToSerializable() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void testClassArrToSerializable() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void userFeatureArrToSerializable() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void stubToEjbIface() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void stubToRemote() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void testClassToTestClass() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void intArrToIntArr() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void stringArrToStringArr() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void stringArrToObjectArr() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void dateArrToObjectArr() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void stubArrToObjectArr() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void testClassArrToObjectArr() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void userFeatureArrToObjectArr() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void stringArrToSerializableArr() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void dateArrToSerializableArr() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void stubArrToSerializableArr() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void testClassArrToSerializableArr() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void userFeatureArrToSerializableArr() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void stubArrToEjbIfaceArr() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void stubArrToRemoteArr() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void testClassArrToTestClassArr() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void enumToObject() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void enumToSerializable() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void timeUnitToObject() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void timeUnitToSerializable() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void cmsfv2ChildDataToObject() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void cmsfv2ChildDataToSerializable() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void testIDLEntityToObject() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void testIDLEntityToSerializable() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void testIDLEntityToIDLEntity() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void testIDLEntityArrToIDLEntityArr() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }

    @Override
    @Test
    public void testTwoLongsToTwoLongs() throws Exception {
        FATServletClient.runTest(server, getServletName(), testName);
    }
}
