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

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.annotation.SkipForRepeat;

@RunWith(FATRunner.class)
public class BvalAppClientTest_11 extends AbstractAppClientTest {
    
	@Test
	@SkipForRepeat(SkipForRepeat.EE9_FEATURES)
	public void testApacheBvalConfig_11_AppClient() throws Exception {
		String testClientName = "com.ibm.ws.clientcontainer.beanvalidation.fat.ApacheBvalConfig_11";
		client = LibertyClientFactory.getLibertyClient(testClientName);
		
        ShrinkHelper.exportToClient(client, "apps", FATSuite.apacheBvalConfigApp);
        
		client.startClient();
		assertClientStartMessages(testClientName);
		assertClientAppMessage("ApacheBvalConfig Application Client Completed.");
	}
	
	@Test
	@SkipForRepeat(SkipForRepeat.EE9_FEATURES)
	public void testBeanvalidation_11_AppClient() throws Exception {
		String testClientName = "com.ibm.ws.clientcontainer.beanvalidation.fat.beanvalidation_11";
		client = LibertyClientFactory.getLibertyClient(testClientName);
		
		
        ShrinkHelper.exportToClient(client, "apps", FATSuite.beanValidationApp);
        
		client.startClient();
		assertClientStartMessages(testClientName);
		assertClientAppMessage("Beanvalidation Application Client Completed.");
	}
	
	@Test
	@SkipForRepeat(SkipForRepeat.EE9_FEATURES)
	public void testBeanValidationCDI_11_AppClient() throws Exception {
		String testClientName = "com.ibm.ws.clientcontainer.beanvalidation.fat.BeanValidationCDI_11";
		client = LibertyClientFactory.getLibertyClient(testClientName);
		
		
        ShrinkHelper.exportToClient(client, "apps", FATSuite.beanValidationCDIApp);
        
		client.startClient();
		assertClientStartMessages(testClientName);
		assertClientAppMessage("BeanValidationCDI Application Client Completed.");
	}
	
	@Test
	@SkipForRepeat(SkipForRepeat.EE9_FEATURES)
	public void testDefaultbeanvalidation_11_AppClient() throws Exception {
		String testClientName = "com.ibm.ws.clientcontainer.beanvalidation.fat.defaultbeanvalidation_11";
		client = LibertyClientFactory.getLibertyClient(testClientName);
		
		
        ShrinkHelper.exportToClient(client, "apps", FATSuite.defaultBeanValidationApp);
        
		client.startClient();
		assertClientStartMessages(testClientName);
		assertClientAppMessage("Defaultbeanvalidation Application Client Completed.");
	}
	
	@Test
	@SkipForRepeat(SkipForRepeat.EE9_FEATURES)
	public void testDefaultBeanValidationCDI_11_AppClient() throws Exception {
		String testClientName = "com.ibm.ws.clientcontainer.beanvalidation.fat.DefaultBeanValidationCDI_11";
		client = LibertyClientFactory.getLibertyClient(testClientName);
		
		
        ShrinkHelper.exportToClient(client, "apps", FATSuite.defaultBeanValidationCDIApp);
        
		client.startClient();
		assertClientStartMessages(testClientName);
		assertClientAppMessage("DefaultBeanValidationCDI Application Client Completed.");
	}
}
