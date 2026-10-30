/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.security.jakartasec.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.utils.Constants;

/**
 * Regression test for issue 35585.
 *
 * <p>
 * An application that registers an {@code OpenIdAuthenticationMechanismDefinition} HAM and exposes an unprotected Jakarta
 * REST route that consumes a multipart body as {@code List&lt;EntityPart&gt;} must receive the uploaded parts. Previously the
 * mechanism unconditionally called {@code HttpServletRequest.getParameter("state")} to detect an OIDC callback, which
 * caused the web container to parse and consume the multipart body before Jakarta REST could read it, leaving the resource
 * with an empty list.
 *
 * <p>
 * This test only starts the RP server: because the route is unprotected, the HAM is still invoked for the request but no
 * login flow (and therefore no OpenID Provider) is required.
 */
@SuppressWarnings("restriction")
@Mode(TestMode.LITE)
@RunWith(FATRunner.class)
public class MultipartUploadTests extends CommonSecurityFat {

    protected static Class<?> thisClass = MultipartUploadTests.class;

    private static final String APP_NAME = "OidcMultipartUpload";
    private static final String UPLOAD_PATH = "/" + APP_NAME + "/api/upload";

    @Server("jakartasec-3.0_fat.multipart.rp")
    public static LibertyServer rpServer;

    private static String rpHttpBase;

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(thisClass, "setUp", "starting setup");

        // Track the server so it is cleaned up at the end of this class and not restored between tests.
        serverTracker.addServer(rpServer);
        skipRestoreServerTracker.addServer(rpServer);

        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME + ".war", "io.openliberty.security.jakartasec.fat.multipart");
        ShrinkHelper.exportDropinAppToServer(rpServer, app);
        rpServer.addInstalledAppForValidation(APP_NAME);

        String configFile = RepeatTestFilter.getRepeatActionsAsString().contains("EE11_FEATURES") ? "server_multipart_ee11.xml" : "server_multipart.xml";
        rpServer.startServerUsingExpandedConfiguration(configFile);

        SecurityFatHttpUtils.saveServerPorts(rpServer, Constants.BVT_SERVER_2_PORT_NAME_ROOT);
        rpHttpBase = "http://localhost:" + rpServer.getBvtPort();

        rpServer.waitForLTPAConfigReady();
    }

    /**
     * Upload two files to the unprotected multipart route and confirm that both reach the Jakarta REST resource.
     */
    @Test
    public void multipartUploadToUnauthenticatedRouteKeepsFiles() throws Exception {
        String endpoint = rpHttpBase + UPLOAD_PATH;
        String boundary = "----OpenLibertyMultipartBoundary" + System.nanoTime();

        StringBuilder body = new StringBuilder();
        addFilePart(body, boundary, "one.txt", "hello from file one");
        addFilePart(body, boundary, "two.txt", "hello from file two");
        body.append("--").append(boundary).append("--\r\n");

        HttpURLConnection connection = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setDoOutput(true);
        try (OutputStream out = connection.getOutputStream()) {
            out.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int status = connection.getResponseCode();
        String response = readResponse(connection);
        Log.info(thisClass, _testName, "Upload response (HTTP " + status + "): " + response);

        assertEquals("The unprotected multipart route should be reachable. Response: " + response, 200, status);
        assertTrue("The OpenID Connect HAM dropped the multipart parts (issue 35585). Response: " + response,
                   response.contains("parts=2"));
    }

    private static void addFilePart(StringBuilder body, String boundary, String fileName, String content) {
        body.append("--").append(boundary).append("\r\n");
        body.append("Content-Disposition: form-data; name=\"").append(fileName)
            .append("\"; filename=\"").append(fileName).append("\"\r\n");
        body.append("Content-Type: text/plain\r\n\r\n");
        body.append(content).append("\r\n");
    }

    private static String readResponse(HttpURLConnection connection) throws Exception {
        InputStream in = connection.getResponseCode() < 400 ? connection.getInputStream() : connection.getErrorStream();
        try (InputStream is = in) {
            if (is == null) {
                return "";
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
