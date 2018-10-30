/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.recoverylog.spi;

/**
 *
 */
public class LeaseInfo {
    private String leaseFile;
    private String logDir;
    private boolean _canDeleteLease;
    private final String _recoveryIdentity;

    /**
     * @param peerRecoveryIdentity
     */
    public LeaseInfo(String recoveryIdentity) {
        _recoveryIdentity = recoveryIdentity;
    }

    @Override
    public String toString() {

        return "Lease file: " + leaseFile + ", Log dir: " + logDir;
    }

    /**
     * @return the logDir
     */
    public String getLogDir() {
        return logDir;
    }

    /**
     * @param logDir the logDir to set
     */
    public void setLogDir(String logDir) {
        this.logDir = logDir;
    }

    /**
     * @return the leaseFile
     */
    public String getLeaseFile() {
        return leaseFile;
    }

    /**
     * @param leaseFile the leaseFile to set
     */
    public void setLeaseFile(String leaseFile) {
        this.leaseFile = leaseFile;
    }

    /**
     *
     */
    public void setCanDeleteLease() {
        _canDeleteLease = true;
    }

    /**
     * @return
     */
    public boolean isCanDeleteLeaseFile() {
        return _canDeleteLease;
    }
}