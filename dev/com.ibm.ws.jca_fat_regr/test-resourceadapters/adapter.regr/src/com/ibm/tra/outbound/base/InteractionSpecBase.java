/*******************************************************************************
 * Copyright (c) 2005, 2022 IBM Corporation and others.
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

package com.ibm.tra.outbound.base;

@SuppressWarnings("serial")
public class InteractionSpecBase implements javax.resource.cci.InteractionSpec, java.io.Serializable {

    private String functionName = null;
    private String interactionVerb = "Default";
    private int executionTimeout = 0;

    public static final String EXECUTE_GOOD = "ex_good";
    public static final String EXECUTE_BAD = "ex_bad";

    public InteractionSpecBase() {
        interactionVerb = InteractionSpecBase.EXECUTE_GOOD;

    }

    public String getFunctionName() {
        return functionName;
    }

    public String getInteractionVerb() {
        return interactionVerb;
    }

    public int getExecutionTimeout() {
        return executionTimeout;
    }

    public void setFunctionName(String function) {
        functionName = function;
    }

    public void setInteractionVerb(String interVerb) {
        interactionVerb = interVerb;
    }

    public void setExecutionTimeout(int timeout) {
        executionTimeout = timeout;
    }

    /**
     * Note: It is up to the RA provider to provide a way to return the InteractionSpec in a readable format.
     * For FVT purpose, we return a String in a simple way.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("functionName=").append(functionName);
        buf.append(", interactionVerb=").append(interactionVerb);
        buf.append(", executionTimeout=").append(executionTimeout);

        return buf.toString();
    }
}
