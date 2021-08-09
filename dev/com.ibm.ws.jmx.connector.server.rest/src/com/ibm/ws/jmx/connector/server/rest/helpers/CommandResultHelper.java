/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.server.rest.helpers;

import java.io.Serializable;

/**
 *
 */
public class CommandResultHelper implements Serializable {

    private String description;
    private String status;
    private int returnCode;
    private String stdOut;
    private String stdErr;
    private long timestamp;

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return the returnCode
     */
    public int getReturnCode() {
        return returnCode;
    }

    /**
     * @param returnCode the returnCode to set
     */
    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    /**
     * @return the stdOut
     */
    public String getStdOut() {
        return stdOut;
    }

    /**
     * @param stdOut the stdOut to set
     */
    public void setStdOut(String stdOut) {
        this.stdOut = stdOut;
    }

    /**
     * @return the stdErr
     */
    public String getStdErr() {
        return stdErr;
    }

    /**
     * @param stdErr the stdErr to set
     */
    public void setStdErr(String stdErr) {
        this.stdErr = stdErr;
    }

    /**
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

}
