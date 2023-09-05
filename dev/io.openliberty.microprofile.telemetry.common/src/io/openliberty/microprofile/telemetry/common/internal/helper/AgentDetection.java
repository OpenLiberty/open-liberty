/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.common.internal.helper;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Methods to detect whether the OpenTelemetry automatic instrumentation agent is active
 */
public class AgentDetection {

    private static final boolean IS_AGENT_ACTIVE = checkAgent();

    @FFDCIgnore(Exception.class)
    private static boolean checkAgent() {
        try {
            Class.forName("io.opentelemetry.javaagent.bootstrap.AgentStarter");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns whether the OpenTelemetry instrumentation agent is active
     *
     * @return {@code true} if the agent is active, otherwise {@code false}
     */
    public static boolean isAgentActive() {
        return IS_AGENT_ACTIVE;
    }

}
