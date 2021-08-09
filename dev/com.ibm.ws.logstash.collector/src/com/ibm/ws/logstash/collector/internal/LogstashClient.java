/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logstash.collector.internal;

import java.io.IOException;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.collector.Client;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.lumberjack.LumberjackClient;
import com.ibm.wsspi.ssl.SSLSupport;

public class LogstashClient implements Client {

    private static final TraceComponent tc = Tr.register(LogstashClient.class, "logstashCollector",
                                                         "com.ibm.ws.logstash.collector.internal.resources.LoggingMessages");

    private final LumberjackClient lumberjackClient;
    private volatile boolean connectionRetry;
    private volatile boolean connectionInitialized;

    //Wait time in milliseconds
    private final int CONNECTION_RETRY_WAIT_TIME = 5000;

    public LogstashClient(String sslConfig, SSLSupport sslSupport) throws SSLException {
        lumberjackClient = new LumberjackClient(sslConfig, sslSupport);
        connectionInitialized = connectionRetry = false;
    }

    @Override
    @FFDCIgnore(value = { IOException.class, InterruptedException.class })
    public void connect(String hostName, int port) throws IOException {
        try {
            // logmet seems to have a keepalive of about 20-30 seconds.
            // Proactively close our connection if this connection hasn't been used in a while
            boolean refreshingConnection = false;
            if (lumberjackClient.isSocketAvailable() && lumberjackClient.isConnectionStale()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "Connection has timed out - will reconnect before trying to use it");

                // avoid printing any info messages in this case -- would be too verbose to do so
                // no change to connectionInitialized
                lumberjackClient.close();
                refreshingConnection = true;
                connectionInitialized = false;
            }

            // We are re-trying to connect because of a connection failure
            // or a bad connection configuration, pause for a short while.
            if (connectionRetry) {
                try {
                    Thread.sleep(CONNECTION_RETRY_WAIT_TIME);
                } catch (InterruptedException e) {
                    //Ignore and continue
                }
            }

            lumberjackClient.connect(hostName, port);
            if (!connectionInitialized) {
                if (!refreshingConnection)
                    Tr.info(tc, "LOGSTASH_CONNECTION_ESTABLISHED", hostName, String.valueOf(port));
                connectionInitialized = true;
            }
            connectionRetry = false;
        } catch (IOException e) {
            if (!connectionRetry) {
                connectionRetry = true;
                Tr.warning(tc, "LOGSTASH_CONNECTION_FAILED", hostName, String.valueOf(port));
            }
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            lumberjackClient.close();
        } finally {
            //A connection has failed and if we issue connection closed message
            //after that it will not look right as we have already informed the
            //user that the connection has failed
            if (connectionInitialized && !connectionRetry)
                Tr.info(tc, "LOGSTASH_CONNECTION_CLOSED");
            connectionInitialized = false;
        }
    }

    @Override
    @FFDCIgnore(value = { IOException.class })
    public void sendData(List<Object> dataObjects) throws IOException {
        try {
            int numObjects = dataObjects.size();
            long frameStartTime = System.nanoTime();
            //Write a window frame to the wire that sets the window size
            //to the number of events in the list
            lumberjackClient.writeWindowFrame(numObjects);
            //Create the corresponding data frame for each event
            byte[] dataFrames = lumberjackClient.createDataFrames(dataObjects);
            //Compress the data frames into a single compressed frame
            //and write to the wire.
            byte[] compressedFrame = lumberjackClient.createCompressedFrame(dataFrames);
            lumberjackClient.writeFrame(compressedFrame);
            traceTime(tc, frameStartTime, "FramingSending " + numObjects + " events ");

            //Wait for the ack frames
            long ackStartTime = System.nanoTime();
            lumberjackClient.readAckFrame();
            traceTime(tc, ackStartTime, "readAck " + numObjects + " events ");
        } catch (IOException e) {
            connectionRetry = true;
            Tr.warning(tc, "LOGSTASH_CONNECTION_NOT_AVAILABLE");
            throw e;
        }
    }

    private static void traceTime(TraceComponent tc, long startTime, String label) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            long endTime = System.nanoTime();
            String s = String.format(label + ": %10.3f ms", (endTime - startTime) / 1000000.0f);
            Tr.event(tc, s);
        }
    }

}
