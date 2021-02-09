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

package com.ibm.ws.query.entities.ano;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import com.ibm.ws.query.entities.interfaces.IPartBase;
import com.ibm.ws.query.entities.interfaces.ISupplier;

@Entity
@Table(name = "JPASupplierPartTab")
public class Supplier implements ISupplier {

    @Id
    protected int sid;
    @Column(length = 40)
    protected String name;

    @ManyToMany
    Collection<PartBase> supplies = new ArrayList();

    public Supplier() {
    }

    public Supplier(int sid, String name) {
        this.sid = sid;
        this.name = name;
    }

    @Override
    public Supplier addPart(IPartBase p) {
        supplies.add((PartBase) p);
        ((Collection<Supplier>) p.getSuppliers()).add(this);
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int getSid() {
        return sid;
    }

    @Override
    public void setSid(int sid) {
        this.sid = sid;
    }

    @Override
    public Collection<PartBase> getSupplies() {
        return supplies;
    }

    @Override
    public void setSupplies(Collection<? extends IPartBase> supplies) {
        this.supplies = (Collection<PartBase>) supplies;
    }

    @Override
    public String toString() {

        return "Supplier:" + sid + " name:+" + name;
    }

}
