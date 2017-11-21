/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation.fat;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.beanvalidation.fat.tests.BasicValidation11Test;
import com.ibm.ws.beanvalidation.fat.tests.BasicValidation20Test;
import com.ibm.ws.beanvalidation.fat.tests.BeanValidation11CDITest;
import com.ibm.ws.beanvalidation.fat.tests.BeanValidation20CDITest;
import com.ibm.ws.beanvalidation.fat.tests.EJBModule11Test;
import com.ibm.ws.beanvalidation.fat.tests.EJBModule20Test;

import componenttest.topology.impl.LibertyServer;

@RunWith(Suite.class)
@SuiteClasses({
                BasicValidation11Test.class,
                EJBModule11Test.class,
                BeanValidation11CDITest.class,
                BasicValidation20Test.class,
                EJBModule20Test.class,
                BeanValidation20CDITest.class
})

public class FATSuite {

    public static final String APACHE_BVAL_CONFIG10 = "ApacheBvalConfig_10";
    public static final String APACHE_BVAL_CONFIG11 = "ApacheBvalConfig_11";
    public static final String BEAN_VALIDATION10 = "beanvalidation_10";
    public static final String BEAN_VALIDATION11 = "beanvalidation_11";
    public static final String DEFAULT_BEAN_VALIDATION10 = "defaultbeanvalidation_10";
    public static final String DEFAULT_BEAN_VALIDATION11 = "defaultbeanvalidation_11";
    private static final String FOLDER = "dropins";

