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
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Version;

@Entity
public class JPA20EMEntityB implements java.io.Serializable {
    @Id
    private int id;

    private String strData;

    @ManyToMany(mappedBy = "entityBList")
    private List<JPA20EMEntityA> entityAList;

    @Version
    private long version;

    public JPA20EMEntityB() {
        entityAList = new ArrayList<JPA20EMEntityA>();
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

    public List<JPA20EMEntityA> getEntityAList() {
        return entityAList;
    }

    public long getVersion() {
        return version;
    }

}
