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

package com.ibm.ws.jpa.fvt.inheritance.entities.jointable.ano;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.ibm.ws.jpa.fvt.inheritance.entities.ITreeLeaf3;

@Entity
@Table(name = "AnoJTSDLeaf3")
@DiscriminatorValue("AnoJTSDLeaf3")
public class AnoJTSDTreeLeaf3Entity extends AnoJTSDTreeMSC implements ITreeLeaf3 {
    public AnoJTSDTreeLeaf3Entity() {
        super();
    }

    private String stringVal2;

    @Override
    public String getStringVal2() {
        return stringVal2;
    }

    @Override
    public void setStringVal2(String stringVal2) {
        this.stringVal2 = stringVal2;
    }

    @Override
    public String toString() {
        return "AnoJTSDTreeLeaf3Entity [stringVal2=" + stringVal2 + ", getStringVal1()=" + getStringVal1()
               + ", getId()=" + getId() + ", getName()=" + getName() + "]";
    }
}
