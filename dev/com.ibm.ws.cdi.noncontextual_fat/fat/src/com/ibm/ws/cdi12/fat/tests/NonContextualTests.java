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
import com.ibm.ws.fat.util.browser.WebResponse;

public class NonContextualTests extends LoggingTest {

    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {

        WebArchive nonContextual = ShrinkWrap.create(WebArchive.class, "nonContextual.war")
                        .addClass("cdi12.noncontextual.test.Servlet")
                        .addClass("cdi12.noncontextual.test.NonContextualBean")
                        .add(new FileAsset(new File("test-applications/nonContextual.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/nonContextual.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");

        return ShrinkWrap.create(EnterpriseArchive.class,"nonContextual.ear")
                        .add(new FileAsset(new File("test-applications/nonContextual.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(nonContextual);
    }

    @ClassRule
    // Create the server.
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("nonContextualServer");

    /** {@inheritDoc} */
    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        // TODO Auto-generated method stub
        return SHARED_SERVER;
    }

    @Test
    public void testNonContextualBean() throws Exception {
        //CreatedViaAnnotations
        verifyResponse("/noncontextual/", "42");
    }

}
