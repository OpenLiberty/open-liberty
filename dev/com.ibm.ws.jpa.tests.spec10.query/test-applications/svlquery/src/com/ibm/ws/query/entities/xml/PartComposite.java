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

package com.ibm.ws.query.entities.xml;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.EntityManager;

import com.ibm.ws.query.entities.interfaces.IPart;
import com.ibm.ws.query.entities.interfaces.IPartComposite;
import com.ibm.ws.query.entities.interfaces.IUsage;

public class PartComposite extends Part implements IPartComposite {
    protected Collection<Usage> partsUsed = new ArrayList();

    protected double assemblyCost;
    protected double massIncrement;

    public PartComposite() {
    }

    public PartComposite(int partno, String name, double asmCost, double massInc) {
        this.partno = partno;
        this.name = name;
        assemblyCost = asmCost;
        massIncrement = massInc;
    }

    @Override
    public PartComposite addSubPart(EntityManager em, int quantity, IPart subpart) {
        Usage use = new Usage(this, quantity, (Part) subpart);
        em.persist(use);
        return this;
    }

    @Override
    public double getAssemblyCost() {
        return assemblyCost;
    }

    @Override
    public void setAssemblyCost(double assemblyCost) {
        this.assemblyCost = assemblyCost;
    }

    @Override
    public double getMassIncrement() {
        return massIncrement;
    }

    @Override
    public void setMassIncrement(double massIncrement) {
        this.massIncrement = massIncrement;
    }

    @Override
    public String toString() {

        return "PartComposite:" + partno + " name:+" + name + " assemblyCost:" + assemblyCost + " massIncrement:" + massIncrement;
    }

    @Override
    public Collection<Usage> getPartsUsed() {
        return partsUsed;
    }

    @Override
    public void setPartsUsed(Collection<? extends IUsage> partsUsed) {
        this.partsUsed = (Collection<Usage>) partsUsed;
    }

}
