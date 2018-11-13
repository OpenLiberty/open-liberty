/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.security.openidconnect.clients.common;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interface to communicate with the common RedirectionProcessor.
 */
public interface RedirectionEntry {

    ConvergedClientConfig getConvergedClientConfig(HttpServletRequest request, String clientId);

    void handleNoState(HttpServletRequest request, HttpServletResponse response) throws IOException;

    void sendError(HttpServletRequest request, HttpServletResponse response) throws IOException;

}
