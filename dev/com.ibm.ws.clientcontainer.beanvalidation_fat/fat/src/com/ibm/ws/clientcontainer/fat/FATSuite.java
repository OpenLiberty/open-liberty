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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.junit.BeforeClass;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.AlwaysPassesTest;


@RunWith(Suite.class)
@SuiteClasses({
    AlwaysPassesTest.class,
	BvalAppClientTest_11.class,
	BvalAppClientTest_20.class
})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

	public static EnterpriseArchive apacheBvalConfigApp;
	public static EnterpriseArchive beanValidationApp;
	public static EnterpriseArchive beanValidationCDIApp;
	public static EnterpriseArchive defaultBeanValidationApp;
	public static EnterpriseArchive defaultBeanValidationCDIApp;
	
	@BeforeClass
	public static void setupApps() throws Exception {
		
		// ApacheBvalConfig app
		JavaArchive apacheBvalConfigJar = ShrinkHelper.buildJavaArchive("ApacheBvalConfig.jar", "apachebvalconfig.*");
		apacheBvalConfigApp = ShrinkWrap.create(EnterpriseArchive.class, "ApacheBvalConfig.ear");
		apacheBvalConfigApp.addAsModule(apacheBvalConfigJar);
        ShrinkHelper.addDirectory(apacheBvalConfigApp, "test-applications/ApacheBvalConfig.ear/resources");
	
        // beanvalidation app
        JavaArchive beanValidationJar = ShrinkHelper.buildJavaArchive("beanvalidation.jar", "beanvalidation.*");
		beanValidationApp = ShrinkWrap.create(EnterpriseArchive.class, "beanvalidation.ear");
		beanValidationApp.addAsModule(beanValidationJar);
        ShrinkHelper.addDirectory(beanValidationApp, "test-applications/beanvalidation.ear/resources");
        
        // BeanValidationCDI app
        JavaArchive beanValidationCDIJar = ShrinkHelper.buildJavaArchive("BeanValidationCDI.jar", "beanvalidationcdi.*");
		beanValidationCDIApp = ShrinkWrap.create(EnterpriseArchive.class, "BeanValidationCDI.ear");
		beanValidationCDIApp.addAsModule(beanValidationCDIJar);
        ShrinkHelper.addDirectory(beanValidationCDIApp, "test-applications/BeanValidationCDI.ear/resources");
        
        // defaultbeanvalidation app
        JavaArchive defaultBeanValidationJar = ShrinkHelper.buildJavaArchive("defaultbeanvalidation.jar", "defaultbeanvalidation.*");
        defaultBeanValidationApp = ShrinkWrap.create(EnterpriseArchive.class, "defaultbeanvalidation.ear");
        defaultBeanValidationApp.addAsModule(defaultBeanValidationJar);
        ShrinkHelper.addDirectory(defaultBeanValidationApp, "test-applications/defaultbeanvalidation.ear/resources");
	
        // DefaultBeanValidationCDI app
        JavaArchive defaultBeanValidationCDIJar = ShrinkHelper.buildJavaArchive("DefaultBeanValidationCDI.jar", "defaultbeanvalidationcdi.*");
        defaultBeanValidationCDIApp = ShrinkWrap.create(EnterpriseArchive.class, "DefaultBeanValidationCDI.ear");
        defaultBeanValidationCDIApp.addAsModule(defaultBeanValidationCDIJar);
        ShrinkHelper.addDirectory(defaultBeanValidationCDIApp, "test-applications/DefaultBeanValidationCDI.ear/resources");
	}
}
