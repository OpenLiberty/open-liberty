/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.admin.internal;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.admin.InvalidFileStoreConfigurationException;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.SIBFileStore;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

/**
 *
 */
public class SIBFileStoreImpl implements SIBFileStore {

    /** RAS trace variable */
    private static final TraceComponent tc = SibTr.register(
                                                            JsMainAdminComponentImpl.class, JsConstants.TRGRP_AS,
                                                            JsConstants.MSG_BUNDLE);
    private static final TraceNLS nls = TraceNLS
                    .getTraceNLS(JsConstants.MSG_BUNDLE);
    private String uuid = null;
    // default filestore path
    private String path = WsLocationConstants.SYMBOL_SERVER_OUTPUT_DIR
                          + "/messaging/messageStore";

    // converting into Bytes
    private long logFileSize = JsAdminConstants.LOGFILESIZE_L * 1024 * 1024;
    private long fileStoreSize = JsAdminConstants.FILESTORESIZE_L * 1024 * 1024;
    private long minPermanentFileStoreSize = JsAdminConstants.MINPERMANENTFILESTORESIZE_L * 1024 * 1024;
    private long maxPermanentFileStoreSize = fileStoreSize / 2;
    private long minTemporaryFileStoreSize = JsAdminConstants.MINTEMPORARYFILESTORESIZE_L * 1024 * 1024;
    private long maxTemporaryFileStoreSize = fileStoreSize / 2;
    private boolean unlimitedTemporaryStoreSize = false;
    private boolean unlimitedPermanentStoreSize = false;

    /** {@inheritDoc} */
    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public void setUuid(String value) {
        this.uuid = value;

    }

    /**
     * @return the path
     */
    @Override
    public String getPath() {
        return path;
    }

    /**
     * @param path
     *            the path to set
     */
    @Override
    public void setPath(String path)
                    throws InvalidFileStoreConfigurationException {
        if (path == null || path.trim().isEmpty())
            throw new InvalidFileStoreConfigurationException(
                            nls.getFormattedMessage("INVALID_FS_PATH_SIAS0120", null,
                                                    null));
        this.path = path;
    }

    /**
     * @return the logFileSize
     */
    @Override
    public long getLogFileSize() {
        return logFileSize;
    }

    /**
     * @param logFileSize the logFileSize to set
     */
    @Override
    public void setLogFileSize(long logFileSize) throws InvalidFileStoreConfigurationException {
        this.logFileSize = logFileSize;
    }

    /**
     * @return the logFileSize
     */
    @Override
    public long getFileStoreSize() {
        return fileStoreSize;
    }

    /**
     * @param logFileSize the logFileSize to set
     */
    @Override
    public void setFileStoreSize(long fileStoreSize) throws InvalidFileStoreConfigurationException {

        if (fileStoreSize < (this.minPermanentFileStoreSize + this.minTemporaryFileStoreSize))
        {
            this.fileStoreSize = JsAdminConstants.FILESTORESIZE_L * 1024 * 1024;
        }
        else
        {
            this.fileStoreSize = fileStoreSize;
        }
        this.maxPermanentFileStoreSize = this.fileStoreSize / 2;
        this.maxTemporaryFileStoreSize = this.fileStoreSize / 2;

    }

    /**
     * @return the minPermanentFileStoreSize
     */
    @Override
    public long getMinPermanentFileStoreSize() {
        return minPermanentFileStoreSize;
    }

    /**
     * @param minPermanentFileStoreSize
     *            the minPermanentFileStoreSize to set
     */
    @Override
    public void setMinPermanentFileStoreSize(long minPermanentFileStoreSize) {
        this.minPermanentFileStoreSize = minPermanentFileStoreSize;
    }

    /**
     * @return the maxPermanentFileStoreSize
     */
    @Override
    public long getMaxPermanentFileStoreSize() {
        return maxPermanentFileStoreSize;
    }

    /**
     * @param maxPermanentFileStoreSize
     *            the maxPermanentFileStoreSize to set
     */
    @Override
    public void setMaxPermanentFileStoreSize(long maxPermanentFileStoreSize) {
        this.maxPermanentFileStoreSize = maxPermanentFileStoreSize;
    }

    /**
     * @return the minTemporaryFileStoreSize
     */
    @Override
    public long getMinTemporaryFileStoreSize() {
        return minTemporaryFileStoreSize;
    }

    /**
     * @param minTemporaryFileStoreSize
     *            the minTemporaryFileStoreSize to set
     */
    @Override
    public void setMinTemporaryFileStoreSize(long minTemporaryFileStoreSize) {
        this.minTemporaryFileStoreSize = minTemporaryFileStoreSize;
    }

    /**
     * @return the maxTemporaryFileStoreSize
     */
    @Override
    public long getMaxTemporaryFileStoreSize() {
        return maxTemporaryFileStoreSize;
    }

    /**
     * @param maxTemporaryFileStoreSize
     *            the maxTemporaryFileStoreSize to set
     */
    @Override
    public void setMaxTemporaryFileStoreSize(long maxTemporaryFileStoreSize) {
        this.maxTemporaryFileStoreSize = maxTemporaryFileStoreSize;
    }

    /**
     * @return the unlimitedTemporaryStoreSize
     */
    @Override
    public boolean isUnlimitedTemporaryStoreSize() {
        return unlimitedTemporaryStoreSize;
    }

    /**
     * @param unlimitedTemporaryStoreSize
     *            the unlimitedTemporaryStoreSize to set
     */
    @Override
    public void setUnlimitedTemporaryStoreSize(
                                               boolean unlimitedTemporaryStoreSize) {
        this.unlimitedTemporaryStoreSize = false;
    }

    /**
     * @return the unlimitedPermanentStoreSize
     */
    @Override
    public boolean isUnlimitedPermanentStoreSize() {
        return unlimitedPermanentStoreSize;
    }

    /**
     * @param unlimitedPermanentStoreSize
     *            the unlimitedPermanentStoreSize to set
     */
    @Override
    public void setUnlimitedPermanentStoreSize(
                                               boolean unlimitedPermanentStoreSize) {
        this.unlimitedPermanentStoreSize = false;
    }

    /** {@inheritDoc} */
    @Override
    public void validateFileStoreSettings() throws InvalidFileStoreConfigurationException {
        if (logFileSize >= maxPermanentFileStoreSize) {
            // We do not want to fail if the logFileSize is more than or equel maxPermanentFileStoreSize,
            // we reset the log to 1 MB less than the maxPermanentFileStoreSize and write a ffdc and a warning message 
            // informing the issue and gracefully continue. 
            String errorMsg = nls.getFormattedMessage("INVALID_FILE_SIZE_SIAS0124", new Object[] { logFileSize / (1024 * 1024), maxPermanentFileStoreSize / (1024 * 1024) }, null);
            SibTr.warning(tc, errorMsg);

            logFileSize = maxPermanentFileStoreSize - 1024 * 1024;

        }

        // Since the minimum is always the default value 
        // if the log file size is more than the minimum values, set to logFileSize 
        if (logFileSize > minPermanentFileStoreSize) {
            minPermanentFileStoreSize = logFileSize;
            minTemporaryFileStoreSize = logFileSize;
        }

    }
}
