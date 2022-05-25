/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

public class OidcSessionInfo {
    private final String configId;
    private final String sub;
    private final String sid;
    private final String timestamp;

    public OidcSessionInfo(String configId, String sub, String sid, String timestamp) {
        this.configId = configId;
        this.sub = sub;
        this.sid = sid;
        this.timestamp = timestamp;
    }

    public String getConfigId() {
        return this.configId;
    }

    public String getSub() {
        return this.sub;
    }

    public String getSid() {
        return this.sid;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

}