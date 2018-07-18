/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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

import org.junit.AfterClass;
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

import componenttest.annotation.AllowedFFDC;

public class InjectInjectionPointAsParamTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12InjectInjectionPointParamServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {

          return ShrinkWrap.create(WebArchive.class, "injectInjectionPointAsParam.war")
                        .addPackage("com.ibm.ws.fat.cdi.injectInjectionPointParam")
                        .add(new FileAsset(new File("test-applications/injectInjectionPointAsParam.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
    }

    @Test
    public void testInjectInjectionPointAsParam() throws Exception {
        SHARED_SERVER.verifyResponse(createWebBrowserForTestCase(), "/injectInjectionPointAsParam/", "passed");
    }


}
