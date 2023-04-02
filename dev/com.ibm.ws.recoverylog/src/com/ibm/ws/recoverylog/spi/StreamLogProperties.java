/*******************************************************************************
 * Copyright (c) 2002, 2021 IBM Corporation and others.
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

package com.ibm.ws.recoverylog.spi;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Implementation of the LogProperties interface on z/OS. This implementation
 * writes to the System Logger instead of to the filesystem (HFS).
 */
public class StreamLogProperties implements LogProperties {
    private static final TraceComponent tc = Tr.register(StreamLogProperties.class, TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);

    // The name of the logstream compression interval custom property
    static public final String COMPRESS_INTERVAL_NAME = /* @PK08027A */
                    new String("RLS_LOGSTREAM_COMPRESS_INTERVAL"); /* @PK08027A */

    // The unique RLI value.
    private int _logIdentifier = 0;

    //The unique RLN value.
    private String _logName = null;

    // The name of the LogStream.
    private String _streamName = null;

    public StreamLogProperties(int logId, String logName, String streamName) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "StreamLogProperties", logId, logName, streamName);

        _logIdentifier = logId;
        _logName = logName;

        if (streamName == null) {
            IllegalArgumentException iae = new IllegalArgumentException("Null logstream high level qualifier");
            if (tc.isEntryEnabled())
                Tr.event(tc, iae.getMessage(), iae);
            throw iae;
        }

        _streamName = streamName.toUpperCase();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "StreamLogProperties", this);
    }

    @Override
    public int logIdentifier() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "logIdentifier", _logIdentifier);
        return _logIdentifier;
    }

    @Override
    public String logName() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "logName", this, _logName);
        return _logName;
    }

    public String streamName() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "streamName", this, _streamName);
        return _streamName;
    }

    /**
     * Determine if two LogProperties references are the same. @MD19650A.
     *
     * @param logProps The log properties to be checked
     * @return boolean true If compared objects are equal.
     */
    @Override
    public boolean equals(Object lp) {
        if (lp == null)
            return false;
        else if (lp == this)
            return true;
        else if (lp instanceof StreamLogProperties) {
            StreamLogProperties zlp = (StreamLogProperties) lp;
            if (zlp.logIdentifier() == this.logIdentifier() &&
                zlp.logName().equals(this.logName()) &&
                zlp.streamName().equals(this.streamName()))
                return true;
        }
        return false;
    }

    /**
     * HashCode implementation. @MD19650A
     *
     * @return int The hash code value.
     */
    @Override
    public int hashCode() {
        int hashCode = 0;

        hashCode += _logIdentifier / 3;
        hashCode += _logName.hashCode() / 3;
        hashCode += _streamName.hashCode() / 3;

        return hashCode;
    }
}
