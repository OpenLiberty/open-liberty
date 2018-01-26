/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import java.io.File;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

public class ValidatorInJarTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12ValidatorInJarServer");

    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {

        JavaArchive TestValidatorInJar = ShrinkWrap.create(JavaArchive.class,"TestValidatorInJar.jar")
                        .addClass("com.ibm.cdi.test.basic.injection.jar.AppScopedBean");

        WebArchive TestValidatorInJarWar = ShrinkWrap.create(WebArchive.class, "TestValidatorInJar.war")
                        .addClass("com.ibm.cdi.test.basic.injection.WebServ")
                        .addAsManifestResource(new File("test-applications/TestValidatorInJar.war/resources/META-INF/MANIFEST.MF"))
                        .add(new FileAsset(new File("test-applications/TestValidatorInJar.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/TestValidatorInJar.war/resources/WEB-INF/lib/jaxrs-analyzer-0.9.jar")), "/WEB-INF/lib/jaxrs-analyzer-0.9.jar");

        return ShrinkWrap.create(EnterpriseArchive.class,"TestValidatorInJar.ear")
                        .add(new FileAsset(new File("test-applications/TestValidatorInJar.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(TestValidatorInJarWar)
                        .addAsModule(TestValidatorInJar);
    }

    /** {@inheritDoc} */
    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        // TODO Auto-generated method stub
        return SHARED_SERVER;
    }

    @Test
    @Mode(TestMode.FULL)
    public void testValidatorInJar() throws Exception {
        //If the application has started correctly the test passes.
        verifyResponse("/TestValidatorInJar/testservlet", "App Scoped Hello World");
    }

}
