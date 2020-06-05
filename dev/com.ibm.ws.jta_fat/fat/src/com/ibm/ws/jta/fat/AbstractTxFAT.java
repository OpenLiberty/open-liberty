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
package com.ibm.ws.jta.fat;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.After;
import org.junit.Before;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

public abstract class AbstractTxFAT {
    protected static LibertyServer server;

    protected String successMessage = "COMPLETED SUCCESSFULLY";

    @Before
    public void initiateTest() throws Exception {
        beforeTest();
    }

    @After
    public void terminateTest() {
        afterTest();
    }

    protected abstract void beforeTest() throws Exception;

    protected abstract void afterTest();

    protected abstract String getTest();

    protected abstract String getContext();

    protected int getPort() {
        return server.getHttpDefaultPort();
    }

    protected String getHostname() {
        return server.getHostname();
    }

    protected StringBuilder runInServlet(String test) throws IOException {
        final URL url = new URL("http://" + getHostname() + ":" + getPort()
                                + "/" + getContext() + "/" + getTest() + "?test=" + test);

        Log.info(this.getClass(), "runInServlet", url.toString());

        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            final InputStream is = con.getInputStream();
            final InputStreamReader isr = new InputStreamReader(is);
            final BufferedReader br = new BufferedReader(isr);

            final String sep = System.getProperty("line.separator");
            final StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br
                            .readLine())
                lines.append(line).append(sep);

            if (lines.indexOf(successMessage) < 0)
                fail("Missing success message in output. " + lines);

            return lines;
        } finally {
            con.disconnect();
        }
    }
}