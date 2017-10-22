/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2016
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.collector;

import java.io.IOException;
import java.util.List;

/**
 * Client interface used for LogstashClients and LogmetClients
 */
public interface Client {

    public void connect(String hostName, int port) throws IOException;

    public void sendData(List<Object> dataObjects) throws IOException;

    public void close() throws IOException;

}
