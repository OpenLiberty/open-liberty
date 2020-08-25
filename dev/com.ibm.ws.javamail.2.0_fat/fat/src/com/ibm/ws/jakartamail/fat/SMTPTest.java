/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jakartamail.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class SMTPTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("mailSessionTestServer");
    private final Class<?> c = SMTPTest.class;
    // Creates a local GreenMail server, but set to null for now. It is full defined in the
    // BeforeClass method
    private static GreenMail smtpServer = null;

    /**
     * TestDescription:
     * This test is test the ability for the javaMail-1.6 feature to have a jakarta.mail.Session
     * object defined on the SMTPInlineServlet. This test the abilty to define a Session on the servlet
     * Inline with the rest of the code. Using the SMTP protocol to send a message on the GreenMail
     * test server. Once the message is sent, the assert verifyies that message is sitting in the smtpServer
     *
     * This test is for FatSuiteLite
     */
    @Test
    public void testInlineMail() throws Exception {

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/TestingApp/SMTPInlineServlet");

        Log.info(c, "testInlineMail",
                 "Calling Inline Session Application with URL=" + url.toString());

        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        String line = br.readLine();

        // Assert that the message has been received by the smtpServer
        assertEquals("FAIL: SMTP was unable to send message.", "Test mail sent by GreenMail",
                     GreenMailUtil.getBody(smtpServer.getReceivedMessages()[0]));
    }

    /**
     * TestDescription:
     * This test is test the ability for the javaMail-1.6 feature to have a jakarta.mail.Session
     * object defined on the IMAPJNDIServlet. This test the abilty to define a Session on the servlet
     * using InitialContext and binding it to JNDI, another class is then called that access the servlet
     * with a JNDI lookup. Using the SMTP protocol to send a message on the GreenMail
     * test server. Once the message is sent, the assert verifyies that message is sitting in the smtpServer
     *
     * This test is for FatSuiteLite
     */
    @Test
    public void testJNDIMail() throws Exception {

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/TestingApp/SMTPJNDIServlet");
        Log.info(c, "testJNDIMail",
                 "Calling JNDI Session Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        String line = br.readLine();

        // Assert that the message has been received by the smtpServer
        assertEquals("FAIL: SMTP was unable to send message.", "Test mail sent by GreenMail",
                     GreenMailUtil.getBody(smtpServer.getReceivedMessages()[0]));
    }

    /**
     * TestDescription:
     * This test is test the ability for the javaMail-1.6 feature to have a jakarta.mail.Session
     * object defined on the SMTPMailSessionServlet. This Tests the features ability to define a mailSession
     * object in the server.xml. The mailSession object is created by the MailSessionService class
     * and injected into a Session object on the SMTPMailSessionServlet using a JNDI lookup. Using the SMTP protocol to send a message on the GreenMail
     * test server. Once the message is sent, the assert verifyies that message is sitting in the smtpServer
     *
     * This test is for FatSuiteLite
     */
    @Test
    public void testMailSessionMail() throws Exception {

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/TestingApp/SMTPMailSessionServlet");
        Log.info(c, "testMailSessionMail",
                 "Calling MailSession Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        String line = br.readLine();

        // Assert that the message has been received by the smtpServer
        assertNotNull("FAIL: IMAP folder was unable to access message.",
                      server.waitForStringInLog("Sent from Liberty JavaMail"));
    }

    @BeforeClass
    public static void startGreenMail() throws Exception {

        if (server.isStarted() != true) {
            server.startServer();
            // Pause for application to start properly and server to say it's listening on ports
            server.waitForStringInLog("port " + server.getHttpDefaultPort());
        }

        // Set the e-mail subject string
        String testSubjectString = "Sent from Liberty JavaMail";
        // Set the e-mail body string
        String testBodyString = "Test mail sent by GreenMail";
        // Create a ServerSetup with a port and host name and protocol type passed to it
        // which is then used to create the actual GreenMail server
        int smtpPort = Integer.getInteger("smtp_port"); // As per server.xml

        System.out.println("Starting SMTP server on port " + smtpPort);
        ServerSetup smtpSetup = new ServerSetup(smtpPort, "localhost", "smtp");

        smtpServer = new GreenMail(smtpSetup);
        // Start the mailTestServer
        smtpServer.start();

        // Add a user account to the mailTestServer by setting a user with email, userid, and password
        // of the user. For this test purpose email and userid are the same
        GreenMailUser setupTestUser = smtpServer.setUser("smtp@testserver.com",
                                                         "smtp@testserver.com", "smtpPa$$word4U2C");
        //Create the user on the mailTestServer
        setupTestUser.create();
        smtpServer.setUser("smtp@testserver.com", "smtpPa$$word4U2C");
    }

    @AfterClass
    public static void stopGreenMail() throws Exception {
        if (null != smtpServer) {
            smtpServer.stop();
        }

        if (server != null && server.isStarted()) {
            server.stopServer("CWWKZ0013E");
        }

    }

    /**
     * This method is used to get a connection stream from an HTTP connection. It
     * gives the output from the webpage that it gets from the connection
     *
     * @param con The connection to the HTTP address
     * @return The Output from the webpage
     */
    private BufferedReader getConnectionStream(HttpURLConnection con) throws IOException {
        InputStream is = con.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        return br;
    }

    /**
     * This method creates a connection to a webpage and then reutrns the connection
     *
     * @param url The Http Address to connect to
     * @return The connection to the http address
     */
    private HttpURLConnection getHttpConnection(URL url) throws IOException, ProtocolException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("GET");
        return con;
    }
}
