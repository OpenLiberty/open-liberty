/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
import com.ibm.wsspi.concurrent.persistent.PartitionRecord;

/**
 * JPA entity for a partition entry in the persistent store.
 */
@Entity
@Trivial
public class Partition {
    /**
     * Generated partition id.
     */
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    public long ID;

    /**
     * Id, JNDI name, or config.displayId of persistent executor instance.
     */
    @Column(length = 100, nullable = false)
    public String EXECUTOR;

    /**
     * Host name.
     */
    @Column(length = 100, nullable = false)
    public String HOSTNAME;

    /**
     * Liberty server name.
     */
    @Column(length = 100, nullable = false)
    public String LSERVER;

    /**
     * Value of ${wlp.user.dir}
     */
    @Column(length = 254, nullable = false)
    public String USERDIR;

    /**
     * Currently unused. Reserved for possible future use.
     */
    @Column(nullable = false)
    public long EXPIRY = Long.MAX_VALUE;

    /**
     * Currently unused. Reserved for possible future use.
     */
    @Column(nullable = false)
    public long STATES = 0l;

    public Partition() {}

    Partition(PartitionRecord record) {
        if (record.hasExecutor())
            EXECUTOR = record.getExecutor();
        if (record.hasId())
            ID = record.getId();
        if (record.hasHostName())
            HOSTNAME = record.getHostName();
        if (record.hasLibertyServer())
            LSERVER = record.getLibertyServer();
        if (record.hasUserDir())
            USERDIR = record.getUserDir();
    }
}
