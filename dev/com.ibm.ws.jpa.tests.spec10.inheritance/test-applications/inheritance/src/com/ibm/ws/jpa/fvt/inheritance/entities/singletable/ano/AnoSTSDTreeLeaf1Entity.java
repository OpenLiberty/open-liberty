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

package com.ibm.ws.jpa.fvt.inheritance.entities.singletable.ano;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.ibm.ws.jpa.fvt.inheritance.entities.ITreeLeaf1;

@Entity
@DiscriminatorValue("AnoSTSDLeaf1")
public class AnoSTSDTreeLeaf1Entity extends AnoSTSDTreeRootEntity implements ITreeLeaf1 {
    private int intVal;

    public AnoSTSDTreeLeaf1Entity() {
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
        return "AnoSTSDTreeLeaf1Entity [intVal=" + intVal + ", getId()=" + getId() + ", getName()=" + getName()
               + "]";
    }
}
