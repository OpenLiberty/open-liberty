/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.cache.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Version;

@Entity
public class JPA20EMEntityA {
    @Id
    private int id;

    private String strData;

    @OneToOne(mappedBy = "entityA")
    private JPA20EMEntityC entityC;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "entityALazy")
    private JPA20EMEntityC entityCLazy;

    @ManyToMany
    private List<JPA20EMEntityB> entityBList;

    @Version
    private long version;

    public JPA20EMEntityA() {
        entityBList = new ArrayList<JPA20EMEntityB>();
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

    public JPA20EMEntityC getEntityC() {
        return entityC;
    }

    public void setEntityC(JPA20EMEntityC entityC) {
        this.entityC = entityC;
    }

    public List<JPA20EMEntityB> getEntityBList() {
        return entityBList;
    }

    public JPA20EMEntityC getEntityCLazy() {
        return entityCLazy;
    }

    public void setEntityCLazy(JPA20EMEntityC entityCLazy) {
        this.entityCLazy = entityCLazy;
    }

    public long getVersion() {
        return version;
    }

}
