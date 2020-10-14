/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.logging.internal;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Class to hold data to be traced, the description of the data, and a
 * preferred formatted representation of the data for trace.
 */
@Trivial
class TracedData {
    final int traceKey;
    final String description;
    final Object item;
    final String formatted;

    TracedData(int traceKey, String description, Object item, String formatted) {
        this.traceKey = traceKey;
        this.description = description;
        this.item = item;
        this.formatted = formatted;
    }

    int getTraceKey() {
        return traceKey;
    }

    String getDescription() {
        return description;
    }

    Object getItem() {
        return item;
    }

    public String getFormatted() {
        return formatted;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  ");
        sb.append(description);
        sb.append(": ").append(formatted);
        return sb.toString();
    }
}