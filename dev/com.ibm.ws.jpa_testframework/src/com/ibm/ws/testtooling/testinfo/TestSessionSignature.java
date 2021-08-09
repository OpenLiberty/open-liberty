/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.testtooling.testinfo;

/**
 * Unique signature to identify a specific test invocation.
 *
 */
public class TestSessionSignature implements java.io.Serializable {
    private static final long serialVersionUID = 3038481768116554202L;
    private static volatile long auto_index = 0;

    private String sessionName;
    private long timestamp;
    private long index;

    public TestSessionSignature() {

    }

    public TestSessionSignature(String sessionName) {
        super();
        this.sessionName = sessionName;
        this.timestamp = System.currentTimeMillis();
        this.index = auto_index++;
    }

    public TestSessionSignature(String sessionName, long timestamp, long index) {
        super();
        this.sessionName = sessionName;
        this.timestamp = timestamp;
        this.index = index;
    }

    public TestSessionSignature(TestSessionSignature tss) {
        super();
        this.sessionName = tss.sessionName;
        this.timestamp = tss.timestamp;
        this.index = tss.index;
    }

    public final String getSessionName() {
        return sessionName;
    }

    public final long getTimestamp() {
        return timestamp;
    }

    public final long getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return "TestSessionSignature [sessionName=" + sessionName
               + ", timestamp=" + timestamp + ", index=" + index + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (index ^ (index >>> 32));
        result = prime * result
                 + ((sessionName == null) ? 0 : sessionName.hashCode());
        result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TestSessionSignature other = (TestSessionSignature) obj;
        if (index != other.index)
            return false;
        if (sessionName == null) {
            if (other.sessionName != null)
                return false;
        } else if (!sessionName.equals(other.sessionName))
            return false;
        if (timestamp != other.timestamp)
            return false;
        return true;
    }

}