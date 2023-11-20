/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

import static io.openliberty.checkpoint.fat.FATSuite.getTestMethodNameOnly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Key;
import java.security.KeyStore;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.jwt.JWTTokenBuilder;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import mpjwt.JWTApplication;
import mpjwt.JWTBean;

@RunWith(FATRunner.class)
@CheckpointTest
public class MPJWTTest extends FATServletClient {

    @Server("checkpointMPJWT")
    public static LibertyServer server;

    private static final String APP_NAME = "mpjwt";

    private static String authHeaderAdmin;
    private static String authHeaderUser;
    private static String urlUsername;
    private static String urlRoles;

    private static WebArchive mpjwtApp;

    @ClassRule
    public static RepeatTests repeatTest = FATSuite.defaultMPRepeat("checkpointMPJWT");

    @BeforeClass
    public static void createAppAndExportToServer() throws Exception {
        Package pkg = JWTApplication.class.getPackage();
        mpjwtApp = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addClass(JWTApplication.class)
                        .addClass(JWTBean.class)
                        .addPackages(true, pkg);
        ShrinkHelper.exportAppToServer(server, mpjwtApp, DeployOptions.OVERWRITE);

        String urlBase = createURL(server, "/app/properties").toExternalForm();
        urlUsername = urlBase + "/username";
        urlRoles = urlBase + "/jwtroles";

        authHeaderAdmin = "Bearer " + createAdminJwt("testAdmin");
        authHeaderUser = "Bearer " + createUserJwt("testUser");
    }

