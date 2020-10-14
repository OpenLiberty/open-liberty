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
package com.ibm.ws.diagnostics.zos.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.ibm.wsspi.logging.IntrospectableService;

public class NetstatInformation implements IntrospectableService {

    /**
     * Declarative services activation callback.
     */
    protected void activate() {
    }

    /**
     * Declarative services deactivation callback.
     */
    protected void deactivate() {
    }

    /**
     * Get string that will serve as the basis of the file name.
     */
    @Override
    public String getName() {
        return "NetstatInformation";
    }

    /**
     * Get a simple description for this diagnostic module.
     */
    @Override
    public String getDescription() {
        return "Network connection status information from /bin/onetstat";
    }

    /**
     * Collect diagnostic information about established connections.
     *
     * @param out the output stream to write diagnostics to
     */
    @Override
    public void introspect(OutputStream out) throws IOException {
        PrintWriter writer = new PrintWriter(out, true);

        // Put out a header before the information
        writer.println("Network Connection Information");
        writer.println("------------------------------");
        getNetstatInfo(writer);

        writer.flush();
    }

    /**
     * Invoke <code>/bin/onetstat -b IDLETIME</code> to get information about
     * connections and listeners. In addition to showing the IP addresses of
     * both sides of TCP connections, it will show the idle time on a connection.
     * This may be useful information in the case of a "hung" server thot's
     * waiting for data from a black box.
     *
     * @param writer the writer we use to capture the information
     */
    private void getNetstatInfo(PrintWriter writer) throws IOException {
        List<String> args = new ArrayList<String>();
        args.add("/bin/onetstat");
        args.add("-b"); // Bytes exchanged
        args.add("IDLETIME"); // Include socket idle time

        // Create the process we'll use to invoke 'netstat'
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        Process netstatProcess = processBuilder.redirectErrorStream(true).start();

        // Wrap the input stream with a reader
        InputStream netstatInputStream = netstatProcess.getInputStream();
        InputStreamReader netstatInputStreamReader = new InputStreamReader(netstatInputStream);
        BufferedReader netstatReader = new BufferedReader(netstatInputStreamReader);

        String line;
        while ((line = netstatReader.readLine()) != null) {
            writer.println(line);
        }
        netstatReader.close();

        int returnCode = -1;
        try {
            returnCode = netstatProcess.waitFor();
        } catch (InterruptedException ie) {
            throw new IOException(ie);
        } finally {
            netstatProcess.getOutputStream().close();
            netstatProcess.getErrorStream().close();
        }

        if (returnCode != 0) {
            throw new IOException("Unable to get netstat info");
        }
    }
}
