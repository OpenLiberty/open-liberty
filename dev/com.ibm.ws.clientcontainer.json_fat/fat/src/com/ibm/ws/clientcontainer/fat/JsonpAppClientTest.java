/*******************************************************************************
 * Copyright (c) 2015,2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.fat;

import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyClientFactory;

public class JsonpAppClientTest extends AbstractAppClientTest {    
    public JsonpAppClientTest() {
        testClientName = "com.ibm.ws.clientcontainer.jsonp.fat.ClientContainerClient";
        client = LibertyClientFactory.getLibertyClient(testClientName);
    }

	@Test
	public void testJSONPClientAppClient() throws Exception {
        ShrinkHelper.exportToClient(client, "apps", FATSuite.jsonpAppClientApp);

		client.startClient();
		assertClientStartMessages();
		assertClientAppMessage("JSON-P Application Client Completed.");
	}
}
