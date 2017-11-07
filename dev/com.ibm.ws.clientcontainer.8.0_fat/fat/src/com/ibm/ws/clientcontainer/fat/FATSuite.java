/*******************************************************************************
 * Copyright (c) 2014, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.fat;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                AppClientTest.class
})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

    protected static LibertyClient client = LibertyClientFactory.getLibertyClient("com.ibm.ws.clientcontainer.fat.ClientContainerClient");
    private static final String FOLDER = "apps";

    @BeforeClass
    public static void setupApps() throws Exception {

        //HelloAppClient ear
        JavaArchive jarHAC = ShrinkWrap.create(JavaArchive.class, "HelloAppClient" + ".jar");
        jarHAC.addPackages(true, "com.ibm.ws.clientcontainer.HelloAppClient.test");
        ShrinkHelper.addDirectory(jarHAC, "test-applications/HelloAppClient.jar/resources");
        EnterpriseArchive earHAC = ShrinkWrap.create(EnterpriseArchive.class, "HelloAppClient" + ".ear");
        earHAC.addAsModule(jarHAC);
        earHAC.addAsManifestResource(new File("test-applications/HelloAppClient.ear/resources/META-INF/application.xml"));
        ShrinkHelper.exportToClient(client, FOLDER, earHAC);

        //HelloAppClientNCDF ear
        JavaArchive jarHACNCDF = ShrinkWrap.create(JavaArchive.class, "HelloAppClientNCDF" + ".jar");
        jarHACNCDF.addPackages(true, "com.ibm.ws.clientcontainer.HelloAppClientNCDF.test");
        ShrinkHelper.addDirectory(jarHACNCDF, "test-applications/HelloAppClientNCDF.jar/resources");
        EnterpriseArchive earHACNCDF = ShrinkWrap.create(EnterpriseArchive.class, "HelloAppClientNCDF" + ".ear");
        earHACNCDF.addAsModule(jarHACNCDF);
        earHACNCDF.addAsManifestResource(new File("test-applications/HelloAppClientNCDF.ear/resources/META-INF/application.xml"));
        ShrinkHelper.exportToClient(client, FOLDER, earHACNCDF);

        //InAppClientContainer ear
        JavaArchive jarIACC = ShrinkWrap.create(JavaArchive.class, "InAppClientContainer" + ".jar");
        jarIACC.addPackages(true, "com.ibm.ws.clientcontainer.InAppClientContainer.test");
        ShrinkHelper.addDirectory(jarIACC, "test-applications/InAppClientContainer.jar/resources");
        EnterpriseArchive earIACC = ShrinkWrap.create(EnterpriseArchive.class, "InAppClientContainer" + ".ear");
        earIACC.addAsModule(jarIACC);
        earIACC.addAsManifestResource(new File("test-applications/InAppClientContainer.ear/resources/META-INF/application.xml"));
        ShrinkHelper.exportToClient(client, FOLDER, earIACC);

        //SystemExitClient ear
        JavaArchive jarSEC = ShrinkWrap.create(JavaArchive.class, "SystemExitClient" + ".jar");
        jarSEC.addPackages(true, "com.ibm.ws.clientcontainer.SystemExitClient.test");
        ShrinkHelper.addDirectory(jarSEC, "test-applications/SystemExitClient.jar/resources");
        EnterpriseArchive earSEC = ShrinkWrap.create(EnterpriseArchive.class, "SystemExitClient" + ".ear");
        earSEC.addAsModule(jarSEC);
        earSEC.addAsManifestResource(new File("test-applications/SystemExitClient.ear/resources/META-INF/application.xml"));
        ShrinkHelper.exportToClient(client, FOLDER, earSEC);

        //SystemExitClientNoDD ear
        JavaArchive jarSECNDD = ShrinkWrap.create(JavaArchive.class, "SystemExitClientNoDD" + ".jar");
        jarSECNDD.addPackages(true, "com.ibm.ws.clientcontainer.SystemExitClientNoDD.test");
        ShrinkHelper.addDirectory(jarSECNDD, "test-applications/SystemExitClientNoDD.jar/resources");
        EnterpriseArchive earSECNoDD = ShrinkWrap.create(EnterpriseArchive.class, "SystemExitClientNoDD" + ".ear");
        earSECNoDD.addAsModule(jarSECNDD);
        earSECNoDD.addAsManifestResource(new File("test-applications/SystemExitClientNoDD.ear/resources/META-INF/application.xml"));
        ShrinkHelper.exportToClient(client, FOLDER, earSECNoDD);

        //CallbackHandlerNoDefaultConstructor ear
        JavaArchive jarCBHNoDC = ShrinkWrap.create(JavaArchive.class, "CallbackHandlerNoDefaultConstructor" + ".jar");
        jarCBHNoDC.addPackages(true, "com.ibm.ws.clientcontainer.CallbackHandlerNoDefaultConstructor.test");
        ShrinkHelper.addDirectory(jarCBHNoDC, "test-applications/CallbackHandlerNoDefaultConstructor.jar/resources");
        EnterpriseArchive earCBHNoDC = ShrinkWrap.create(EnterpriseArchive.class, "CallbackHandlerNoDefaultConstructor" + ".ear");
        earCBHNoDC.addAsModule(jarCBHNoDC);
        earCBHNoDC.addAsManifestResource(new File("test-applications/CallbackHandlerNoDefaultConstructor.ear/resources/META-INF/application.xml"));
        ShrinkHelper.exportToClient(client, FOLDER, earCBHNoDC);
    }

}
