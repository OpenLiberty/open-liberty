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

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToMany;

import com.ibm.ws.query.entities.interfaces.IPartBase;
import com.ibm.ws.query.entities.interfaces.ISupplier;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "PARTTYPE")
@DiscriminatorValue(value = "PartBase")
public class PartBase extends Part implements IPartBase {

    protected double cost;
    protected double mass;

    @ManyToMany(mappedBy = "supplies")
    protected Collection<Supplier> suppliers = new ArrayList();

    public PartBase() {
    }

    public PartBase(int partno, String name, double cost, double mass) {
        this.partno = partno;
        this.name = name;
        this.cost = cost;
        this.mass = mass;
    }

    @Override
    public int getPartno() {
        return partno;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getCost() {
        return cost;
    }

    @Override
    public void setCost(double cost) {
        this.cost = cost;
    }

    @Override
    public double getMass() {
        return mass;
    }

    @Override
    public void setMass(double mass) {
        this.mass = mass;
    }

    @Override
    public Collection<Supplier> getSuppliers() {
        return suppliers;
    }

    @Override
    public void setSuppliers(Collection<? extends ISupplier> suppliers) {
        this.suppliers = (Collection<Supplier>) suppliers;
    }

    @Override
    public String toString() {
        String sup = "";
        if (getSuppliers() != null)
            for (Supplier s : getSuppliers()) {
                sup = sup + s.sid + ",";
            }
        return "PartBase:" + partno + " name:+" + name + " cost:" + cost + " mass:" + mass + " supplies=[" + sup + "]";
    }

}
