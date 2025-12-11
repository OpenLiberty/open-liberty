/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
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

package com.ibm.ws.messaging.JMS20.fat.DurableUnshared;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.messaging.JMS20security.fat.TestUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class DurableUnshared {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("TestServer");
    private static LibertyServer server1 = LibertyServerFactory.getLibertyServer("TestServer1");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    private boolean runInServlet(String test) throws IOException {
        boolean result = false;

        URL url = new URL("http://" + HOST + ":" + PORT
                          + "/DurableUnshared?test=" + test);
        System.out.println("The Servlet URL is : " + url.toString());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            con.connect();

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String sep = System.lineSeparator();
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br.readLine())
                lines.append(line).append(sep);

            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0) {
                org.junit.Assert.fail("Missing success message in output. "
                                      + lines);

                result = false;
            } else
                result = true;

            return result;
        } finally {
            con.disconnect();
        }
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {

        server1.copyFileToLibertyInstallRoot("lib/features",
                                             "features/testjmsinternals-1.0.mf");
        server1.copyFileToLibertyServerRoot("resources/security",
                                            "serverLTPAKeys/cert.der");
        server1.copyFileToLibertyServerRoot("resources/security",
                                            "serverLTPAKeys/ltpa.keys");
        server1.copyFileToLibertyServerRoot("resources/security",
                                            "serverLTPAKeys/ltpaFIPS.keys");
        server1.copyFileToLibertyServerRoot("resources/security",
                                            "serverLTPAKeys/mykey.jks");
        server.copyFileToLibertyInstallRoot("lib/features",
                                            "features/testjmsinternals-1.0.mf");
        server.copyFileToLibertyServerRoot("resources/security",
                                           "clientLTPAKeys/mykey.jks");

        TestUtils.addDropinsWebApp(server, "DurableUnshared", "web");

        startAppServers();
    }

    /**
     * Start both the JMSConsumerClient local and remote messaging engine AppServers.
     *
     * @throws Exception
     */
    private static void startAppServers() throws Exception {
        server.setServerConfigurationFile("JMSContext_ssl.xml");
        server1.setServerConfigurationFile("TestServer1_ssl.xml");
        server.startServer("DurableUnShared_Client.log");
        server1.startServer("DurableUnShared_Server.log");

        // CWWKF0011I: The TestServer1 server is ready to run a smarter planet. The TestServer1 server started in 6.435 seconds.
        // CWSID0108I: JMS server has started.
        // CWWKS4105I: LTPA configuration is ready after 4.028 seconds.
        for (String messageId : new String[] { "CWWKF0011I.*", "CWSID0108I.*", "CWWKS4105I.*" }) {
            String waitFor = server.waitForStringInLog(messageId, server.getMatchingLogFile("messages.log"));
            assertNotNull("Server message " + messageId + " not found", waitFor);
            waitFor = server1.waitForStringInLog(messageId, server1.getMatchingLogFile("messages.log"));
            assertNotNull("Server1 message " + messageId + " not found", waitFor);
        }
        
        // The following FFDC may be thrown at server startup because the channel framework does not become active until the CWWKF0011I message is seen, whereas MDB initialisation takes place beforehand.
        // FFDC1015I: An FFDC Incident has been created: "com.ibm.wsspi.channelfw.exception.InvalidChainNameException: Chain configuration not found in framework, BootstrapSecureMessaging com.ibm.ws.sib.jfapchannel.richclient.framework.impl.RichClientTransportFactory.getOutboundNetworkConnectionFactoryByName 00280001" at ffdc_21.09.27_15.21.46.0.log
        
        // Ignore failed connection attempts between the two servers.
        // CWSIV0782W: The creation of a connection for destination RedeliveryQueue1 on bus defaultBus for endpoint activation jmsapp/jmsmdb/RDC2MessageDrivenBean failed with exception javax.resource.ResourceException: 
        server.addIgnoredErrors(Arrays.asList("CWSIV0782W"));
    }
    
    private static void stopAppServers() throws Exception {
        
        if (JakartaEEAction.isEE9OrLaterActive()) {
            // Remove the Jakarta special case once fixed.
            // Also remove @AllowedFFDC( { "jakarta.resource.spi.InvalidPropertyException"} )
            // [24/03/21 16:57:09:781 GMT] 0000004b com.ibm.ws.config.xml.internal.ConfigEvaluator               W CWWKG0032W: Unexpected value specified for property [destinationType], value = [javax.jms.Topic]. Expected value(s) are: [jakarta.jms.Queue][jakarta.jms.Topic]. Default value in use: [jakarta.jms.Queue].
            // [24/03/21 16:57:09:781 GMT] 0000004b com.ibm.ws.config.xml.internal.ConfigEvaluator               W CWWKG0032W: Unexpected value specified for property [destinationType], value = [javax.jms.Topic]. Expected value(s) are: [jakarta.jms.Queue][jakarta.jms.Topic]. Default value in use: [jakarta.jms.Queue].
            // [24/03/21 16:57:16:336 GMT] 0000004b com.ibm.ws.jca.service.EndpointActivationService             E J2CA8802E: The message endpoint activation failed for resource adapter wasJms due to exception: jakarta.resource.spi.InvalidPropertyException: CWSJR1181E: The JMS activation specification has invalid values - the reason(s) for failing to validate the JMS       
            server.addIgnoredErrors(Arrays.asList("CWWKG0032W","J2CA8802E"));
        }
        server.stopServer();
        server1.stopServer();  
    }

    @AllowedFFDC( { "jakarta.resource.spi.InvalidPropertyException"} )
    @AllowedFFDC( { "com.ibm.websphere.sib.exception.SIResourceException"} )
    @Test
    public void testCreateUnSharedDurable_B_SecOn() throws Exception {

        boolean val = runInServlet("testCreateUnSharedDurableConsumer_create");
        assertTrue("testCreateUnSharedDurable_B_SecOn_create failed", val);

        stopAppServers();
        startAppServers();

        val = runInServlet("testCreateUnSharedDurableConsumer_consume");
        assertTrue("testCreateUnSharedDurable_B_SecOn_consume failed", val);
    }

    @AllowedFFDC( { "jakarta.resource.spi.InvalidPropertyException"} )
    @AllowedFFDC( { "com.ibm.websphere.sib.exception.SIResourceException"} )
    @Test
    public void testCreateUnSharedDurable_TCP_SecOn() throws Exception {

        boolean val = runInServlet("testCreateUnSharedDurableConsumer_create_TCP");
        assertTrue("testCreateUnSharedDurable_TCP_SecOn_create failed", val);

        stopAppServers();
        startAppServers();

        val = runInServlet("testCreateUnSharedDurableConsumer_consume_TCP");
        assertTrue("testCreateUnSharedDurable_TCP_SecOn_consume failed", val);
    }

    @org.junit.AfterClass
    public static void tearDown() {
        try {
            stopAppServers();
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new RuntimeException("tearDown exception", exception);
        }
        
        ShrinkHelper.cleanAllExportedArchives();
    }

}
