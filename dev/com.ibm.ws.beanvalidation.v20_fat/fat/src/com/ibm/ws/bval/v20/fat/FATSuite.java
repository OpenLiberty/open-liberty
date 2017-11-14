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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.bval.v20.fat.BasicValidation11Test;
import com.ibm.ws.bval.v20.fat.BeanValidationCDITest;
import com.ibm.ws.bval.v20.fat.EJBModule11Test;
import com.ibm.ws.bval.v20.fat.BeanVal20Test;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({
        BasicValidation11Test.class,
        EJBModule11Test.class,
        BeanValidationCDITest.class,
        BeanVal20Test.class
})

public class FATSuite {
    private static LibertyServer server1 = LibertyServerFactory.getLibertyServer("com.ibm.ws.beanvalidation_1.1.fat");
    private static LibertyServer server2 = LibertyServerFactory.getLibertyServer("com.ibm.ws.beanvalidation.cdi.fat");

    public static final String HIBERNATE_BVAL_CONFIG10 = "HibernateBvalConfig_10";
    public static final String HIBERNATE_BVAL_CONFIG11 = "HibernateBvalConfig_11";
    public static final String BEAN_VALIDATION10 = "beanvalidation_10";
    public static final String BEAN_VALIDATION11 = "beanvalidation_11";
    public static final String DEFAULT_BEAN_VALIDATION10 = "defaultbeanvalidation_10";
    public static final String DEFAULT_BEAN_VALIDATION11 = "defaultbeanvalidation_11";
    private static final String FOLDER = "dropins";

    @BeforeClass
    public static void createAndExportWARs() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, HIBERNATE_BVAL_CONFIG10 + ".war");
        war.addPackage("beanvalidation.hibernatebvalconfig10.web");
        war.addAsWebInfResource(new File("test-applications/" + HIBERNATE_BVAL_CONFIG10 + ".war/resources/WEB-INF/validation.xml"));
        war.addAsManifestResource(new File("test-applications/" + HIBERNATE_BVAL_CONFIG10 + ".war/resources/META-INF/permissions.xml"));
        exportToServers(war);

        war = ShrinkWrap.create(WebArchive.class, HIBERNATE_BVAL_CONFIG11 + ".war");
        war.addPackage("beanvalidation.hibernatebvalconfig11.web");
        war.addAsWebInfResource(new File("test-applications/" + HIBERNATE_BVAL_CONFIG11 + ".war/resources/WEB-INF/validation.xml"));
        war.addAsManifestResource(new File("test-applications/" + HIBERNATE_BVAL_CONFIG11 + ".war/resources/META-INF/permissions.xml"));
        exportToServers(war);

        war = ShrinkWrap.create(WebArchive.class, BEAN_VALIDATION10 + ".war");
        war.addPackage("beanvalidation10");
        war.addPackage("beanvalidation10.web");
        war.addPackage("beanvalidation10.web.beans");
        war.addAsWebInfResource(new File("test-applications/" + BEAN_VALIDATION10 + ".war/resources/WEB-INF/validation.xml"));
        war.addAsWebInfResource(new File("test-applications/" + BEAN_VALIDATION10 + ".war/resources/WEB-INF/web.xml"));
        war.addAsWebInfResource(new File("test-applications/" + BEAN_VALIDATION10 + ".war/resources/WEB-INF/constraints-house.xml"));
        war.addAsManifestResource(new File("test-applications/" + BEAN_VALIDATION10 + ".war/resources/META-INF/permissions.xml"));
        exportToServers(war);

        war = ShrinkWrap.create(WebArchive.class, BEAN_VALIDATION11 + ".war");
        war.addPackage("beanvalidation11");
        war.addPackage("beanvalidation11.web");
        war.addPackage("beanvalidation11.web.beans");
        war.addAsWebInfResource(new File("test-applications/" + BEAN_VALIDATION11 + ".war/resources/WEB-INF/validation.xml"));
        war.addAsWebInfResource(new File("test-applications/" + BEAN_VALIDATION11 + ".war/resources/WEB-INF/web.xml"));
        war.addAsWebInfResource(new File("test-applications/" + BEAN_VALIDATION11 + ".war/resources/WEB-INF/constraints-house.xml"));
        war.addAsManifestResource(new File("test-applications/" + BEAN_VALIDATION11 + ".war/resources/META-INF/permissions.xml"));
        exportToServers(war);

        war = ShrinkWrap.create(WebArchive.class, DEFAULT_BEAN_VALIDATION10 + ".war");
        war.addPackage("defaultbeanvalidation10.web");
        war.addPackage("defaultbeanvalidation10.web.beans");
        war.addAsWebInfResource(new File("test-applications/" + DEFAULT_BEAN_VALIDATION10 + ".war/resources/WEB-INF/web.xml"));
        war.addAsManifestResource(new File("test-applications/" + DEFAULT_BEAN_VALIDATION10 + ".war/resources/META-INF/permissions.xml"));
        exportToServers(war);

        war = ShrinkWrap.create(WebArchive.class, DEFAULT_BEAN_VALIDATION11 + ".war");
        war.addPackage("defaultbeanvalidation11.web");
        war.addPackage("defaultbeanvalidation11.web.beans");
        war.addAsWebInfResource(new File("test-applications/" + DEFAULT_BEAN_VALIDATION11 + ".war/resources/WEB-INF/web.xml"));
        war.addAsManifestResource(new File("test-applications/" + DEFAULT_BEAN_VALIDATION11 + ".war/resources/META-INF/permissions.xml"));
        exportToServers(war);
    }

    /**
     * @param war
     * @throws Exception
     */
    private static void exportToServers(WebArchive war) throws Exception {
        ShrinkHelper.exportToServer(server1, FOLDER, war);
        ShrinkHelper.exportToServer(server2, FOLDER, war);
    }

}
