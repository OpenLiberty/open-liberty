/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.clientcontainer.fat;

import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.asset.FileAsset;

import componenttest.topology.impl.LibertyClientFactory;

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
