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
package com.ibm.ws.wsoc.external;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.HandshakeResponse;

import com.ibm.websphere.ras.annotation.Sensitive;

public class HandshakeResponseExt implements HandshakeResponse {

    private Map<String, List<String>> headers = null;

    public HandshakeResponseExt(@Sensitive Map<String, List<String>> _headers) {

        headers = _headers;
        // if there are no headers, then _headers should be empty list, not null.  Following servlet spec (wsoc javadoc doesn't say)
        if (headers == null) {
            headers = new HashMap<String, List<String>>();
        }
    }

    @Override
    @Sensitive
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

}
