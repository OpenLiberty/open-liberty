/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http;

@Deprecated
public class Alias {
    private final String hostname;
    private final String port;

    @Deprecated
    public Alias(String host, String port) {
        hostname = host;
        this.port = port;
    }

    @Deprecated
    public String getHostname() {
        return hostname;
    }

    @Deprecated
    public String getPort() {
        return port;
    }

    //LI3816
    @Override
    public String toString() {
        return hostname + ":" + port;
    }
}
