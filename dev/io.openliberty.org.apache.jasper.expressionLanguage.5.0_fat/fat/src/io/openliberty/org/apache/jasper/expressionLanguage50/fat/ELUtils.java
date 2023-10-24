/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.jasper.expressionLanguage50.fat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Properties;

import componenttest.topology.impl.LibertyServer;

// Borrowed from JSPUtils.java
public class ELUtils {
    protected static final Class<?> c = ELUtils.class;

    /**
     * Construct a URL for a test case so a request can be made.
     *
     * @param server - The server that is under test, this is used to get the port and host name.
     * @param contextRoot - The context root of the application
     * @param path - Additional path information for the request.
     * @return - A fully formed URL.
     * @throws Exception
     */
    public static URL createHttpUrl(LibertyServer server, String contextRoot, String path) throws Exception {
        return new URL(createHttpUrlString(server, contextRoot, path));
    }

    /**
     * Construct a URL for a test case so a request can be made.
     *
     * @param server - The server that is under test, this is used to get the port and host name.
     * @param contextRoot - The context root of the application
     * @param path - Additional path information for the request.
     * @return - A fully formed URL string.
     * @throws Exception
     */
    public static String createHttpUrlString(LibertyServer server, String contextRoot, String path) {

        StringBuilder sb = new StringBuilder();
        sb.append("http://")
          .append(server.getHostname())
          .append(":")
          .append(server.getHttpDefaultPort())
          .append("/")
          .append(contextRoot)
          .append("/")
          .append(path);

        return sb.toString();
    }

    /**
     * Set websphere.java.security.exempt to true in the provided server's bootstrap.properties file.
     *
     * @param server
     * @throws Exception
     */
    public static void setServerJavaSecurityExempt(LibertyServer server) throws Exception {
        File bootstrapPropertiesFile = new File(server.getFileFromLibertyServerRoot("bootstrap.properties").getAbsolutePath());
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(bootstrapPropertiesFile)) {
            props.load(in);
        }
        props.put("websphere.java.security.exempt", "true");
        try (OutputStream out = new FileOutputStream(bootstrapPropertiesFile)) {
            props.store(out, "Updated to include \"websphere.java.security.exempt=true\"");
        }
    }
}
