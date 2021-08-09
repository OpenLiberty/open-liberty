/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.utils;

import java.util.HashSet;
import java.util.Set;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;

public class WebClientTracker {

    protected static Class<?> thisClass = WebClientTracker.class;

    Set<WebClient> webClients = new HashSet<WebClient>();

    public void addWebClient(WebClient client) {
        if (client == null) {
            return;
        }
        webClients.add(client);
    }

    public Set<WebClient> getWebClients() {
        return new HashSet<WebClient>(webClients);
    }

    public void closeAllWebClients() throws Exception {

        if (webClients != null) {
            for (WebClient client : webClients) {
                try {
                    if (client != null) {
                        Log.info(thisClass, "closeAllWebClients", "Closing WebClient");
                        client.close();
                    }
                } catch (Exception e) {
                    Log.info(thisClass, "closeAllWebClients", "An exception occurred while closing a WebClient: " + e.toString());
                }
            }
        }
    }

}
