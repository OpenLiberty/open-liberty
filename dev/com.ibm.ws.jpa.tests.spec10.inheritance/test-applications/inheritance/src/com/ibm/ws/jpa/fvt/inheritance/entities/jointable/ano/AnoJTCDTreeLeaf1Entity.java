/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.inheritance.entities.jointable.ano;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.ibm.ws.jpa.fvt.inheritance.entities.ITreeLeaf1;

@Entity
@Table(name = "AnoJTCDLeaf1")
@DiscriminatorValue("B")
public class AnoJTCDTreeLeaf1Entity extends AnoJTCDTreeRootEntity implements ITreeLeaf1 {
    private int intVal;

    public AnoJTCDTreeLeaf1Entity() {
        super();
    }

    @Override
    public int getIntVal() {
        return intVal;
    }

    @Override
    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    @Override
    public String toString() {
        return "AnoJTCDTreeLeaf1Entity [intVal=" + intVal + ", getId()=" + getId() + ", getName()=" + getName()
               + "]";
    }
}
