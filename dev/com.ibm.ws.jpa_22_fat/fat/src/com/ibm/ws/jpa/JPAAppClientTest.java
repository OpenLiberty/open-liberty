/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa;

import java.io.File;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ClientConfiguration;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;

@RunWith(FATRunner.class)
public class JPAAppClientTest {
    private static final String APPCLIENT_ROOT = "test-applications/appclient";
    private final static String CLIENT_NAME = "JPAAppClient";
    private final static String PKG_ROOT = "jpaappcli";

    protected static LibertyClient client = LibertyClientFactory.getLibertyClient(CLIENT_NAME);

    @BeforeClass
    public static void setUp() throws Exception {

    }

    @AfterClass
    public static void tearDown() throws Exception {

    }

    @Test
    public void testAppClientFieldInjection() throws Exception {
        final String appName = "AppCliJPAFieldInj";
        final String resRoot = APPCLIENT_ROOT + "/" + appName + ".ear";

        final JavaArchive appCliJar = ShrinkWrap.create(JavaArchive.class, appName + ".jar");
        appCliJar.addPackage(PKG_ROOT + ".client.fieldinjection");
        appCliJar.addPackage(PKG_ROOT + ".entity");
        ShrinkHelper.addDirectory(appCliJar, resRoot + "/" + appName + ".jar");

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, appName + ".ear");
        app.addAsModule(appCliJar);
        app.setApplicationXML(new File(resRoot + "/META-INF/application.xml"));

        ShrinkHelper.exportToClient(client, "apps", app);
        client.addInstalledAppForValidation(appName);

        Application appRecord = new Application();
        appRecord.setLocation(appName + ".ear");
        appRecord.setName(appName);

        ClientConfiguration cc = client.getClientConfiguration();
        cc.getApplications().add(appRecord);
        client.updateClientConfiguration(cc);
        client.saveClientConfiguration();

        client.startClient();
        client.waitForStringInCopiedLog("CWWKE0908I");
        List<String> jpaCliStrings = client.findStringsInCopiedLogs("JPACLI");
        boolean foundPass = false;
        boolean foundFail = false;
        if (jpaCliStrings != null && jpaCliStrings.size() > 0) {
            for (String s : jpaCliStrings) {
                if (s.indexOf("JPACLI: PASSED:") > -1) {
                    foundPass = true;
                }
                if (s.indexOf("JPACLI: FAILED:") > -1) {
                    foundFail = true;
                }
            }
        }

        cc = client.getClientConfiguration();
        cc.getApplications().clear();;
        client.updateClientConfiguration(cc);
        client.saveClientConfiguration();

        if (foundFail) {
            Assert.fail("Observed failure messages in the client log.");
        }
        if (foundPass) {
            System.out.println("Found PASS eyecatcher in the client log.");
        } else {
            Assert.fail("Observed no PASS messages in the client log.");
        }
    }

    @Test
    public void testAppClientMethodInjection() throws Exception {
        final String appName = "AppCliJPAMethodInj";
        final String resRoot = APPCLIENT_ROOT + "/" + appName + ".ear";

        final JavaArchive appCliJar = ShrinkWrap.create(JavaArchive.class, appName + ".jar");
        appCliJar.addPackage(PKG_ROOT + ".client.methodinjection");
        appCliJar.addPackage(PKG_ROOT + ".entity");
        ShrinkHelper.addDirectory(appCliJar, resRoot + "/" + appName + ".jar");

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, appName + ".ear");
        app.addAsModule(appCliJar);
        app.setApplicationXML(new File(resRoot + "/META-INF/application.xml"));

        ShrinkHelper.exportToClient(client, "apps", app);
        client.addInstalledAppForValidation(appName);

        Application appRecord = new Application();
        appRecord.setLocation(appName + ".ear");
        appRecord.setName(appName);

        ClientConfiguration cc = client.getClientConfiguration();
        cc.getApplications().add(appRecord);
        client.updateClientConfiguration(cc);
        client.saveClientConfiguration();

        client.startClient();
        client.waitForStringInCopiedLog("CWWKE0908I");
        List<String> jpaCliStrings = client.findStringsInCopiedLogs("JPACLI");
        boolean foundPass = false;
        boolean foundFail = false;
        if (jpaCliStrings != null && jpaCliStrings.size() > 0) {
            for (String s : jpaCliStrings) {
                if (s.indexOf("JPACLI: PASSED:") > -1) {
                    foundPass = true;
                }
                if (s.indexOf("JPACLI: FAILED:") > -1) {
                    foundFail = true;
                }
            }
        }

        cc = client.getClientConfiguration();
        cc.getApplications().clear();;
        client.updateClientConfiguration(cc);
        client.saveClientConfiguration();

        if (foundFail) {
            Assert.fail("Observed failure messages in the client log.");
        }
        if (foundPass) {
            System.out.println("Found PASS eyecatcher in the client log.");
        } else {
            Assert.fail("Observed no PASS messages in the client log.");
        }
    }

    @Test
    public void testAppClientMethodJNDI() throws Exception {
        final String appName = "AppCliJPAJNDIInj";
        final String resRoot = APPCLIENT_ROOT + "/" + appName + ".ear";

        final JavaArchive appCliJar = ShrinkWrap.create(JavaArchive.class, appName + ".jar");
        appCliJar.addPackage(PKG_ROOT + ".client.jndiinjection");
        appCliJar.addPackage(PKG_ROOT + ".entity");
        ShrinkHelper.addDirectory(appCliJar, resRoot + "/" + appName + ".jar");

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, appName + ".ear");
        app.addAsModule(appCliJar);
        app.setApplicationXML(new File(resRoot + "/META-INF/application.xml"));

        ShrinkHelper.exportToClient(client, "apps", app);
        client.addInstalledAppForValidation(appName);

        Application appRecord = new Application();
        appRecord.setLocation(appName + ".ear");
        appRecord.setName(appName);

        ClientConfiguration cc = client.getClientConfiguration();
        cc.getApplications().add(appRecord);
        client.updateClientConfiguration(cc);
        client.saveClientConfiguration();

        client.startClient();
        client.waitForStringInCopiedLog("CWWKE0908I");
        List<String> jpaCliStrings = client.findStringsInCopiedLogs("JPACLI");
        boolean foundPass = false;
        boolean foundFail = false;
        if (jpaCliStrings != null && jpaCliStrings.size() > 0) {
            for (String s : jpaCliStrings) {
                if (s.indexOf("JPACLI: PASSED:") > -1) {
                    foundPass = true;
                }
                if (s.indexOf("JPACLI: FAILED:") > -1) {
                    foundFail = true;
                }
            }
        }

        cc = client.getClientConfiguration();
        cc.getApplications().clear();;
        client.updateClientConfiguration(cc);
        client.saveClientConfiguration();

        if (foundFail) {
            Assert.fail("Observed failure messages in the client log.");
        }
        if (foundPass) {
            System.out.println("Found PASS eyecatcher in the client log.");
        } else {
            Assert.fail("Observed no PASS messages in the client log.");
        }
    }
}
