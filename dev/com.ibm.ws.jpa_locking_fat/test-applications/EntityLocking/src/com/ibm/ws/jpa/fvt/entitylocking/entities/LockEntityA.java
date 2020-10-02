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

package com.ibm.ws.jpa.fvt.entitylocking.entities;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "EL10LockEntA")
public class LockEntityA {
    @Id
    @Column(name = "ID")
    private int id;

    @Basic
    @Column(name = "STRDATA")
    private String strData;

    @Version
    @Column(name = "VERSION")
    private int version;

    @OneToOne
    @JoinColumn(name = "ENTB_OTO")
    private LockEntityB lockEntBOneToOne;

    @ManyToOne
    @JoinColumn(name = "ENTB_MTO")
    private LockEntityB lockEntBManyToOne;

    @OneToMany
    @JoinTable(name = "EL10LEOMJT", joinColumns = { @JoinColumn(name = "ENTA") }, inverseJoinColumns = { @JoinColumn(name = "ENTB") })
    private Collection<LockEntityB> lockEntBOneToManyJT;

    @ManyToMany
    @JoinTable(name = "EL10LEMMJT", joinColumns = { @JoinColumn(name = "ENTA") }, inverseJoinColumns = { @JoinColumn(name = "ENTB") })
    private Collection<LockEntityB> lockEntBManyToMany;

    public LockEntityA() {
        lockEntBOneToManyJT = new ArrayList<LockEntityB>();
        lockEntBManyToMany = new ArrayList<LockEntityB>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStrData() {
        return strData;
    }

    public void setStrData(String strData) {
        this.strData = strData;
    }

    public LockEntityB getLockEntBOneToOne() {
        return lockEntBOneToOne;
    }

    public void setLockEntBOneToOne(LockEntityB lockEntBOneToOne) {
        this.lockEntBOneToOne = lockEntBOneToOne;
    }

    public LockEntityB getLockEntBManyToOne() {
        return lockEntBManyToOne;
    }

    public void setLockEntBManyToOne(LockEntityB lockEntBManyToOne) {
        this.lockEntBManyToOne = lockEntBManyToOne;
    }

    public Collection<LockEntityB> getLockEntBOneToManyJT() {
        return lockEntBOneToManyJT;
    }

    public void setLockEntBOneToManyJT(Collection<LockEntityB> lockEntBOneToManyJT) {
        this.lockEntBOneToManyJT = lockEntBOneToManyJT;
    }

    public Collection<LockEntityB> getLockEntBManyToMany() {
        return lockEntBManyToMany;
    }

    public void setLockEntBManyToMany(Collection<LockEntityB> lockEntBManyToMany) {
        this.lockEntBManyToMany = lockEntBManyToMany;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "LockEntityA [id=" + id + ", strData=" + strData + ", version="
               + version + ", lockEntBOneToOne=" + lockEntBOneToOne
               + ", lockEntBManyToOne=" + lockEntBManyToOne
               + ", lockEntBOneToManyJT=" + lockEntBOneToManyJT
               + ", lockEntBManyToMany=" + lockEntBManyToMany + "]";
    }
}
