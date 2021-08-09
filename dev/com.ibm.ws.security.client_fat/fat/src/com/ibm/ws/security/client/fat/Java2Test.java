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

package com.ibm.ws.security.client.fat;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyClientFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Java2Test extends CommonTest {
	private static final Class<?> c = Java2Test.class;

	public static final String JAVA2_CLIENT = "java2Client";
	public static final String SET_PROPERTY_OPERATION = "setProperty";
	public static final String GET_PROPERTY_OPERATION = "getProperty";
	public static final String READ_FILE_OPERATION = "readFile";
	public static final String WRITE_FILE_OPERATION = "writeFile";
	public static final String ACCESS_DENIED = "access denied";

	/**
	 * Test description:
	 * - start the client which will attempt to read a file.
	 *   Neither client.xml nor the ear have permissions defined.
	 * 
	 * Expected results:
	 * - We should see access denied
	 */
	@Mode(TestMode.LITE)
	@Test
	public void testReadFileNoPermissions() {
		testClient = LibertyClientFactory.getLibertyClient(JAVA2_CLIENT);
		List<String> startParms = new ArrayList<String>();
		startParms.add("--");
		startParms.add(READ_FILE_OPERATION);
		startParms.add(testClient.getClientRoot() + File.separator + "test.txt");
		try {
			String fullClientXmlPath = buildFullClientConfigPath(testClient, "client_no_perms.xml");
			Log.info(c, name.getMethodName(), "Using client configuration file: " + fullClientXmlPath);
			copyNewClientConfig(fullClientXmlPath);
			Log.info(c, name.getMethodName(), "Starting the client ...");
			ProgramOutput programOutput = testClient.startClientWithArgs(true,true,true,false,"run",startParms,false);
			Log.info(c, name.getMethodName(), "Client returned.");
			String output = programOutput.getStdout();
			assertTrue("Did not get access denied.", output.contains(ACCESS_DENIED));
			assertNoErrMessages(output);
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - start the client which will attempt to write to a file.
	 *   Neither client.xml nor the ear have permissions defined.
	 * 
	 * Expected results:
	 * - We should see access denied
	 */
	@Test
	public void testWriteFileNoPermissions() {
		testClient = LibertyClientFactory.getLibertyClient(JAVA2_CLIENT);
		List<String> startParms = new ArrayList<String>();
		startParms.add("--");
		startParms.add(WRITE_FILE_OPERATION);
		startParms.add(testClient.getClientRoot() + File.separator + "test.txt");
		try {
			String fullClientXmlPath = buildFullClientConfigPath(testClient, "client_no_perms.xml");
			Log.info(c, name.getMethodName(), "Using client configuration file: " + fullClientXmlPath);
			copyNewClientConfig(fullClientXmlPath);
			Log.info(c, name.getMethodName(), "Starting the client ...");
			ProgramOutput programOutput = testClient.startClientWithArgs(true,true,true,false,"run",startParms,false);
			Log.info(c, name.getMethodName(), "Client returned.");
			String output = programOutput.getStdout();
			assertTrue("Did not get access denied.", output.contains(ACCESS_DENIED));
			assertNoErrMessages(output);
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - start the client which will attempt to get a system property.
	 *   Neither client.xml nor the ear have permissions defined.
	 * 
	 * Expected results:
	 * - We should see access denied
	 */
	@Test
	public void testGetPropertyNoPermissions() {
		testClient = LibertyClientFactory.getLibertyClient(JAVA2_CLIENT);
		List<String> startParms = new ArrayList<String>();
		startParms.add("--");
		startParms.add(GET_PROPERTY_OPERATION);
		System.setProperty("bob", "bob");
		try {
			String fullClientXmlPath = buildFullClientConfigPath(testClient, "client_no_perms.xml");
			Log.info(c, name.getMethodName(), "Using client configuration file: " + fullClientXmlPath);
			copyNewClientConfig(fullClientXmlPath);
			Log.info(c, name.getMethodName(), "Starting the client ...");
			ProgramOutput programOutput = testClient.startClientWithArgs(true,true,true,false,"run",startParms,false);
			Log.info(c, name.getMethodName(), "Client returned.");
			String output = programOutput.getStdout();
			assertTrue("Did not get access denied.", output.contains(ACCESS_DENIED));
			assertNoErrMessages(output);
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - start the client which will attempt to get a system property.
	 *   Neither client.xml nor the ear have permissions defined.
	 * 
	 * Expected results:
	 * - We should see access denied
	 */
	@Test
	public void testSetPropertyNoPermissions() {
		testClient = LibertyClientFactory.getLibertyClient(JAVA2_CLIENT);
		List<String> startParms = new ArrayList<String>();
		startParms.add("--");
		startParms.add(SET_PROPERTY_OPERATION);
		try {
			String fullClientXmlPath = buildFullClientConfigPath(testClient, "client_no_perms.xml");
			Log.info(c, name.getMethodName(), "Using client configuration file: " + fullClientXmlPath);
			copyNewClientConfig(fullClientXmlPath);
			Log.info(c, name.getMethodName(), "Starting the client ...");
			ProgramOutput programOutput = testClient.startClientWithArgs(true,true,true,false,"run",startParms,false);
			Log.info(c, name.getMethodName(), "Client returned.");
			String output = programOutput.getStdout();
			assertTrue("Did not get access denied.", output.contains(ACCESS_DENIED));
			assertNoErrMessages(output);
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - start the client which will attempt to read a file.
	 *   client.xml has no permissions defined, the ear has permissions.xml.
	 * 
	 * Expected results:
	 * - We should see the file read successfully
	 */
	@Test
	public void testReadFileWithPermissionsXML() {
		testClient = LibertyClientFactory.getLibertyClient(JAVA2_CLIENT);
		List<String> startParms = new ArrayList<String>();
		startParms.add("--");
		startParms.add(READ_FILE_OPERATION);
		startParms.add(testClient.getClientRoot() + File.separator + "test.txt");
		try {
			String fullClientXmlPath = buildFullClientConfigPath(testClient, "client_app_perms.xml");
			Log.info(c, name.getMethodName(), "Using client configuration file: " + fullClientXmlPath);
			copyNewClientConfig(fullClientXmlPath);
			Log.info(c, name.getMethodName(), "Starting the client ...");
			ProgramOutput programOutput = testClient.startClientWithArgs(true,true,true,false,"run",startParms,false);
			Log.info(c, name.getMethodName(), "Client returned.");
			String output = programOutput.getStdout();
			assertTrue("File was not successfully read.", output.contains("file read"));
			assertNoErrMessages(output);
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - start the client which will attempt to write to a file.
	 *   client.xml has no permissions defined, the ear has permissions.xml.
	 * 
	 * Expected results:
	 * - We should see the file written to successfully
	 */
	@Mode(TestMode.LITE)
	@Test
	public void testWriteFileWithPermissionsXML() {
		testClient = LibertyClientFactory.getLibertyClient(JAVA2_CLIENT);
		List<String> startParms = new ArrayList<String>();
		startParms.add("--");
		startParms.add(WRITE_FILE_OPERATION);
		startParms.add(testClient.getClientRoot() + File.separator + "test.txt");
		try {
			String fullClientXmlPath = buildFullClientConfigPath(testClient, "client_app_perms.xml");
			Log.info(c, name.getMethodName(), "Using client configuration file: " + fullClientXmlPath);
			copyNewClientConfig(fullClientXmlPath);
			Log.info(c, name.getMethodName(), "Starting the client ...");
			ProgramOutput programOutput = testClient.startClientWithArgs(true,true,true,false,"run",startParms,false);
			Log.info(c, name.getMethodName(), "Client returned.");
			String output = programOutput.getStdout();
			assertTrue("File was not successfully written.", output.contains("file written"));
			assertNoErrMessages(output);
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - start the client which will attempt to get a system property.
	 *   client.xml has no permissions defined, the ear has permissions.xml.
	 * 
	 * Expected results:
	 * - We should see the property read successfully
	 */
	@Test
	public void testGetPropertyWithPermissionsXML() {
		testClient = LibertyClientFactory.getLibertyClient(JAVA2_CLIENT);
		List<String> startParms = new ArrayList<String>();
		startParms.add("--");
		startParms.add(GET_PROPERTY_OPERATION);
		System.setProperty("bob", "bob");
		try {
			String fullClientXmlPath = buildFullClientConfigPath(testClient, "client_app_perms.xml");
			Log.info(c, name.getMethodName(), "Using client configuration file: " + fullClientXmlPath);
			copyNewClientConfig(fullClientXmlPath);
			Log.info(c, name.getMethodName(), "Starting the client ...");
			ProgramOutput programOutput = testClient.startClientWithArgs(true,true,true,false,"run",startParms,false);
			Log.info(c, name.getMethodName(), "Client returned.");
			String output = programOutput.getStdout();
			assertTrue("Property was not successfully read.", output.contains("property bob value "));
			assertNoErrMessages(output);
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - start the client which will attempt to set a system property.
	 *   client.xml has no permissions defined, the ear has permissions.xml.
	 * 
	 * Expected results:
	 * - We should see the property set successfully
	 */
	@Test
	public void testSetPropertyWithPermissionsXML() {
		testClient = LibertyClientFactory.getLibertyClient(JAVA2_CLIENT);
		List<String> startParms = new ArrayList<String>();
		startParms.add("--");
		startParms.add(SET_PROPERTY_OPERATION);
		try {
			String fullClientXmlPath = buildFullClientConfigPath(testClient, "client_app_perms.xml");
			Log.info(c, name.getMethodName(), "Using client configuration file: " + fullClientXmlPath);
			copyNewClientConfig(fullClientXmlPath);
			Log.info(c, name.getMethodName(), "Starting the client ...");
			ProgramOutput programOutput = testClient.startClientWithArgs(true,true,true,false,"run",startParms,false);
			Log.info(c, name.getMethodName(), "Client returned.");
			String output = programOutput.getStdout();
			assertTrue("Property was not successfully set.", output.contains("property bob set to: phil"));
			assertNoErrMessages(output);
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}
	/**
	 * Test description:
	 * - start the client which will attempt to read a file.
	 *   client.xml has permissions defined, the ear has no permissions.xml.
	 * 
	 * Expected results:
	 * - We should see the file read successfully
	 */
	@Test
	public void testReadFileWithClientXMLPermissions() {
		testClient = LibertyClientFactory.getLibertyClient(JAVA2_CLIENT);
		List<String> startParms = new ArrayList<String>();
		startParms.add("--");
		startParms.add(READ_FILE_OPERATION);
		startParms.add(testClient.getClientRoot() + File.separator + "test.txt");
		try {
			String fullClientXmlPath = buildFullClientConfigPath(testClient, "client_xml_perms.xml");
			Log.info(c, name.getMethodName(), "Using client configuration file: " + fullClientXmlPath);
			copyNewClientConfig(fullClientXmlPath);
			Log.info(c, name.getMethodName(), "Starting the client ...");
			ProgramOutput programOutput = testClient.startClientWithArgs(true,true,true,false,"run",startParms,false);
			Log.info(c, name.getMethodName(), "Client returned.");
			String output = programOutput.getStdout();
			assertTrue("File was not successfully read.", output.contains("file read"));
			assertNoErrMessages(output);
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - start the client which will attempt to write to a file.
	 *   client.xml has permissions defined, the ear has no permissions.xml.
	 * 
	 * Expected results:
	 * - We should see the file written to successfully
	 */
	@Test
	public void testWriteFileWithClientXMLPermissions() {
		testClient = LibertyClientFactory.getLibertyClient(JAVA2_CLIENT);
		List<String> startParms = new ArrayList<String>();
		startParms.add("--");
		startParms.add(WRITE_FILE_OPERATION);
		startParms.add(testClient.getClientRoot() + File.separator + "test.txt");
		try {
			String fullClientXmlPath = buildFullClientConfigPath(testClient, "client_xml_perms.xml");
			Log.info(c, name.getMethodName(), "Using client configuration file: " + fullClientXmlPath);
			copyNewClientConfig(fullClientXmlPath);
			Log.info(c, name.getMethodName(), "Starting the client ...");
			ProgramOutput programOutput = testClient.startClientWithArgs(true,true,true,false,"run",startParms,false);
			Log.info(c, name.getMethodName(), "Client returned.");
			String output = programOutput.getStdout();
			assertTrue("File was not successfully written.", output.contains("file written"));
			assertNoErrMessages(output);
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - start the client which will attempt to get a system property.
	 *   client.xml has permissions defined, the ear has no permissions.xml.
	 * 
	 * Expected results:
	 * - We should see the property read successfully
	 */
	@Test
	public void testGetPropertyWithClientXMLPermissions() {
		testClient = LibertyClientFactory.getLibertyClient(JAVA2_CLIENT);
		List<String> startParms = new ArrayList<String>();
		startParms.add("--");
		startParms.add(GET_PROPERTY_OPERATION);
		System.setProperty("bob", "bob");
		try {
			String fullClientXmlPath = buildFullClientConfigPath(testClient, "client_xml_perms.xml");
			Log.info(c, name.getMethodName(), "Using client configuration file: " + fullClientXmlPath);
			copyNewClientConfig(fullClientXmlPath);
			Log.info(c, name.getMethodName(), "Starting the client ...");
			ProgramOutput programOutput = testClient.startClientWithArgs(true,true,true,false,"run",startParms,false);
			Log.info(c, name.getMethodName(), "Client returned.");
			String output = programOutput.getStdout();
			assertTrue("Property was not successfully read.", output.contains("property bob value "));
			assertNoErrMessages(output);
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

	/**
	 * Test description:
	 * - start the client which will attempt to set a system property.
	 *   client.xml has permissions defined, the ear has no permissions.xml.
	 * 
	 * Expected results:
	 * - We should see the property set successfully
	 */
	@Test
	public void testSetPropertyWithClientXMLPermissions() {
		testClient = LibertyClientFactory.getLibertyClient(JAVA2_CLIENT);
		List<String> startParms = new ArrayList<String>();
		startParms.add("--");
		startParms.add(SET_PROPERTY_OPERATION);
		try {
			String fullClientXmlPath = buildFullClientConfigPath(testClient, "client_xml_perms.xml");
			Log.info(c, name.getMethodName(), "Using client configuration file: " + fullClientXmlPath);
			copyNewClientConfig(fullClientXmlPath);
			Log.info(c, name.getMethodName(), "Starting the client ...");
			ProgramOutput programOutput = testClient.startClientWithArgs(true,true,true,false,"run",startParms,false);
			Log.info(c, name.getMethodName(), "Client returned.");
			String output = programOutput.getStdout();
			assertTrue("Property was not successfully set.", output.contains("property bob set to: phil"));
			assertNoErrMessages(output);
		} catch (Exception e) {
			Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
			fail("Exception was thrown: " + e);
		}
	}

}
