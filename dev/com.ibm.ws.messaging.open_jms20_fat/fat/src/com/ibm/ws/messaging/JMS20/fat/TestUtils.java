/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20.fat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import componenttest.topology.impl.LibertyServer;
import com.ibm.websphere.simplicity.ShrinkHelper;

public class TestUtils {

    public static WebArchive addWebApp(
        LibertyServer targetServer,
        String appName,
        String... packageNames) throws Exception {

        return addWebApp(targetServer, !IS_DROPIN, appName, packageNames);
    }

    public static WebArchive addDropinsWebApp(
        LibertyServer targetServer,
        String appName,
        String... packageNames) throws Exception {

        return addWebApp(targetServer, IS_DROPIN, appName, packageNames);
    }

    public static final boolean IS_DROPIN = true;

    public static WebArchive addWebApp(
        LibertyServer targetServer,
        boolean isDropin,
        String appName,
        String... packageNames) throws Exception {

        WebArchive webApp = ShrinkWrap.create(WebArchive.class, appName + ".war");
        webApp.addPackages(true, packageNames);

        File webInf = new File("test-applications/" + appName + "/resources/WEB-INF");
        if ( webInf.exists() ) {
            for ( File webInfElement : webInf.listFiles() ) {
                webApp.addAsWebInfResource(webInfElement);
            }
        }

        String appFolder = ( isDropin ? "dropins" : "apps" );
        ShrinkHelper.exportToServer(targetServer, appFolder, webApp);

        return webApp;
    }

    public static boolean runInServlet(
        String host, int port,
        String contextRoot, String test) throws IOException {

        URL servletUrl = new URL("http://" + host + ":" + port + "/" + contextRoot + "?test=" + test);
        System.out.println("Test URL [ " + servletUrl + " ]");

        HttpURLConnection con = (HttpURLConnection) servletUrl.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("GET");

        try {
            con.connect();

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String sep = System.lineSeparator();

            StringBuilder lines = new StringBuilder();
            String line;
            while ( (line = br.readLine()) != null ) {
                lines.append(line).append(sep);
            }

            String successMessage = "COMPLETED SUCCESSFULLY";
            boolean result;
            if ( lines.indexOf(successMessage) < 0 ) {
                org.junit.Assert.fail("Missing success message [ " + successMessage + " ] in output [ " + lines + " ]");
                result = false;
            } else {
                result = true;
            }
            return result;

        } finally {
            con.disconnect();
        }
    }
}
