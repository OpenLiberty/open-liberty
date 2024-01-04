/*******************************************************************************
 * Copyright (c) 2017,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.fat;

import static io.openliberty.checkpoint.fat.FATSuite.configureEnvVariable;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class IMAPTest {

    private static final String SERVER_NAME = "imapTestServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;
    private final Class<?> c = IMAPTest.class;
    private static final String SENT = "Sent from Liberty JavaMail";
    // Creates a local GreenMail server, but set to null for now. It is full defined in the
    // BeforeClass method
    private static GreenMail imapServer = null;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action().forServers(SERVER_NAME).fullFATOnly())
                    .andWith(new JakartaEE10Action().forServers(SERVER_NAME).fullFATOnly());

    /**
     * TestDescription:
     * This test is test the ability for the javaMail-1.6 feature to have a javax.mail.Session
     * object defined on the IMAPMailSessionServlet. This Tests the features ability to define a mailSession
     * object in the server.xml. The mailSession object is created by the MailSessionService class
     * and injected into a Session object on the IMAPMailSessionServlet using a JNDI lookup. Using the
     * IMAP protocol it access the inbox folder on the GreenMail test server. If the folder is accessed
     * than it prints out a message if an exception isn't thrown, and that message is used in the
     * assert in validate that the message was printed to the log
     *
     * This test is for FatSuiteLite
     */
    @Test
    public void testMailSessionMail() throws Exception {

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/IMAPTestingApp/IMAPMailSessionServlet");
        Log.info(c, "testAutoInstall",
                 "Calling MailSession Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        String line = br.readLine();

        // Check if IMAP protocol Session was able to access Inbox and check MimeMessage
        assertNotNull("FAIL: IMAP folder was unable to access message.",
                      server.waitForStringInLog(SENT));
    }

    private static void setupApp() throws Exception {
        WebArchive testingApp = ShrinkWrap.create(WebArchive.class, "IMAPTestingApp.war")
                        .addPackages(true, "IMAPTestingApp")
                        .addAsWebInfResource(new File("test-applications/IMAPTestingApp/resources/META-INF/MANIFEST.MF"));
        ShrinkHelper.exportAppToServer(server, testingApp);

    }

    /**
     * The BeforeClass method sets up the GreenMail test server, so that the "server" is waiting
     * before even the Liberty class is set up.
     *
     * @throws UserException
     * @throws Exception
     */
    @BeforeClass
    public static void startGreenMail() throws Exception {
        setupApp();

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer();
        // Update the mailSession config with host, user and password before restore.
        Map<String, String> imapProps = new HashMap<>();
        imapProps.put("imap_host", "localhost");
        imapProps.put("imap_user", "imap@testserver.com");
        imapProps.put("imap_password", "imapPa$$word");

        configureEnvVariable(server, imapProps);

        server.checkpointRestore();
        // Pause for application to start properly and server to say it's listening on ports
        server.waitForStringInLog("port " + server.getHttpDefaultPort());

        // Create a ServerSetup with a port and host name and protocol type passed to it
        // which is then used to create the actual GreenMail server.
        int imapPort = Integer.getInteger("imap_port"); // As per server.xml
        System.out.println("Starting IMAP server on port " + imapPort);
        ServerSetup imapSetup = new ServerSetup(imapPort, "localhost", "imap");
        imapServer = new GreenMail(imapSetup);
        // Start the imapServer, the GreenMail server is now listening for connections
        imapServer.start();

        // Add a user account to the mailTestServer by setting a user with email, userid, and password
        // of the user. For this test purpose email and userid are the same
        GreenMailUser setupTestUser = imapServer.setUser("imap@testserver.com",
                                                         "imap@testserver.com", "imapPa$$word");
        //Create the user on the mailTestServer
        setupTestUser.create();
        imapServer.setUser("imaps@testserver.com", "imapsPa$$word");

        // After the server and user are setup, a MimeMessage is used to
        // to store a message under the user's inbox folder on the GreenMail server
        MimeMessage message = new MimeMessage((javax.mail.Session) null);
        message.setFrom(new InternetAddress("imaps@testserver.com"));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("imaps@testserver.com"));
        message.setSubject("Sent from Liberty JavaMail");
        message.setText("Test mail sent by GreenMail");

        // Now that the MimeMessage is created the user delivers it to imapServer
        setupTestUser.deliver(message);

    }

    /**
     * Tear down the GreenMail imapSever.
     *
     * @throws Exception
     */
    @AfterClass
    public static void stopGreenMail() throws Exception {

        if (null != imapServer) {
            imapServer.stop();
        }

        if (server != null && server.isStarted()) {
            server.stopServer();
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
