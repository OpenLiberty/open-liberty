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
package com.ibm.ws.clientcontainer.javamail.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;

@RunWith(FATRunner.class)
public class JavaMailAppClientTest {

    static final Class<?> c = JavaMailAppClientTest.class;
    static final String CLIENT_NAME = "javaMailClient";
    static final String EAR_NAME = "JavaMailClientEAR.ear";
    static final LibertyClient client = LibertyClientFactory.getLibertyClient(CLIENT_NAME);

    static GreenMail greenMailTestServer;
    static final int smtpPort = Integer.getInteger("smtp_port");
    static final int imapPort = Integer.getInteger("imap_port");
    static final String mailUserAddress = "user@testserver.com";
    static final String mailUserPass = "userPass";

    // The client test application will be launched and will complete by the time we exit this method (or will
    // fail to launch in which case the entire test fails).
    // Other tests in this class simply examine the output logs for various strings that the client will emit when
    // some piece of functionality is tested and passes in the client.
    @BeforeClass
    public static void beforeClass() throws Exception {
        JavaArchive jar = ShrinkHelper.buildJavaArchive("JavaMailClientApp.jar", "com.ibm.ws.javamail.client");
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME)
                        .addAsModule(jar);
        ShrinkHelper.addDirectory(ear, "test-applications/" + EAR_NAME + "/resources");
        ShrinkHelper.exportToClient(client, "apps", ear);

        //define smtp and imap services and start GreenMail
        ServerSetup imapSetup = new ServerSetup(imapPort, "localhost", "imap");
        ServerSetup smtpSetup = new ServerSetup(smtpPort, "localhost", "smtp");
        greenMailTestServer = new GreenMail(new ServerSetup[] { imapSetup, smtpSetup });
        greenMailTestServer.start();
        //define greenMail user.
        greenMailTestServer.setUser(mailUserAddress, mailUserPass);

        // Set up JVM options and launch client
        Map<String, String> jvmOptions = client.getJvmOptionsAsMap();
        jvmOptions.put("-Dcom.ibm.ws.javaMailClient.fat.smtp.port", Integer.toString(smtpPort));
        jvmOptions.put("-Dcom.ibm.ws.javaMailClient.fat.imap.port", Integer.toString(imapPort));
        client.setJvmOptions(jvmOptions);
        client.startClient();

        // Verify that test app started
        assertNotNull("FAIL: Did not receive application started message:CWWKZ0001I",
                      client.waitForStringInCopiedLog("CWWKZ0001I:.*JavaMailClientEAR"));
    }

    @AfterClass
    public static void afterClass() {
        greenMailTestServer.stop();
    }

    /**
     * Check for basic send(SMTP)/receive(IMAP) of email using a session object created programatically.
     */
    @Test
    public void testClientJavaMailSendReceive() throws Exception {
        // check for messages from the 3 main test scenarious that are
        //Check message.log for mail sent and received messages
        int matches = client.findStringsInCopiedLogs(".*testBasicSendReceive.*Application client received email with expected 'From' and 'Subject' headers.*").size();
        assertEquals("Did not find expected number of messages in message.log", 1, matches);
    }

    @Test
    public void testClientJavaMailSendReceiveUsingAnnotationDefinition() throws Exception {
        int matches = client.findStringsInCopiedLogs(".*testInjectionFromAnnotationDefinition.*Application client received email with expected 'From' and 'Subject' headers.*")
                        .size();
        assertEquals("Did not find expected number of messages in message.log", 1, matches);
    }

    @Test
    public void testClientJavaMailSendReceiveUsingAnnotationDescriptor() throws Exception {
        int matches = client.findStringsInCopiedLogs(".*testInjectionFromDescriptorDefinition.*Application client received email with expected 'From' and 'Subject' headers.*")
                        .size();
        assertEquals("Did not find expected number of messages in message.log", 1, matches);
    }
}
