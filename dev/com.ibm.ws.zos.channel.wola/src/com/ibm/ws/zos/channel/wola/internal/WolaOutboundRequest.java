/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.resource.ResourceException;

import com.ibm.ws.zos.channel.wola.WolaInterruptObjectBridge;
import com.ibm.ws.zos.channel.wola.WolaJcaRequestInfo;
import com.ibm.ws.zos.channel.wola.internal.msg.CicsLinkServerContext;
import com.ibm.ws.zos.channel.wola.internal.msg.WolaMessage;
import com.ibm.ws.zos.channel.wola.internal.msg.WolaMessageContextArea;
import com.ibm.ws.zos.channel.wola.internal.msg.WolaMessageParseException;
import com.ibm.ws.zos.channel.wola.internal.msg.WolaServiceNameContext;

/**
 * Encapsulates a WOLA outbound request from Liberty to a service hosted by
 * a WOLA client.
 *
 */
public class WolaOutboundRequest {

    /**
     * DS ref
     */
    private WolaOutboundConnMgr wolaOutboundConnMgr;

    /**
     * DS ref
     */
    private WolaInterruptObjectBridge odiBridge;

    /**
     * Inject DS - called by the endpoint, not by DS directly.
     */
    public WolaOutboundRequest setWolaOutboundConnMgr(WolaOutboundConnMgr wolaOutboundConnMgr) {
        this.wolaOutboundConnMgr = wolaOutboundConnMgr;
        return this;
    }

    /**
     * Inject DS - called by the endpoint, not by DS directly.
     */
    public WolaOutboundRequest setWolaInterruptObjectBridge(WolaInterruptObjectBridge odiBridge) {
        this.odiBridge = odiBridge;
        return this;
    }

    /**
     * Invoke the given serviceName, hosted by the client with the given registerName,
     * passing along the given appData to the service. The response is returned as
     * a byte[].
     *
     * @param registerName       - The registration name of the client hosting the service
     * @param serviceName        - The service to invoke
     * @param appData            - Parameter data for the service
     * @param wolaJcaRequestInfo - Extra data for the request
     *
     * @return The response data
     */
    public byte[] invoke(String registerName,
                         String serviceName,
                         byte[] appData,
                         WolaJcaRequestInfo wolaJcaRequestInfo) throws IOException, ResourceException {

        // Send the request.  This method returns a Future.
        // The Future is posted when the response is ready.
        WolaOutboundRequestService outboundRequestService = getWolaConnLink(registerName, serviceName,
                                                                            wolaJcaRequestInfo.getConnectionWaitTimeout()).getOutboundRequestService();
        WolaMessage requestMessage = buildMessage(serviceName,
                                                  appData,
                                                  wolaJcaRequestInfo);
        Future<WolaMessage> responseFuture = outboundRequestService.sendRequest(requestMessage);

        // If requestTiming-1.0 is configured, register an ODI before we start waiting.
        byte[] responseData = null;
        Object odiToken = null;
        if (odiBridge != null) {
            odiToken = odiBridge.register(responseFuture);
        }

        try {
            responseData = waitOnResponse(responseFuture);
        } finally {
            if (odiToken != null) {
                odiBridge.deregister(odiToken);
            }
        }

        return responseData;
    }

    /**
     * @param registerName - client's registration name
     * @param serviceName  - name of service hosted by client
     * @param timeout_s    - the time to wait (in seconds) for the client service to become available before giving up
     *
     * @return The WolaConnLink associated with the given client registerName and serviceName.
     *
     * @throws IOException if the WolaConnLink could not be found.
     */
    protected WolaConnLink getWolaConnLink(String registerName, String serviceName, int timeout_s) throws IOException {

        WolaConnLink wolaConnLink = wolaOutboundConnMgr.getWolaConnLink(registerName, serviceName, timeout_s);

        if (wolaConnLink == null) {
            throw new IOException("There is no WOLA connection for a client with registration name " + registerName + " and service name " + serviceName);
        }

        return wolaConnLink;
    }

    /**
     * @return a new WOLA request message with the given appData in its data area.
     */
    protected WolaMessage buildMessage(String serviceName,
                                       byte[] appData,
                                       WolaJcaRequestInfo wolaJcaRequestInfo) throws IOException {

        try {
            WolaMessageContextArea contextArea = new WolaMessageContextArea().addWolaMessageContext(new WolaServiceNameContext(serviceName)).addWolaMessageContext(new CicsLinkServerContext(wolaJcaRequestInfo));

            return new WolaMessage().setMvsUserId(wolaJcaRequestInfo.getMvsUserID()).setJcaConnectionId(wolaJcaRequestInfo.getConnectionID()).appendContextArea(contextArea).appendDataArea(appData);

        } catch (WolaMessageParseException wmpe) {
            // Wrap in an IOException.
            throw new IOException(wmpe);
        }
    }

    /**
     * @param responseFuture - the Future on which to wait
     *
     * @return the response data (via responseFuture.get())
     *
     * @throws IOException if anything goes wrong
     */
    protected byte[] waitOnResponse(Future<WolaMessage> responseFuture) throws ResourceException, IOException {

        try {
            // Wait on the Future for the response.
            WolaMessage wolaMsg = responseFuture.get(); // Will always be set.
            byte[] responseData = wolaMsg.getDataArea();
            if (wolaMsg.isException() == false) {
                return responseData;
            } else {
                throw new ResourceException(new String(responseData, "Cp1047"));
            }

            // Convert exceptions to IOExceptions.
        } catch (ExecutionException ee) {
            if (ee.getCause() instanceof IOException) {
                throw (IOException) ee.getCause();
            } else {
                throw new IOException(ee.getCause());
            }
        } catch (InterruptedException ie) {
            throw new IOException(ie);
        }
    }

}
