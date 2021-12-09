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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;


@RunWith(Suite.class)
@SuiteClasses({
	BvalAppClientTest_11.class,
	BvalAppClientTest_20.class
})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

	@ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action());

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
