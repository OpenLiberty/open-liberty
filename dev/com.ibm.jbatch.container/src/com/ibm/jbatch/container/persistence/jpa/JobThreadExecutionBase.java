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
package com.ibm.jbatch.container.persistence.jpa;

import java.util.Date;

import javax.batch.runtime.BatchStatus;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;

/**
 * @author skurz
 */
@MappedSuperclass
public class JobThreadExecutionBase implements EntityConstants {

    @Column(name = "BATCHSTATUS", nullable = false)
    private BatchStatus batchStatus;

    @Column(name = "EXITSTATUS", length = MAX_EXIT_STATUS_LENGTH)
    private String exitStatus;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column(name = "CREATETIME", nullable = false)
    private Date createTime;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column(name = "STARTTIME")
    private Date startTime;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column(name = "ENDTIME")
    private Date endTime;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column(name = "UPDATETIME")
    private Date lastUpdatedTime;

    @Column(name = "SERVERID", length = 256)
    private String serverId;

    @Column(name = "LOGPATH", nullable = true, length = 512)
    private String logpath;

    @Column(name = "RESTURL", length = 512)
    private String restUrl;

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Date getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    public void setLastUpdatedTime(Date updateTime) {
        this.lastUpdatedTime = updateTime;
    }

    public BatchStatus getBatchStatus() {
        return batchStatus;
    }

    public void setBatchStatus(BatchStatus batchStatus) {
        this.batchStatus = batchStatus;
    }

    public String getExitStatus() {
        return exitStatus;
    }

    public void setExitStatus(String exitStatus) {
        this.exitStatus = exitStatus;
    }

    public String getServerId() {
        return (serverId == null ? "" : serverId);
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getLogpath() {
        return logpath;
    }

    public void setLogpath(String logpath) {
        this.logpath = logpath;
    }

    public String getRestUrl() {
        return (restUrl == null ? "" : restUrl);
    }

    public void setRestUrl(String restUrl) {
        this.restUrl = restUrl;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("From JobThreadExecutionBase: ");
        buf.append("batchStatus = " + batchStatus);
        buf.append(", exitStatus = " + exitStatus);
        buf.append(", restUrl = " + restUrl);
        buf.append(", logpath = " + logpath);
        buf.append(", serverId = " + serverId);
        return buf.toString();
    }

}