    @Before
    public void setUp() throws Exception {
        server.saveServerConfiguration();
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false,
                             server -> {
                                 assertNotNull("'SRVE0169I: Loading Web Module' message not found in log before rerstore",
                                               server.waitForStringInLogUsingMark("SRVE0169I: Loading Web Module: " + APP_NAME, 0));
                                 assertNotNull("'CWWKZ0001I: Application mpjwt started' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I: Application " + APP_NAME + " started", 0));
                             });
        server.startServer(getTestMethodNameOnly(testName) + ".log");
    }

    public static URL createURL(LibertyServer server, String path) throws MalformedURLException {
        if (!path.startsWith("/"))
            path = "/" + path;
        return new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + path);
    }

    @Test
    public void testRolesEndpoint() throws Exception {
        try {
            server.checkpointRestore();

            // wait on LTPA to be available to avoid error "CWWKS4000E: ... The requested TokenService instance of type Ltpa2 could not be found."
            assertNotNull("'CWWKS4105I: LTPA configuration is ready' message not found.", server.waitForStringInLog("CWWKS4105I.*"));

            Response response = makeRequest(urlRoles, authHeaderAdmin);
            assertEquals("Incorrect response code from " + urlRoles, 200, response.getStatus());
            assertEquals("Incorrect groups claim in token " + urlRoles, "[\"admin\",\"user\"]", response.readEntity(String.class));

            response = makeRequest(urlRoles, authHeaderUser);
            assertEquals("Incorrect response code from " + urlRoles, 200, response.getStatus());
            assertEquals("Incorrect groups claim in token " + urlRoles, "[\"user\"]", response.readEntity(String.class));

            response.close();
        } finally {
            server.stopServer(false, "");
        }
    }

    @Test
    public void testUpdatedClockSkewOnRestore() throws Exception {
        try {
            server.checkpointRestore();

            // wait on LTPA to be available to avoid error "CWWKS4000E: ... The requested TokenService instance of type Ltpa2 could not be found."
            assertNotNull("'CWWKS4105I: LTPA configuration is ready' message not found.", server.waitForStringInLog("CWWKS4105I.*"));

            Response response = makeRequest(urlUsername, authHeaderUser);
            assertEquals("Incorrect response code from " + urlUsername, 200, response.getStatus());
            server.stopServer(false, "");

            // By default clockSkew is set to 5 mins so total expiry time of JWT token = exp + clockSkew = 5min 5 sec.
            Thread.sleep(5000);
            server.checkpointRestore();
            response = makeRequest(urlUsername, authHeaderUser);
            assertEquals("Incorrect response code from " + urlUsername, 200, response.getStatus());
            server.stopServer(false, "");

            ServerConfiguration config = server.getServerConfiguration();
            config.getVariables().getById("clockSkew").setValue("0");
            server.updateServerConfiguration(config);
            server.checkpointRestore();

            // After updating the clockSkew to 0, the expiry time of JWT token = 5sec. The request should be unauthorized after the token is expired.
            response = makeRequest(urlUsername, authHeaderUser);
            assertEquals("Incorrect response code from " + urlUsername, 401, response.getStatus());
            response.close();
        } finally {
            //[ERROR   ] CWWKS5523E: The MicroProfile JWT feature cannot authenticate the request because the token that is included in the request cannot be validated.
            // CWWKS5524E: The MicroProfile JWT feature encountered an error while creating a JWT by using the [myMpJwt] configuration and the token included in the request.
            // CWWKS6031E: The JSON Web Token (JWT) consumer [myMpJwt] cannot process the token string. JWT_TOKEN_AGE_AFTER_CURRENT_TIME
            server.stopServer(false, "CWWKS5523E", "CWWKS5524E", "CWWKS6031E");
        }

    }

    @Test
    public void testUpdatedAudienceOnRestore() throws Exception {
        try {
            server.checkpointRestore();

            // wait on LTPA to be available to avoid error "CWWKS4000E: ... The requested TokenService instance of type Ltpa2 could not be found."
            assertNotNull("'CWWKS4105I: LTPA configuration is ready' message not found.", server.waitForStringInLog("CWWKS4105I.*"));

            Response response = makeRequest(urlUsername, authHeaderUser);
            assertEquals("Incorrect response code from " + urlUsername, 200, response.getStatus());
            server.stopServer(false, "");

            ServerConfiguration config = server.getServerConfiguration();
            config.getVariables().getById("audience").setValue("audience2");
            server.updateServerConfiguration(config);
            server.checkpointRestore();

            response = makeRequest(urlUsername, authHeaderUser);
            assertEquals("Incorrect response code from " + urlUsername, 401, response.getStatus());
            response.close();
        } finally {
            //[ERROR   ] CWWKS5524E: The MicroProfile JWT feature encountered an error while creating a JWT by using the [myMpJwt] configuration and the token included in the request.
            // CWWKS6031E: The JSON Web Token (JWT) consumer [myMpJwt] cannot process the token string.
            // CWWKS6023E: The audience [[audience1]] of the provided JSON web token (JWT) is not listed as a trusted audience in the [myMpJwt] JWT configuration. The trusted audiences are [[audience2]].
            server.stopServer(false, "CWWKS5523E", "CWWKS5524E", "CWWKS6031E");
        }

    }

    private Response makeRequest(String url, String authHeader) {
        Client client = ClientBuilder.newClient();
        Builder builder = client.target(url).request();
        builder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        if (authHeader != null) {
            Log.info(getClass(), "makeRequest", authHeader);
            builder.header(HttpHeaders.AUTHORIZATION, authHeader);
        }
        Response response = builder.get();
        return response;
    }

    public static String createUserJwt(String username) throws Exception {
        Set<String> groups = new HashSet<String>();
        groups.add("user");
        return createJwt(username, groups);
    }

    public static String createAdminJwt(String username) throws Exception {
        Set<String> groups = new HashSet<String>();
        groups.add("admin");
        groups.add("user");
        return createJwt(username, groups);
    }

    @SuppressWarnings("restriction")
    private static String createJwt(String username, Set<String> groups) throws Exception {
        JWTTokenBuilder builder = new JWTTokenBuilder();
        builder.setIssuer("client1");
        builder.setClaim("upn", username);
        builder.setSubject(username);
        builder.setAlorithmHeaderValue("RS256");
        builder.setRSAKey(getPrivateKey());
        builder.setAudience("audience1");
        builder.setIssuedAtToNow();
        builder.setExpirationTimeSecondsFromNow(5);
        builder.setClaim("groups", groups);
        String jwtToken = builder.buildAsIs();
        return jwtToken;
    }

    private static String getPrivateKey() throws Exception {
        String keystorePath = new RemoteFile(server.getMachine(), server.getServerRoot() + "/resources/security/key.p12").getAbsolutePath();
        String rsPrivateKeyPath = new RemoteFile(server.getMachine(), server.getServerRoot() + "/resources/security/RS256private-key.pem").getAbsolutePath();
        try {
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            char[] password = new String("secret").toCharArray();
            keystore.load(new FileInputStream(new RemoteFile(server.getMachine(), keystorePath).getAbsolutePath()), password);
            Key key = keystore.getKey("default", password);
            String output = "-----BEGIN PRIVATE KEY-----\n"
                            + Base64.getEncoder().encodeToString(key.getEncoded()) + "\n"
                            + "-----END PRIVATE KEY-----";

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(rsPrivateKeyPath))) {
                writer.write(output);
                writer.flush();
            }
            return rsPrivateKeyPath;
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't write the encoded private key to the file " + rsPrivateKeyPath, e);
        }
    }

    @After
    public void tearDown() throws Exception {
        server.postStopServerArchive();
        server.restoreServerConfiguration();
    }
}
