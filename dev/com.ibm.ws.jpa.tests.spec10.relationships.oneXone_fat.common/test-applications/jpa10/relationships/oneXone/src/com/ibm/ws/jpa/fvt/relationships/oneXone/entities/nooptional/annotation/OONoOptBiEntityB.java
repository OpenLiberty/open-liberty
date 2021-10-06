/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.oneXone.entities.nooptional.annotation;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.INoOptEntityB;

@Entity
public class OONoOptBiEntityB implements INoOptEntityB {
    @Id
    private int id;
    private String name;

    @OneToOne(mappedBy = "b")
    private OONoOptBiEntityA a;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public OONoOptBiEntityA getA() {
        return a;
    }

    public void setA(OONoOptBiEntityA a) {
        this.a = a;
    }

    @Override
    public String toString() {
        return "OONoOptBiEntityB [id=" + id + ", name=" + name + "]";
    }

}
