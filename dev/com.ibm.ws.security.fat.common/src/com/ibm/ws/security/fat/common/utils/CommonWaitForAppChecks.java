/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.utils;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.security.fat.common.MessageConstants;

public class CommonWaitForAppChecks {

    public static List<String> getSSLChannelReadyMsgs() throws Exception {
        List<String> waitForMessages = new ArrayList<String>();
        return getSSLChannelReadyMsgs(waitForMessages);
    }

    public static List<String> getSSLChannelReadyMsgs(List<String> waitForMessages) throws Exception {
        waitForMessages.add(MessageConstants.CWWKO0219I_SSL_CHANNEL_READY);
        return waitForMessages;
    }

    public static List<String> getBasicSecurityReadyMsgs() throws Exception {
        List<String> waitForMessages = new ArrayList<String>();
        return getBasicSecurityReadyMsgs(waitForMessages);
    }

    public static List<String> getBasicSecurityReadyMsgs(List<String> waitForMessages) throws Exception {
        waitForMessages.add(MessageConstants.CWWKS0008I_SECURITY_SERVICE_READY);
        return waitForMessages;
    }

    public static List<String> getSecurityReadyMsgs() throws Exception {
        List<String> waitForMessages = new ArrayList<String>();
        return getBasicSecurityReadyMsgs(getSSLChannelReadyMsgs(waitForMessages));
    }

}