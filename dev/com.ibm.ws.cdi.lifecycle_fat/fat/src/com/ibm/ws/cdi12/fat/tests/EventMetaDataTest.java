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

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * These tests verify that inspecting event meta data works correctly as per http://docs.jboss.org/cdi/spec/1.1/cdi-spec.html#event_metadata
 */

@Mode(TestMode.FULL)
@SkipForRepeat({ SkipForRepeat.EE9_FEATURES }) // Skipped temporarily to test PassivationBeanTests for sessionDatabase-1.0 feature
public class EventMetaDataTest extends LoggingTest {

    @ClassRule
    // Create the server.
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12EventMetadataServer");

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
    public static Archive<?> buildShrinkWrap() {

        WebArchive eventMetaData = ShrinkWrap.create(WebArchive.class, "eventMetaData.war");
        eventMetaData.addClass("com.ibm.ws.cdi12.test.MetaQualifier");
        eventMetaData.addClass("com.ibm.ws.cdi12.test.MetaDataServlet");
        eventMetaData.addClass("com.ibm.ws.cdi12.test.MyEvent");
        eventMetaData.addClass("com.ibm.ws.cdi12.test.RequestScopedBean");
        eventMetaData.add(new FileAsset(new File("test-applications/eventMetaData.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");
        eventMetaData.add(new FileAsset(new File("test-applications/eventMetaData.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "eventMetaData.ear");
        ear.add(new FileAsset(new File("test-applications/eventMetaData.ear/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml");
        ear.add(new FileAsset(new File("test-applications/eventMetaData.ear/resources/META-INF/application.xml")), "/META-INF/application.xml");
        ear.addAsModule(eventMetaData);

        return ear;
    }

    @Test
    public void testDefaultMetaData() throws Exception {
        WebResponse response = SHARED_SERVER.getResponse(createWebBrowserForTestCase(), "/MetaDataTest/");
        String[] splitResponse = response.getResponseBody().split("]");
        String qualifiers = splitResponse[0];
        assertTrue(qualifiers.contains("Default event qualifiers")
                   && qualifiers.contains("@javax.enterprise.inject.Any()")
                   && qualifiers.contains("@javax.enterprise.context.Initialized(value=javax.enterprise.context.RequestScoped.class)"));
        this.verifyResponse("/MetaDataTest/",
                            new String[] { "Default event injection points: null",
                                           "Default event type: interface javax.servlet.http.HttpServletRequest" });
    }

    @Test
    public void testFiredMetaData() throws Exception {
        this.verifyResponse("/MetaDataTest/",
                            new String[] { "Non-default event qualifiers: [@javax.enterprise.inject.Any(), @com.ibm.ws.cdi12.test.MetaQualifier()]",
                                           "Non-default event injection points: [BackedAnnotatedField] @Inject @MetaQualifier",
                                           "Non-default event type: class com.ibm.ws.cdi12.test.MyEvent" });
    }
}
