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

import com.ibm.wsspi.channelfw.InterChannelCallback;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * Generic write callback used by the FVT server programs. This is used
 * when finishing the response message to the client.
 */
@SuppressWarnings("unused")
public class ResponseWriteCallback implements InterChannelCallback {

    /** Singleton instance of this class */
    private static ResponseWriteCallback myInstance = null;

    /** Constructor */
    private ResponseWriteCallback() {
        // nothing
    }

    /**
     * Create the singleton instance of the class here
     * 
     * @concurrency $none
     */
    static private synchronized void createSingleton() {
        if (null == myInstance) {
            myInstance = new ResponseWriteCallback();
        }
    }

    /**
     * Access the singleton instance of this class.
     * 
     * @return ResponseWriteCallback
     */
    public static final ResponseWriteCallback getRef() {
        if (null == myInstance) {
            createSingleton();
        }
        return myInstance;
    }

    /**
     * Called when a write request has completed
     * 
     * @param vc
     */
    public void complete(VirtualConnection vc) {
        // after writing the response message
        HTTPServerConnLink link =
                        (HTTPServerConnLink) vc.getStateMap().get("TestServerConnLink");
        link.close(vc, null);
    }

    /**
     * Called when an error occurred while writing data
     * 
     * @param vc
     * @param t
     */
    public void error(VirtualConnection vc, Throwable t) {
        // nothing to do
    }

}
