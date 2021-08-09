/*******************************************************************************
 * Copyright (c) 2014,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.concurrent.persistent.TaskRecord;

/**
 * JPA entity for a task entry in the persistent store.
 */
@Entity
@Trivial
public class Task {
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    public Long ID;

    @Column(length = 254, nullable = false)
    public String INAME;

    @Column(length = 254, nullable = false)
    public String LOADER;

    @Column(nullable = false)
    public short MBITS;

    @Column(nullable = false)
    public long NEXTEXEC;

    @Column(nullable = false)
    public long ORIGSUBMT;

    @Column(length = 120, nullable = false)
    public String OWNR;

    @Column(nullable = false)
    public long PARTN;

    @Column
    public Long PREVSCHED;

    @Column
    public Long PREVSTART;

    @Column
    public Long PREVSTOP;

    @Column
    public byte[] RESLT;

    @Column(nullable = false)
    public short RFAILS;

    @Column(nullable = false)
    public short STATES;

    @Column
    public byte[] TASKB;

    @Column(nullable = false)
    public byte[] TASKINFO;

    @Column
    public byte[] TRIG;

    @Column(nullable = false)
    public int TXTIMEOUT;

    @Column(nullable = false)
    public int VERSION;

    public Task() {
    }

    Task(TaskRecord taskRecord) {
        if (taskRecord.hasId())
            ID = taskRecord.getId();

        if (taskRecord.hasIdentifierOfClassLoader())
            LOADER = taskRecord.getIdentifierOfClassLoader();

        if (taskRecord.hasIdentifierOfOwner())
            OWNR = taskRecord.getIdentifierOfOwner();

        if (taskRecord.hasClaimExpiryOrPartition())
            PARTN = taskRecord.getClaimExpiryOrPartition();

        if (taskRecord.hasMiscBinaryFlags())
            MBITS = taskRecord.getMiscBinaryFlags();

        if (taskRecord.hasName())
            INAME = taskRecord.getName();

        if (taskRecord.hasNextExecutionTime())
            NEXTEXEC = taskRecord.getNextExecutionTime();

        if (taskRecord.hasOriginalSubmitTime())
            ORIGSUBMT = taskRecord.getOriginalSubmitTime();

        if (taskRecord.hasPreviousScheduledStartTime())
            PREVSCHED = taskRecord.getPreviousScheduledStartTime();

        if (taskRecord.hasPreviousStartTime())
            PREVSTART = taskRecord.getPreviousStartTime();

        if (taskRecord.hasPreviousStopTime())
            PREVSTOP = taskRecord.getPreviousStopTime();

        if (taskRecord.hasResult())
            RESLT = taskRecord.getResult();

        if (taskRecord.hasConsecutiveFailureCount())
            RFAILS = taskRecord.getConsecutiveFailureCount();

        if (taskRecord.hasState())
            STATES = taskRecord.getState();

        if (taskRecord.hasTask())
            TASKB = taskRecord.getTask();

        if (taskRecord.hasTaskInformation())
            TASKINFO = taskRecord.getTaskInformation();

        if (taskRecord.hasTrigger())
            TRIG = taskRecord.getTrigger();

        if (taskRecord.hasTransactionTimeout())
            TXTIMEOUT = taskRecord.getTransactionTimeout();

        if (taskRecord.hasVersion())
            VERSION = taskRecord.getVersion();
    }
}
