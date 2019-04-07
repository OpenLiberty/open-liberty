/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.fat.proxy;

import com.ibm.websphere.simplicity.log.Log;

public class HttpProxyServer extends Thread {
    private static int port = 0;
    private static HttpProxyServer uniqueInstance = null;

    private HttpProxyServer(int p) {
        port = p;
        Log.info(HttpProxyServer.class, "HttpProxyServer", "Construct proxy main thread. ");
        start();
    }

    public static synchronized void startHttpProxyServer(int port) {

        if (uniqueInstance == null) {
            uniqueInstance = new HttpProxyServer(port);
            Log.info(HttpProxyServer.class, "startHttpProxyServer", "Started proxy server thread.");
        } else {
            Log.info(HttpProxyServer.class, "startHttpProxyServer", "Proxy server is started.");
        }

    }

    public static boolean stopHttpProxyServer(int p) {
        if (HttpProxy.stopProxy(p)) {
            port = 0;
            uniqueInstance = null;
            Log.info(HttpProxyServer.class, "stopHttpProxyServer", "Proxy server stopped at port: " + p);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void run() {
        if (port != 0) {
            Log.info(HttpProxyServer.class, "run", "Start proxy server thread. ");
            HttpProxy.startProxy(port);
            Log.info(HttpProxyServer.class, "run", "Proxy server thread stopped. ");
        }
    }
}
