/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
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

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class DisablingBeansXmlValidationTest extends LoggingTest {

    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {
        return ShrinkWrap.create(WebArchive.class,
                                 "invalidBeansXml.war").addClass("com.ibm.ws.cdi12.test.TestServlet").addClass("com.ibm.ws.cdi12.test.TestBean").add(new FileAsset(new File("test-applications/invalidBeansXml.war/resources/WEB-INF/web.xml")),
                                                                                                                                                     "/WEB-INF/web.xml").add(new FileAsset(new File("test-applications/invalidBeansXml.war/resources/WEB-INF/beans.xml")),
                                                                                                                                                                             "/WEB-INF/beans.xml");
    }

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12NoBeansXmlValidationServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testDisablingBeansXmlValidation() throws Exception {
        this.verifyResponse("/invalidBeansXml/TestServlet", "PASSED");
    }
}
