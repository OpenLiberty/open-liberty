/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyClientFactory;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 1.8)
public class BvalAppClientTest_20 extends AbstractAppClientTest {
	
	@Test
	public void testBeanvalidation_20_AppClient() throws Exception {
		String testClientName = "com.ibm.ws.clientcontainer.beanvalidation.fat.beanvalidation_20";
		client = LibertyClientFactory.getLibertyClient(testClientName);
		
        ShrinkHelper.exportToClient(client, "apps", FATSuite.beanValidationApp);
        
		client.startClient();
		assertClientStartMessages(testClientName);
		assertClientAppMessage("Beanvalidation Application Client Completed.");
	}
	
	@Test
	public void testBeanValidationCDI_20_AppClient() throws Exception {
		String testClientName = "com.ibm.ws.clientcontainer.beanvalidation.fat.BeanValidationCDI_20";
		client = LibertyClientFactory.getLibertyClient(testClientName);
		
        ShrinkHelper.exportToClient(client, "apps", FATSuite.beanValidationCDIApp);
        
		client.startClient();
		assertClientStartMessages(testClientName);
		assertClientAppMessage("BeanValidationCDI Application Client Completed.");
	}
	
	@Test
	public void testDefaultbeanvalidation_20_AppClient() throws Exception {
		String testClientName = "com.ibm.ws.clientcontainer.beanvalidation.fat.defaultbeanvalidation_20";
		client = LibertyClientFactory.getLibertyClient(testClientName);
		
        ShrinkHelper.exportToClient(client, "apps", FATSuite.defaultBeanValidationApp);
        
		client.startClient();
		assertClientStartMessages(testClientName);
		assertClientAppMessage("Defaultbeanvalidation Application Client Completed.");
	}
	
	@Test
	public void testDefaultBeanValidationCDI_20_AppClient() throws Exception {
		String testClientName = "com.ibm.ws.clientcontainer.beanvalidation.fat.DefaultBeanValidationCDI_20";
		client = LibertyClientFactory.getLibertyClient(testClientName);
		
        ShrinkHelper.exportToClient(client, "apps", FATSuite.defaultBeanValidationCDIApp);
        
		client.startClient();
		assertClientStartMessages(testClientName);
		assertClientAppMessage("DefaultBeanValidationCDI Application Client Completed.");
	}
}
