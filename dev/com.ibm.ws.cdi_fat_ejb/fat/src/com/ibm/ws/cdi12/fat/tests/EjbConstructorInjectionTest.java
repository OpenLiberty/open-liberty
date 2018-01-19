/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;

public class EjbConstructorInjectionTest extends LoggingTest {

    @ClassRule
    // Create the server and install the CDIOWB Test feature.
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12EjbConstructorInjectionServer", EjbConstructorInjectionTest.class);

    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {
        return ShrinkWrap.create(WebArchive.class, "ejbConstructorInjection.war")
                        .addClass("com.ibm.ws.cdi.ejb.constructor.test.Servlet")
                        .addClass("com.ibm.ws.cdi.ejb.constructor.test.BeanTwo")
                        .addClass("com.ibm.ws.cdi.ejb.constructor.test.BeanThree")
                        .addClass("com.ibm.ws.cdi.ejb.constructor.test.MyQualifier")
                        .addClass("com.ibm.ws.cdi.ejb.constructor.test.MyForthQualifier")
                        .addClass("com.ibm.ws.cdi.ejb.constructor.test.MyThirdQualifier")
                        .addClass("com.ibm.ws.cdi.ejb.constructor.test.Iface")
                        .addClass("com.ibm.ws.cdi.ejb.constructor.test.BeanFourWhichIsEJB")
                        .addClass("com.ibm.ws.cdi.ejb.constructor.test.MySecondQualifier")
                        .addClass("com.ibm.ws.cdi.ejb.constructor.test.BeanOne")
                        .addClass("com.ibm.ws.cdi.ejb.constructor.test.BeanEJB")
                        .addClass("com.ibm.ws.cdi.ejb.constructor.test.StaticState")
                        .add(new FileAsset(new File("test-applications/ejbConstructorInjection.war/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml")
                        .add(new FileAsset(new File("test-applications/ejbConstructorInjection.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");
    }

    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testTransientReferenceOnEjbConstructor() throws Exception {
        this.verifyResponse("/ejbConstructorInjection/Servlet", new String[] { "destroy called",
                                                                               "First bean message: foo",
                                                                               "Second bean message: bar",
                                                                               "Third bean message: spam",
                                                                               "Forth bean message: eggs" });
    }

}
