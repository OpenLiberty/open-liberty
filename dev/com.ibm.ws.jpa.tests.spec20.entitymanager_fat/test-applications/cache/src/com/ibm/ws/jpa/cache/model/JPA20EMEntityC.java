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

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Version;

@Entity
public class JPA20EMEntityC {
    @Id
    private int id;

    private String strData;

    @OneToOne
    private JPA20EMEntityA entityA;

    @OneToOne(fetch = FetchType.LAZY)
    private JPA20EMEntityA entityALazy;

    @Version
    private long version;

    public JPA20EMEntityC() {

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

    public JPA20EMEntityA getEntityA() {
        return entityA;
    }

    public void setEntityA(JPA20EMEntityA entityA) {
        this.entityA = entityA;
    }

    public JPA20EMEntityA getEntityALazy() {
        return entityALazy;
    }

    public void setEntityALazy(JPA20EMEntityA entityALazy) {
        this.entityALazy = entityALazy;
    }

    public long getVersion() {
        return version;
    }

}
