/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.query.entities.xml;

import java.util.ArrayList;
import java.util.Collection;

import com.ibm.ws.query.entities.interfaces.IPartBase;
import com.ibm.ws.query.entities.interfaces.ISupplier;

public class XMLPartBase extends XMLPart implements IPartBase {
    protected double cost;
    protected double mass;

    protected Collection<XMLSupplier> suppliers = new ArrayList();

    public XMLPartBase() {
    }

    public XMLPartBase(int partno, String name, double cost, double mass) {
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
    public Collection<XMLSupplier> getSuppliers() {
        return suppliers;
    }

    @Override
    public void setSuppliers(Collection<? extends ISupplier> suppliers) {
        this.suppliers = (Collection<XMLSupplier>) suppliers;
    }

    @Override
    public String toString() {
        String sup = "";
        if (getSuppliers() != null)
            for (XMLSupplier s : getSuppliers()) {
                sup = sup + s.sid + ",";
            }
        return "PartBase:" + partno + " name:+" + name + " cost:" + cost + " mass:" + mass + " supplies=[" + sup + "]";
    }

}
