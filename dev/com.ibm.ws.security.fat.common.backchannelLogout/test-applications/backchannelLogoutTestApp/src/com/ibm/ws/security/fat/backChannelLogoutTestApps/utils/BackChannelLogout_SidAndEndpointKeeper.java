/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.backChannelLogoutTestApps.utils;

public class BackChannelLogout_SidAndEndpointKeeper {

    public String sid;
    public String bclEndpoint;

    public BackChannelLogout_SidAndEndpointKeeper(String inSid, String inBclEndpoint) {
        this.sid = inSid;
        this.bclEndpoint = inBclEndpoint;
    }

    public String getSid() {
        return sid;
    }

    public String getBclEndpoint() {
        return bclEndpoint;
    }
}
