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

import com.ibm.ws.jpa.fvt.inheritance.entities.ITreeLeaf2;

@Entity
@Table(name = "AnoJTIDLeaf2")
@DiscriminatorValue("2")
public class AnoJTIDTreeLeaf2Entity extends AnoJTIDTreeRootEntity implements ITreeLeaf2 {
    public AnoJTIDTreeLeaf2Entity() {
        super();
    }

    private float floatVal;

    @Override
    public float getFloatVal() {
        return floatVal;
    }

    @Override
    public void setFloatVal(float floatVal) {
        this.floatVal = floatVal;
    }

    @Override
    public String toString() {
        return "AnoJTIDTreeLeaf2Entity [floatVal=" + floatVal + ", getId()=" + getId() + ", getName()=" + getName()
               + "]";
    }
}
