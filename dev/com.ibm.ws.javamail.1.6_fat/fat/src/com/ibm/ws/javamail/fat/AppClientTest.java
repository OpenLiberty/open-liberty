package com.ibm.ws.javamail.fat;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Map;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

import componenttest.annotation.SkipForRepeat;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;
import junit.framework.Assert;

@SkipForRepeat("javaMail-1.6")
public class AppClientTest {
    public static int smtpPort;
    public static int imapPort;
    public static final String mailUserAddress = "user@testserver.com";
    public static final String mailUserPass = "userPass";

    protected static LibertyClient client;

    private static final Class<?> c = AppClientTest.class;
    private static String testClientName = "javamail.client_fat";
    private static String jarName = "JavaMailClientApp";
    private static String earName = "JavaMailClientEAR";

    private static GreenMail greenMailTestServer;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    // The client test application will be launched and will complete by the time we exit this method (or will
    // fail to launch in which case the entire test fails).
    // Other tests in this class simply examine the output logs for various strings that the client will emit when
    // some piece of functionality is tested and passes in the client.
    public static void setupTestAndLaunch() throws Exception {

        String thisMethod = "before";
        Log.info(c, thisMethod,
                 "Performing the server setup for all test classes");

        // Ports are set as system properties for BVT from
        // testports.properties
        smtpPort = Integer.parseInt(System.getProperty("smtp_port"));
        imapPort = Integer.parseInt(System.getProperty("imap_port"));

        Log.info(c, thisMethod, "Server setup is complete");

        configureAndLaunchGreenMailService();
        runClient(testClientName, "javaMailClient");
        client.waitForStringInCopiedLog("CWWKE1103I:.*");

    }

    @AfterClass
    public static void shutDownGreenmail() {
        greenMailTestServer.stop();
    }

    /**
     *
     * Check for basic send(SMTP)/receive(IMAP) of email using a session object created
     * programatically.
     *
     * @throws Exception
     */
    @Test
    public void testClientJavaMailSendReceive() throws Exception {
        // check for messages from the 3 main test scenarious that are
        //Check message.log for mail sent and received messages
        int matches = client.findStringsInCopiedLogs(".*testBasicSendReceive.*Application client received email with expected 'From' and 'Subject' headers.*").size();
        Assert.assertEquals("Did not find expected number of messages in message.log", 1, matches);
    }

    @Test
    public void testClientJavaMailSendReceiveUsingAnnotationDefinition() throws Exception {
        int matches = client.findStringsInCopiedLogs(".*testInjectionFromAnnotationDefinition.*Application client received email with expected 'From' and 'Subject' headers.*")
                        .size();
        Assert.assertEquals("Did not find expected number of messages in message.log", 1, matches);
    }

    @Test
    public void testClientJavaMailSendReceiveUsingAnnotationDescriptor() throws Exception {
        int matches = client.findStringsInCopiedLogs(".*testInjectionFromDescriptorDefinition.*Application client received email with expected 'From' and 'Subject' headers.*")
                        .size();
        Assert.assertEquals("Did not find expected number of messages in message.log", 1, matches);
    }

    // Run the client and check for expected messages that the client started. By the time this
    //  method returns the client will have completed all tests and exited.
    private static void runClient(String testClientName, String appName) throws Exception {
        final String methodName = "startClient";
        Log.entering(c, methodName, "Starting clients & checking console messages");
        Log.info(c, methodName, "Name of client:" + testClientName);

        client = LibertyClientFactory.getLibertyClient(testClientName);

        JavaArchive jar = ShrinkHelper.buildJavaArchive(jarName + ".jar", "com.ibm.ws.javamail.client");

        String clientManifest = "test-applications/JavaMailClientApp/resources/META-INF/MANIFEST.MF";

        ShrinkHelper.addDirectory(jar, "test-applications/" + jarName + "/resources/");
        jar.setManifest(new File(clientManifest));

        jar.addAsManifestResource(new File("test-applications/JavaMailClientApp/resources/META-INF/application-client.xml"));

        ShrinkHelper.addDirectory(jar, "test-applications/" + jarName + "/resources/");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, earName + ".ear").addAsModule(jar);
        ShrinkHelper.addDirectory(ear, "test-applications/" + earName + "/resources/");

        ear.setManifest(new File("test-applications/JavaMailClientEAR/resources/META-INF/MANIFEST.MF"));
        String appXML = "test-applications/" + earName + "/resources/META-INF/application.xml";
        ear.addAsManifestResource(new File(appXML));

        File permissionsFile = new File("test-applications/" + earName + "/resources/META-INF/permissions.xml");
        if (permissionsFile.exists()) {
            Log.info(c, methodName, "Add EAR permissions: " + earName);
            ear.addAsManifestResource(permissionsFile);
        }

        ShrinkHelper.exportArtifact(ear, "tmp/");

        ShrinkHelper.exportAppToClient(client, ear);

        Map<String, String> jvmOptions = client.getJvmOptionsAsMap();
        jvmOptions.put("-Dcom.ibm.ws.javaMailClient.fat.smtp.port", Integer.toString(smtpPort));
        jvmOptions.put("-Dcom.ibm.ws.javaMailClient.fat.imap.port", Integer.toString(imapPort));
        client.setJvmOptions(jvmOptions);
        Log.info(c, methodName, "Starting Client");
        client.startClient();

        // Verify that test app started
        assertNotNull("FAIL: Did not receive application started message:CWWKZ0001I",
                      client.waitForStringInCopiedLog("CWWKZ0001I:.*" + appName));
        Log.exiting(c, methodName);
    }

    private static void configureAndLaunchGreenMailService() throws Exception {
        //define smtp and imap services and start GreenMail
        ServerSetup imapSetup = new ServerSetup(imapPort, "localhost", "imap");
        ServerSetup smtpSetup = new ServerSetup(smtpPort, "localhost", "smtp");
        greenMailTestServer = new GreenMail(new ServerSetup[] { imapSetup, smtpSetup });
        greenMailTestServer.start();
        //define greenMail user.
        greenMailTestServer.setUser(mailUserAddress, mailUserPass);
    }
}
