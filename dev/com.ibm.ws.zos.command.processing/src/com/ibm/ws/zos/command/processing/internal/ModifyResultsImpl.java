/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.command.processing.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.wsspi.zos.command.processing.ModifyResults;

/**
 *
 */
public class ModifyResultsImpl implements ModifyResults {

    int completionStatus = ModifyResults.UNKNOWN_COMMAND;
    List<String> responses = null;
    boolean responsesContainMSGIDs = false;

    private final Map<String, Object> props = Collections.synchronizedMap(new HashMap<String, Object>());

    /** {@inheritDoc} */
    @Override
    public int getCompletionStatus() {
        return completionStatus;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getResponses() {
        return responses;
    }

    /** {@inheritDoc} */
    //@Override
    public void setCompletionStatus(int completionStatus) {
        this.completionStatus = completionStatus;
    }

    /** {@inheritDoc} */
    //@Override
    public void setResponses(List<String> inResponses) {
        this.responses = inResponses;
    }

    /** {@inheritDoc} */
    @Override
    public Object getProperty(String key) {
        return props.get(key);
    }

    /** {@inheritDoc} */
    @Override
    public void setProperty(String key, Object value) {
        props.put(key, value);
        return;
    }

    /** {@inheritDoc} */
    @Override
    public boolean responsesContainMSGIDs() {
        return responsesContainMSGIDs;
    }

    /** {@inheritDoc} */
    @Override
    public void setResponsesContainMSGIDs(boolean value) {
        responsesContainMSGIDs = value;
    }

    /**
     * toString
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("\nModifyResults: \n\tCompletionStatus = ");
        sb.append(Integer.toHexString(completionStatus));
        sb.append(", \n\tresponses = (");
        List<String> responseMsgs = responses;
        if ((responseMsgs != null) && !responseMsgs.isEmpty()) {
            Iterator<String> it = responseMsgs.iterator();
            while (it.hasNext()) {
                String currentMsg = it.next();
                sb.append(currentMsg);
                if (it.hasNext()) {
                    sb.append(",");
                }
            }
        }
        sb.append("), \n\tResponsesContainMSGIDs = ");
        sb.append(Boolean.toString(responsesContainMSGIDs));
        return sb.toString();
    }
}
