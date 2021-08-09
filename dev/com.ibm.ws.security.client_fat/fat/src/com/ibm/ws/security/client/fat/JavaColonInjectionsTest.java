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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JavaColonInjectionsTest {
	private static Class<?> c = JavaColonInjectionsTest.class;

	@Rule
	public TestName testName = new TestName();

	private static LibertyClient client = LibertyClientFactory.getLibertyClient("javacolonClientInjection");
	private static LibertyServer server = LibertyServerFactory.getLibertyServer("javacolonServerInjection");

	@BeforeClass
	public static void beforeClass() throws Exception {
		CommonTest.transformApps(server);
		ProgramOutput po = server.startServer();
		assertEquals("server did not start correctly", 0, po.getReturnCode());

		CommonTest.transformApps(client);
		client.startClient();
	}

	@AfterClass
	public static void afterClass() throws Exception {
		server.stopServer("CWWKG0033W", "CWWKZ0124E: Application testmarker does not contain any modules.");
	}

	private void check() throws Exception {
		String methodName = testName.getMethodName();
		int idx = -1;
		if ((idx = methodName.indexOf("_EE9_FEATURES")) != -1) {
		    methodName = methodName.substring(0, idx);
		}
		List<String> strings = client.findStringsInCopiedLogs(methodName+"-PASSED");
		Log.info(c, methodName, "Found in logs: " + strings);
		assertTrue("Did not find expected method message " + methodName, strings != null && strings.size() >= 1);

		// now explicitly check that we do not see messages like this in the log output:
		// W CWNEN0057W: The com.ibm.ws.clientcontainer.security.fat.InjectionClientMain.mailSessionComp injection target must not be declared static.
		// The spec says that injection into client modules MUST be static - this message indicates a bug in how injection is
		// processed - usually by the client container injection runtime or CDI.
		strings = client.findStringsInCopiedLogs("CWNEN0057W");
		assertTrue("Invalid warning message about injecting into static field", 0 == strings.size());
	}

	//////////////////////////////////////////////////////
	// java:global

	/**
	 * Tests that a remote EJB is injected when using a
	 * java:global lookup.
	 */
	@Mode(TestMode.LITE)
	@Test
	public void injectGlobal_EJB() throws Exception {
		check();
	}

}
