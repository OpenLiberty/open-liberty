/*******************************************************************************
 * Copyright (c) 2004, 2020 IBM Corporation and others.
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
package com.ibm.ws.http.channel.internal.values;

import java.util.Objects;

import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;

public class AccessLogRemoteUser extends AccessLogData {

    public AccessLogRemoteUser() {
        super("%u");
    }

    @Override
    public boolean set(StringBuilder accessLogEntry,
                       HttpResponseMessage response, HttpRequestMessage request, Object data) {

        String remoteUser = getRemoteUser(response, request, data);

        if (remoteUser != null && !remoteUser.equals("")) {
            accessLogEntry.append(remoteUser);
        } else {
            accessLogEntry.append("-");
        }

        return true;
    }

    public static String getRemoteUser(HttpResponseMessage response, HttpRequestMessage request, Object data) {

        return (Objects.nonNull(request)) ? request.getRemoteUser() : null;

    }
}
