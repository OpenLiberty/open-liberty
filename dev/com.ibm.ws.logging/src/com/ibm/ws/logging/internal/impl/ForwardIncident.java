/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import com.ibm.wsspi.logging.Incident;

/**
 * This implementation of Incident has a reference to the Throwable, caller and objectArray[] passed
 * into FFDC for the exception that occurred. These objects are used by the getIntrospectedCallerDump()
 * method. We do not want to keep these objects referenced in memory in the collection of Incidents.
 */
public class ForwardIncident implements Incident {

    private final Incident originalIncident;
    private final Throwable th;
    private final Object callerThis;
    private final Object[] objectArray;

    ForwardIncident(Incident originalIncident, Throwable th, Object callerThis, Object[] objectArray) {
        this.originalIncident = originalIncident;
        this.th = th;
        this.callerThis = callerThis;
        this.objectArray = objectArray;
    }

    @Override
    public String getSourceId() {
        return originalIncident.getSourceId();
    }

    @Override
    public String getProbeId() {
        return originalIncident.getProbeId();
    }

    @Override
    public String getExceptionName() {
        return originalIncident.getExceptionName();
    }

    @Override
    public int getCount() {
        return originalIncident.getCount();
    }

    @Override
    public long getTimeStamp() {
        return originalIncident.getTimeStamp();
    }

    @Override
    public Date getDateOfFirstOccurrence() {
        return originalIncident.getDateOfFirstOccurrence();
    }

    @Override
    public String getLabel() {
        return originalIncident.getLabel();
    }

    @Override
    public long getThreadId() {
        return originalIncident.getThreadId();
    }

    @Override
    public String getIntrospectedCallerDump() {
        String dump = null;
        ByteArrayOutputStream oStream = new ByteArrayOutputStream();

        IncidentStreamImpl iStream = null;

        try {
            iStream = new IncidentStreamImpl(oStream);
        } catch (Exception e) {
            // darn. Prevent the exception logging the error from percolating upward
        }

        if (iStream != null) {
            try {
                new IncidentLogger().logIncident(iStream, this, th, callerThis, objectArray, true);
            } catch (Throwable e) {
                iStream.printStackTrace(e);
            } finally {
                LoggingFileUtils.tryToClose(iStream);

                try {
                    dump = oStream.toString("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    // darn. Prevent the exception logging the error from percolating upward
                }
            }
        }

        return dump;
    }
}
