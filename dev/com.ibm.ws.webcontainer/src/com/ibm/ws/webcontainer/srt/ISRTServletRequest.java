/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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
package com.ibm.ws.webcontainer.srt;

import javax.servlet.http.HttpServletRequest;

import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

public interface ISRTServletRequest {

    SRTRequestContext getRequestContext();

    void resetPathElements();

    void setSSLAttributesInRequest(HttpServletRequest httpServletReq, String cs);
    
    String getCipherSuite();

    String getHeader(HttpHeaderKeys headerKey);

    static String getHeader(HttpServletRequest request, HttpHeaderKeys headerKey) {
        if (request instanceof ISRTServletRequest) {
            return ((ISRTServletRequest) request).getHeader(headerKey);
        }
        return request.getHeader(headerKey.getName());
    }
}
