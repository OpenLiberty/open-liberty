/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ras.instrument.internal.model;

import java.util.ArrayList;
import java.util.List;

public class TraceOptionsData {

    private final List<String> traceGroups = new ArrayList<String>();
    private String messageBundle;
    private boolean traceExceptionThrow;
    private boolean traceExceptionHandling;

    public TraceOptionsData() {
        super();
    }

    public TraceOptionsData(List<String> traceGroups, String messageBundle, boolean traceExceptionThrow, boolean traceExceptionHandling) {
        for (String traceGroup : traceGroups) {
            addTraceGroup(traceGroup);
        }
        setMessageBundle(messageBundle);
        this.traceExceptionThrow = traceExceptionThrow;
        this.traceExceptionHandling = traceExceptionHandling;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object != null && object.getClass() == TraceOptionsData.class) {
            TraceOptionsData traceOptions = (TraceOptionsData) object;
            return traceGroups.equals(traceOptions.traceGroups) &&
                   (messageBundle == null ? traceOptions.messageBundle == null : messageBundle.equals(traceOptions.messageBundle)) &&
                   traceExceptionThrow == traceOptions.traceExceptionThrow &&
                   traceExceptionHandling == traceOptions.traceExceptionHandling;
        }

        return false;
    }

    public List<String> getTraceGroups() {
        return traceGroups;
    }

    public void addTraceGroup(String traceGroup) {
        if (traceGroup != null && !traceGroup.equals("") && !traceGroups.contains(traceGroup)) {
            traceGroups.add(traceGroup);
        }
    }

    public boolean isTraceExceptionHandling() {
        return traceExceptionHandling;
    }

    public void setTraceExceptionHandling(boolean traceExceptionHandling) {
        this.traceExceptionHandling = traceExceptionHandling;
    }

    public boolean isTraceExceptionThrow() {
        return traceExceptionThrow;
    }

    public void setTraceExceptionThrow(boolean traceExceptionThrow) {
        this.traceExceptionThrow = traceExceptionThrow;
    }

    public void setMessageBundle(String messageBundle) {
        if (messageBundle != null && !messageBundle.equals("")) {
            this.messageBundle = messageBundle;
        }
    }

    public String getMessageBundle() {
        return this.messageBundle;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(";traceGroups=").append(traceGroups);
        sb.append(",messageBundle=").append(messageBundle);
        sb.append(",traceExceptionThrow=").append(traceExceptionThrow);
        sb.append(",traceExceptionHandling=").append(traceExceptionHandling);
        return sb.toString();
    }
}
