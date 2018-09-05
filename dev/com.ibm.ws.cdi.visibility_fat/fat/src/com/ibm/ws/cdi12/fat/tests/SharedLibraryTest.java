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
import java.util.HashMap;
import java.util.Map;

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

/**
 * Tests for CDI from shared libraries
 */

public class SharedLibraryTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12SharedLibraryServer");

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
    public static Map<Archive,String> buildShrinkWrap() {
       Map<Archive,String> archives = new HashMap<Archive,String>();
       
       WebArchive sharedLibraryAppWeb = ShrinkWrap.create(WebArchive.class, "sharedLibraryAppWeb1.war")
                        .addClass("com.ibm.ws.cdi12.test.web1.SharedLibraryServlet");

       WebArchive sharedLibraryNoInjectionApp = ShrinkWrap.create(WebArchive.class, "sharedLibraryNoInjectionApp.war")
                        .addClass("com.ibm.ws.cdi12.test.web1.NoInjectionServlet");

       JavaArchive sharedLibrary = ShrinkWrap.create(JavaArchive.class,"sharedLibrary.jar")
                        .addClass("com.ibm.ws.cdi12.test.shared.NonInjectedHello")
                        .addClass("com.ibm.ws.cdi12.test.shared.InjectedHello");

       archives.put(sharedLibraryNoInjectionApp, "/apps");
       archives.put(sharedLibraryAppWeb, "/apps");
       archives.put(sharedLibrary, "/InjectionSharedLibrary");
       return archives;
    }

    @Test
    public void testSharedLibraryNoInjection() throws Exception {
        // Sanity check test, tests that the shared library exists and is available without CDI being involved
        this.verifyResponse("/sharedLibraryNoInjectionApp/noinjection",
                            "Hello from shared library class? :Hello from a non injected class name: Iain");

    }

    @Test
    public void testSharedLibraryWithCDI() throws Exception {

        // Now with CDI
        this.verifyResponse("/sharedLibraryAppWeb1/",
                            "Can i get to HelloC? :Hello from an InjectedHello, I am here: Iain");

    }

}