    public static void createAndExportCommonWARs(LibertyServer server) throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, BEAN_VALIDATION10 + ".war");
        war.addPackage("beanvalidation10");
        war.addPackage("beanvalidation10.web");
        war.addPackage("beanvalidation10.web.beans");
        war.addAsWebInfResource(new File("test-applications/" + BEAN_VALIDATION10 + ".war/resources/WEB-INF/validation.xml"));
        war.addAsWebInfResource(new File("test-applications/" + BEAN_VALIDATION10 + ".war/resources/WEB-INF/web.xml"));
        war.addAsWebInfResource(new File("test-applications/" + BEAN_VALIDATION10 + ".war/resources/WEB-INF/constraints-house.xml"));
        war.addAsManifestResource(new File("test-applications/" + BEAN_VALIDATION10 + ".war/resources/META-INF/permissions.xml"));
        ShrinkHelper.exportToServer(server, FOLDER, war);

        war = ShrinkWrap.create(WebArchive.class, BEAN_VALIDATION11 + ".war");
        war.addPackage("beanvalidation11");
        war.addPackage("beanvalidation11.web");
        war.addPackage("beanvalidation11.web.beans");
        war.addAsWebInfResource(new File("test-applications/" + BEAN_VALIDATION11 + ".war/resources/WEB-INF/validation.xml"));
        war.addAsWebInfResource(new File("test-applications/" + BEAN_VALIDATION11 + ".war/resources/WEB-INF/web.xml"));
        war.addAsWebInfResource(new File("test-applications/" + BEAN_VALIDATION11 + ".war/resources/WEB-INF/constraints-house.xml"));
        war.addAsManifestResource(new File("test-applications/" + BEAN_VALIDATION11 + ".war/resources/META-INF/permissions.xml"));
        ShrinkHelper.exportToServer(server, FOLDER, war);

        war = ShrinkWrap.create(WebArchive.class, DEFAULT_BEAN_VALIDATION10 + ".war");
        war.addPackage("defaultbeanvalidation10.web");
        war.addPackage("defaultbeanvalidation10.web.beans");
        war.addAsWebInfResource(new File("test-applications/" + DEFAULT_BEAN_VALIDATION10 + ".war/resources/WEB-INF/web.xml"));
        war.addAsManifestResource(new File("test-applications/" + DEFAULT_BEAN_VALIDATION10 + ".war/resources/META-INF/permissions.xml"));
        ShrinkHelper.exportToServer(server, FOLDER, war);

        war = ShrinkWrap.create(WebArchive.class, DEFAULT_BEAN_VALIDATION11 + ".war");
        war.addPackage("defaultbeanvalidation11.web");
        war.addPackage("defaultbeanvalidation11.web.beans");
        war.addAsWebInfResource(new File("test-applications/" + DEFAULT_BEAN_VALIDATION11 + ".war/resources/WEB-INF/web.xml"));
        war.addAsManifestResource(new File("test-applications/" + DEFAULT_BEAN_VALIDATION11 + ".war/resources/META-INF/permissions.xml"));
        ShrinkHelper.exportToServer(server, FOLDER, war);
    }

    public static void createAndExportApacheWARs(LibertyServer server) throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APACHE_BVAL_CONFIG10 + ".war");
        war.addPackage("beanvalidation.apachebvalconfig10.web");
        war.addAsWebInfResource(new File("test-applications/" + APACHE_BVAL_CONFIG10 + ".war/resources/WEB-INF/validation.xml"));
        war.addAsManifestResource(new File("test-applications/" + APACHE_BVAL_CONFIG10 + ".war/resources/META-INF/permissions.xml"));
        ShrinkHelper.exportToServer(server, FOLDER, war);

        war = ShrinkWrap.create(WebArchive.class, APACHE_BVAL_CONFIG11 + ".war");
        war.addPackage("beanvalidation.apachebvalconfig11.web");
        war.addAsWebInfResource(new File("test-applications/" + APACHE_BVAL_CONFIG11 + ".war/resources/WEB-INF/validation.xml"));
        war.addAsManifestResource(new File("test-applications/" + APACHE_BVAL_CONFIG11 + ".war/resources/META-INF/permissions.xml"));
        ShrinkHelper.exportToServer(server, FOLDER, war);
    }

    public static void createAndExportCDIWARs(LibertyServer server) throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "BeanValidationCDI_11" + ".war");
        war.addPackage("beanvalidation.cdi.beans");
        war.addPackage("beanvalidation.cdi.web");
        war.addPackage("beanvalidation.cdi.validation");
        war.addAsWebInfResource(new File("test-applications/BeanValidationCDI_11.war/resources/WEB-INF/beans.xml"));
        war.addAsDirectory("WEB-INF/classes/META-INF");
        war.addAsResource(new File("test-applications/BeanValidationCDI_11.war/resources/WEB-INF/classes/META-INF/validation.xml"), "META-INF/validation.xml");
        ShrinkHelper.exportToServer(server, FOLDER, war);

        war = ShrinkWrap.create(WebArchive.class, "DefaultBeanValidationCDI_11" + ".war");
        war.addPackage("defaultbeanvalidation.cdi.beans");
        war.addPackage("defaultbeanvalidation.cdi.web");
        war.addPackage("defaultbeanvalidation.cdi.validation");
        war.addAsWebInfResource(new File("test-applications/DefaultBeanValidationCDI_11.war/resources/WEB-INF/beans.xml"));
        ShrinkHelper.exportToServer(server, FOLDER, war);
    }

    public static void createAndExportEJBWARs(LibertyServer server) throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "EJBModule1EJB" + ".jar");
        jar.addPackage("beanvalidation.ejbmodule.ejb");
        jar.addPackage("beanvalidation.ejbmodule");
        ShrinkHelper.addDirectory(jar, "test-applications/EJBModule1EJB.jar/resources");

        WebArchive war = ShrinkWrap.create(WebArchive.class, "EJBModuleWeb" + ".war");
        war.addPackage("beanvalidation.web");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "OneEJBModuleApp" + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(jar);
        ear.addAsManifestResource(new File("test-applications/OneEJBModuleApp.ear/resources/META-INF/application.xml"));
        ShrinkHelper.exportToServer(server, FOLDER, ear);

        JavaArchive jar2 = ShrinkWrap.create(JavaArchive.class, "EJBModule2EJB" + ".jar");
        jar2.addPackage("beanvalidation.ejbmodule2.ejb");
        ShrinkHelper.addDirectory(jar2, "test-applications/EJBModule2EJB.jar/resources");

        EnterpriseArchive ear2 = ShrinkWrap.create(EnterpriseArchive.class, "TwoEJBModulesApp" + ".ear");
        ear2.addAsModule(war);
        ear2.addAsModule(jar);
        ear2.addAsModule(jar2);
        ear2.addAsManifestResource(new File("test-applications/TwoEJBModulesApp.ear/resources/META-INF/application.xml"));
        ShrinkHelper.exportToServer(server, FOLDER, ear2);
    }
}
