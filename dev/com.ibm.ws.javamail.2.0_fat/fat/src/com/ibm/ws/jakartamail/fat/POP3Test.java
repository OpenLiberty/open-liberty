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
package com.ibm.ws.jakartamail.fat;

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

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class POP3Test {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("mailSessionTestServer");
    private final Class<?> c = POP3Test.class;
    private static final String MULTIPART = "Multipart Example";
    private static POP3Server pop3Server = null;
    private static final POP3Handler handler = new POP3Handler();

    /**
     * TestDescription:
     * This test is test the ability for the javaMail-1.5 feature to have a jakarta.mail.Session
     * object defined on the POP3InlineServlet. This tests the abilty to define a Session on the servlet
     * Inline with the rest of the code. Using the POP3 protocol it access the inbox folder on the POP3
     * test server. If the folder is accessed than it prints out a message if an exception isn't thrown, and that
     * message is used in the assert in validate that the message was printed to the log
     *
     * This test is for FatSuiteLite
     */
    @Test
    public void testInlineMail() throws Exception {

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/TestingApp/POP3InlineServlet");
        Log.info(c, "testInlineMail",
                 "Calling Inline Session Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        String line = br.readLine();
        //If there is output then the Application automatically installed correctly
        assertNotNull("FAIL: POP3 folder was unable to access message.",
                      server.waitForStringInLog(MULTIPART));
    }

    /**
     * TestDescription:
     * This test is test the ability for the javaMail-1.5 feature to have a jakarta.mail.Session
     * object defined on the POP3JNDIServlet. This test the abilty to define a Session on the servlet
     * using InitialContext and binding it to JNDI, another class is then called that access the servlet
     * with a JNDI lookup. Using the POP3 protocol it access the inbox folder on the POP3
     * test server. If the folder is accessed than it prints out a message if an exception isn't thrown, and that
     * message is used in the assert in validate that the message was printed to the log
     *
     * This test is for FatSuiteLite
     */
    @Test
    public void testJNDIMail() throws Exception {

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/TestingApp/POP3JNDIServlet");
        Log.info(c, "testJNDIMail",
                 "Calling MailSession Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        String line = br.readLine();

        // Check if IMAP protocol Session was able to access Inbox and check MimeMessage
        assertNotNull("FAIL: POP3 folder was unable to access message.",
                      server.waitForStringInLog(MULTIPART));
    }

    /**
     * TestDescription:
     * This test is test the ability for the javaMail-1.5 feature to have a jakarta.mail.Session
     * object defined on the POP3MailSessionServlet. This Tests the features ability to define a mailSession
     * object in the server.xml. The mailSession object is created by the MailSessionService class
     * and injected into a Session object on the POP3MailSessionServlet using a JNDI lookup. Using the
     * IMAP protocol it access the inbox folder on the POP3 test server. If the folder is accessed
     * than it prints out a message if an exception isn't thrown, and that message is used in the
     * assert in validate that the message was printed to the log
     *
     * This test is for FatSuiteLite
     */
    @Test
    public void testMailSessionMail() throws Exception {

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/TestingApp/POP3MailSessionServlet");
        Log.info(c, "testAutoInstall",
                 "Calling MailSession Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        String line = br.readLine();

        // Check if IMAP protocol Session was able to access Inbox and check MimeMessage
        assertNotNull("FAIL: POP3 folder was unable to access message.",
                      server.waitForStringInLog(MULTIPART));
    }

    /**
     * Unlike the IMAP or SMTP tests the POP3 tests use the POP3Server and POP3Handler
     * that are two classes taken from the JavaMail-1.5 source. This class is simple enough
     * and all that needs to be set is passed a handler and a port number which for the pop3
     * server has been set to 3110
     *
     * @throws UserException
     * @throws Exception
     */
    @BeforeClass
    public static void startPOP3Server() throws Exception {

        int pop3Port = Integer.getInteger("pop3_port"); // As per server.xm
        pop3Server = new POP3Server(handler, pop3Port);
        pop3Server.start();

        if (server.isStarted() != true) {
            server.startServer();
            // Pause for application to start properly and server to say it's listening on ports
            server.waitForStringInLog("port " + server.getHttpDefaultPort());
        }

    }

    @AfterClass
    public static void stopGreenMail() throws Exception {
        pop3Server.quit();
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
