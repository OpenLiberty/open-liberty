/*******************************************************************************
 * Copyright (c) 2004, 2018 IBM Corporation and others.
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
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;

public class AccessLogRequestProtocol extends AccessLogData {

    public AccessLogRequestProtocol() {
        super("%H");
    }

    @Override
    public boolean set(StringBuilder accessLogEntry,
                       HttpResponseMessage response, HttpRequestMessage request, Object data) {

        String protocol = null;

        if (Objects.nonNull(request)) {

            HttpInboundServiceContext serviceContext = (request.getServiceContext() instanceof HttpInboundServiceContext) ? (HttpInboundServiceContext) request.getServiceContext() : null;

            if (Objects.nonNull(serviceContext)) {
                protocol = serviceContext.useForwardedHeadersInAccessLog() ? serviceContext.getForwardedRemoteProto() : null;
            }

            if (Objects.isNull(protocol)) {
                protocol = request.getVersion();
            }

        }

        logSafe(accessLogEntry, protocol);

        return Boolean.TRUE;
    }

}
