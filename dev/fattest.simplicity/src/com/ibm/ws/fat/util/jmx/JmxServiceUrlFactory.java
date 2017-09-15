/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.util.jmx;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.remote.JMXServiceURL;

import componenttest.topology.impl.LibertyServer;

/**
 * Caches the location of the local connector's JMX service address
 * 
 * @author Tim Burns
 */
public class JmxServiceUrlFactory {

    private static final Logger LOG = Logger.getLogger(JmxConnection.class.getName());

    protected static JmxServiceUrlFactory INSTANCE;

    /**
     * Retrieve a global instance of this class; useful if the JMX local address files
     * for your liberty servers do not change during the lifetime of your JVM.
     * 
     * @return a global instance of this class
     */
    public static JmxServiceUrlFactory getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JmxServiceUrlFactory();
        }
        return INSTANCE;
    }

    /**
     * Convenience method that derives the base directory from a {@link LibertyServer}.
     * 
     * @see #getUrl(String)
     * @param server a liberty server with the localConnector-1.0 feature enabled
     * @return a {@link JMXServiceURL} that allows you to invoke MBeans on the server
     * @throws JmxException
     *             if the server can't be found,
     *             the localConnector-1.0 feature is not enabled,
     *             or the address file is not valid
     */
    public JMXServiceURL getUrl(LibertyServer server) throws JmxException {
        return this.getUrl(server.getServerRoot());
    }

    /**
     * <p>
     * Detects a {@link JMXServiceURL} given a liberty server's base directory, and caches that URL for future reference.
     * The localConnector-1.0 feature <b>must</b> be enabled on the server in order for this method to work.
     * </p>
     * <p>
     * Caching the URL is tricky since a new URL will be generated every time the server restarts.
     * </p>
     * 
     * @param serverRoot the base directory of a liberty server with the localConnector-1.0 feature enabled
     * @return a {@link JMXServiceURL} that allows you to invoke MBeans on the server
     * @throws JmxException
     *             if the server can't be found,
     *             the localConnector-1.0 feature is not enabled,
     *             or the address file is not valid
     */
    public JMXServiceURL getUrl(String serverRoot) throws JmxException {
        JMXServiceURL url = null;
        String connectorFile = serverRoot + File.separator + "workarea" + File.separator + "com.ibm.ws.jmx.local.address";
        LOG.info("Reading JMXServiceURL from local address file: " + connectorFile);
        try {
            String connectorAddr = null;
            File file = new File(connectorFile);
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
                connectorAddr = br.readLine();
            } finally {
                try {
                    if (br != null) {
                        br.close();
                    }
                } catch (IOException e) {
                    LOG.log(Level.INFO, "Failed to close reader for " + file, e);
                }
            }
            url = new JMXServiceURL(connectorAddr);
            LOG.info("JMXServiceURL: " + connectorAddr);
        } catch (Exception e) {
            StringBuilder msg = new StringBuilder();
            msg.append("Failed to read a JMXServiceURL from a server's local address file; is the localConnector-1.0 feature enabled? Address File: ");
            msg.append(connectorFile);
            throw new JmxException(msg.toString(), e);
        }
        return url;
    }

}
