/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

/**
 * An IncidentStreamImpl is a lightweight implementation of an Incident stream
 * 
 */
public final class IncidentSummaryLogger {
    public void logIncidentSummary(OutputStream os, List<IncidentImpl> incidents) {
        PrintStream ps = new PrintStream(os);
        try {
            ps.println();
            ps.println(" Index  Count  Time of first Occurrence    Time of last Occurrence     Exception SourceId ProbeId");
            ps.println("------+------+---------------------------+---------------------------+---------------------------");
            int i = -1;
            for (IncidentImpl incident : incidents) {
                ++i;
                ps.println(incident.formatSummaryEntry(i));
            }
            ps.println("------+------+---------------------------+---------------------------+---------------------------");
        } finally {
            ps.flush();
        }
    }
}