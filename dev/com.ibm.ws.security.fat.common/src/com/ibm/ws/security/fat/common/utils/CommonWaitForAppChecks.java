/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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
package com.ibm.ws.security.fat.common.utils;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.security.fat.common.MessageConstants;

public class CommonWaitForAppChecks {

    public static List<String> getSSLChannelReadyMsgs() {
        List<String> waitForMessages = new ArrayList<String>();
        return getSSLChannelReadyMsgs(waitForMessages);
    }

    public static List<String> getSSLChannelReadyMsgs(List<String> waitForMessages) {
        waitForMessages.add(MessageConstants.CWWKO0219I_SSL_CHANNEL_READY);
        return waitForMessages;
    }

    public static List<String> getBasicSecurityReadyMsgs() {
        List<String> waitForMessages = new ArrayList<String>();
        return getBasicSecurityReadyMsgs(waitForMessages);
    }

    public static List<String> getBasicSecurityReadyMsgs(List<String> waitForMessages) {
        waitForMessages.add(MessageConstants.CWWKS0008I_SECURITY_SERVICE_READY);
        return waitForMessages;
    }

    public static List<String> getSecurityReadyMsgs() {
        List<String> waitForMessages = new ArrayList<String>();
        return getBasicSecurityReadyMsgs(getSSLChannelReadyMsgs(waitForMessages));
    }

    public static List<String> getLTPAReadyMsgs(List<String> waitForMessages) {
        if (waitForMessages == null) {
            waitForMessages = new ArrayList<String>();
        }
        waitForMessages.add(MessageConstants.CWWKS4105I_LTPA_CONFIG_READY);
        return waitForMessages;
    }
}