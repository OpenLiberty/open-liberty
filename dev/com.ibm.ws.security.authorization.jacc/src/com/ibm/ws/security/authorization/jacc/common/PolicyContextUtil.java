/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.authorization.jacc.common;

import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

public class PolicyContextUtil {

    public static String getContextId(WsLocationAdmin locationAdmin, String applicationName, String moduleName) {
        StringBuilder output = new StringBuilder();
        output.append(getHostName()).append("#").append(locationAdmin.resolveString("${wlp.user.dir}").replace('\\',
                                                                                                               '/')).append("#").append(locationAdmin.getServerName()).append("#");
        output.append(applicationName).append("#").append(moduleName);
        return output.toString();
    }

    /**
     * Get the host name.
     *
     * @return String value of the host name or "localhost" if not able to resolve
     */
    private static String getHostName() {
        String hostName = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                try {
                    return java.net.InetAddress.getLocalHost().getCanonicalHostName().toLowerCase();
                } catch (java.net.UnknownHostException e) {
                    return "localhost";
                }
            }
        });
        return hostName;
    }
}
