/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;


import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

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
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@Mode(TestMode.FULL)
public class InjectInjectionPointTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12InjectInjectionPointServer");

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

          return ShrinkWrap.create(WebArchive.class, "injectInjectionPoint.war")
                        .addClass("com.ibm.ws.fat.cdi.injectInjectionPoint.EmptyBean")
                        .addClass("com.ibm.ws.fat.cdi.injectInjectionPoint.InjectInjectionPointServlet");
    }

    @Test
    @AllowedFFDC("com.ibm.ws.container.service.state.StateChangeException")
    public void testInjectInjectionPoint() throws Exception {
                List<String> logs = SHARED_SERVER.getLibertyServer().findStringsInLogs("CWWKZ0002E(?=.*injectInjectionPoint)(?=.*com.ibm.ws.container.service.state.StateChangeException)(?=.*org.jboss.weld.exceptions.DefinitionException)(?=.*WELD-001405)(?=.*BackedAnnotatedField)(?=.*com.ibm.ws.fat.cdi.injectInjectionPoint.InjectInjectionPointServlet.thisShouldFail)");
        assertEquals("DefinitionException not found", 1, logs.size()); //Unlike the two sibling tests this only emits the message once. 
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (SHARED_SERVER != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            /*
             * Ignore CWWKZ0002E which is an error while starting an application
             */
            SHARED_SERVER.getLibertyServer().stopServer("CWWKZ0002E");
        }
    }

}
