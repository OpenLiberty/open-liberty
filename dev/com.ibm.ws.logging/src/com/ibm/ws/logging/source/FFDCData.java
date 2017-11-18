/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.source;

/**
 *
 */
public class FFDCData {

    private final String exceptionName;
    private final String message;
    private final String className;
    private final int count;
    private final long timeStamp;
    private final long timeOfFirstOccurrence;
    private final String label;
    private final String probeID;
    private final String sourceID;
    private final int threadID;
    private final String stackTrace;
    private final String objectDetails;
    private final String sequence;

    public FFDCData(String exceptionName, String message, String className, long timeStamp, long timeOfFirstOccurrence, int count, String label, String probeID, String sourceID,
                    int threadID, String stackTrace, String objectDetails, String sequence) {
        this.exceptionName = exceptionName;
        this.message = message;
        this.className = className;
        this.timeStamp = timeStamp;
        this.timeOfFirstOccurrence = timeOfFirstOccurrence;
        this.count = count;
        this.label = label;
        this.probeID = probeID;
        this.sourceID = sourceID;
        this.threadID = threadID;
        this.stackTrace = stackTrace;
        this.objectDetails = objectDetails;
        this.sequence = sequence;
    }

    public String getClassName() {
        return className;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public String getExceptionName() {
        return exceptionName;
    }

    public String getProbeID() {
        return probeID;
    }

    public String getSourceID() {
        return sourceID;
    }

    public int getThreadID() {
        return threadID;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public int getCount() {
        return count;
    }

    public long getTimeOfFirstOccurrence() {
        return timeOfFirstOccurrence;
    }

    public String getLabel() {
        return label;
    }

    public String getObjectDetails() {
        return objectDetails;
    }

    public String getSequence() {
        return sequence;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "FFDCData ["
               + "exceptionName=" + exceptionName
               + ", message=" + message
               + ", className=" + className
               + ", count=" + count
               + ", timeStamp=" + timeStamp
               + ", timeOfFirstOccurrence=" + timeOfFirstOccurrence
               + ", label=" + label
               + ", probeID=" + probeID
               + ", sourceID=" + sourceID
               + ", threadID=" + threadID
               + ", stackTrace=" + stackTrace
               + ", objectDetails=" + objectDetails
               + ", sequence=" + sequence + "]";
    }

}
