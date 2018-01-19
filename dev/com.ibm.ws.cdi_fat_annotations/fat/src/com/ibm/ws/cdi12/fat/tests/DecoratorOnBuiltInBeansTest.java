/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
import com.ibm.ws.fat.util.browser.WebBrowser;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * These tests checks to see if you can use a Decorator on one of the
 * built in beans
 */
@Mode(TestMode.FULL)
public class DecoratorOnBuiltInBeansTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12DecoratorOnBuiltInBeansTestServer");

    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {

          return ShrinkWrap.create(WebArchive.class, "defaultDecoratorApp.war")
                        .addClass("com.ibm.ws.cdi12.test.defaultdecorator.ConversationDecorator")
                        .addClass("com.ibm.ws.cdi12.test.defaultdecorator.DefaultDecoratorServlet")
                        .add(new FileAsset(new File("test-applications/defaultDecoratorApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
    }

    /** {@inheritDoc} */
    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        // TODO Auto-generated method stub
        return SHARED_SERVER;
    }

    @Test
    public void testBeanWasFound() throws Exception {
        verifyResponse("/defaultDecoratorApp/", "decorating");
    }

}
