/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.test.server;

import com.ibm.wsspi.genericbnf.exception.UnsupportedProtocolVersionException;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.channel.values.StatusCodes;
import com.ibm.wsspi.http.channel.values.VersionValues;

/**
 * A work queue used to process HTTP requests. This is required because
 * the notification received by a connection link that there is new work to
 * do is invoked on the TCPChannel's thread. Since servicing a HTTP request
 * is somewhat heavyweight, we should switch threads to do this.
 */
public class HTTPServerWorkQueue {

    private static HTTPServerWorkQueue instanceRef = null;

    /**
     * Constructor
     * 
     */
    private HTTPServerWorkQueue() {
        // nothing
    }

    /**
     * This class implements a singleton pattern. Use this method to retrieve
     * a reference to the instance.
     * 
     * @concurrency $none
     * @return HTTPServerWorkQueue
     */
    public static synchronized HTTPServerWorkQueue getRef() {

        if (instanceRef == null) {
            instanceRef = new HTTPServerWorkQueue();
        }
        return instanceRef;
    }

    /**
     * Helper method to create a default response when the request does
     * not match a given testcase.
     * 
     * @param resp
     * @param req
     */
    private void createNewResponse(
                                   HttpResponseMessage resp, HttpRequestMessage req) {
        resp.setStatusCode(StatusCodes.OK);
        try {
            resp.setVersion(req.getVersion());
        } catch (UnsupportedProtocolVersionException e) {
            resp.setVersion(VersionValues.V11);
        }
        resp.setHeader(HttpHeaderKeys.HDR_CONNECTION, "Close");
    }

    /**
     * Method for handling an inbound request.
     * 
     * @param link
     */
    public void doWork(HTTPServerConnLink link) {

        HttpInboundServiceContext inboundContext =
                        (HttpInboundServiceContext) link.getDeviceLink().getChannelAccessor();
        HttpRequestMessage req = inboundContext.getRequest();
        HttpResponseMessage resp = inboundContext.getResponse();
        boolean bStopping = "/KILLSERVER".equals(req.getRequestURI());

        createNewResponse(resp, req);
        try {
            inboundContext.finishResponseMessage(null);
            link.close(link.getVirtualConnection(), null);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bStopping) {
                // give the response some time to complete
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                    // nothing to do
                }
                cleanup();
            }
        }
    }

    /**
     * Destroy any unnecessary items at the end of the test.
     * 
     */
    private void cleanup() {
        instanceRef = null;
    }

    /**
     * Add work to the work queue.
     * 
     * @param connlink
     */
    public void queueWork(HTTPServerConnLink connlink) {
        doWork(connlink);
    }
}
