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


import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import declared.roles.DeclaredRolesApplication;
import declared.roles.NoRoleResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.assertEquals;

@RunWith(FATRunner.class)
@Mode(Mode.TestMode.LITE)
public class UnauthenticatedDeclaredRolesTests extends FATServletClient {

    private static final Class<?> c = UnauthenticatedDeclaredRolesTests.class;

    public static final String SERVER_NAME = "basicServer";
    public static final String APP_NAME = "DeclaredRoles";
    private static final String CONTEXT_ROOT = "/" + APP_NAME;
    private static final String RESOURCE_PATH = "/noroles";

    private static String url = null;

    @Server(SERVER_NAME)
    //@TestServlet(servlet = UnauthenticatedServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        Log.info(c, "setUp", "Starting server setup...");
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME+".war").addClass(DeclaredRolesApplication.class).addClass(NoRoleResource.class);
        ShrinkHelper.exportDropinAppToServer(server, app, ShrinkHelper.DeployOptions.SERVER_ONLY);
        server.startServer();

        server.waitForStringInLog("CWWKS4105I");
        Log.info(c, "setUp", "Server started successfully");
    }


      @Test
      public void unauthenticatedTest() throws Exception {
        String url = "http://localhost:" + server.getHttpDefaultPort() + CONTEXT_ROOT + RESOURCE_PATH;
        URL urlObj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();

          conn.setRequestMethod("GET");
          conn.setDoInput(true);

          int responseCode = conn.getResponseCode();
          assertEquals("Expected status code " + 200 + " but got " + responseCode,
                  200, responseCode);

          // Read response
          StringBuilder response = new StringBuilder();
          if (responseCode == 200) {
              BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
              String inputLine;
              while ((inputLine = in.readLine()) != null) {
                  response.append(inputLine);
              }
              in.close();
          }

          conn.disconnect();

          // Unauthenticated users should have 0 roles.
          assertEquals("[]", response.toString());
      }

      @After
      public void teardown() throws Exception {
        server.stopServer();
      }
}
