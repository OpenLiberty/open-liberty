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

import com.ibm.ws.http.channel.internal.HttpResponseMessageImpl;
import com.ibm.ws.http.netty.MSP;
import com.ibm.ws.http.netty.message.NettyResponseMessage;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;

public class AccessLogResponseSize extends AccessLogData {

    public AccessLogResponseSize() {
        super("%b");
        // %b
        // Size of response in bytes, excluding HTTP headers.
        // In CLF format, i.e. a '-' rather than a 0 when no bytes are sent.
    }

    @Override
    public boolean set(StringBuilder accessLogEntry,
                       HttpResponseMessage response, HttpRequestMessage request, Object data) {

        long responseSize = getResponseSize(response, request, data);

        if (responseSize != -999) {
            accessLogEntry.append(responseSize);
        } else {
            accessLogEntry.append("-");
        }

        return true;
    }

    public static long getResponseSize(HttpResponseMessage response, HttpRequestMessage request, Object data) {
        long responseSize = -999;
        if (Objects.nonNull(response)) {

            if (response instanceof NettyResponseMessage) {
                NettyResponseMessage nettyResponseMessage = (NettyResponseMessage) response;
                responseSize = Objects.nonNull(nettyResponseMessage) ? nettyResponseMessage.getServiceContext().getNumBytesWritten() : -999;
            }

            else if (response instanceof HttpResponseMessageImpl) {
                HttpResponseMessageImpl legacyResponseMessage = (HttpResponseMessageImpl) response;

                responseSize = Objects.nonNull(legacyResponseMessage) ? legacyResponseMessage.getServiceContext().getNumBytesWritten() : -999;
            }

        }
        MSP.log("%b access log directive set to: " + responseSize);
        return responseSize;
    }
}
