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

package com.ibm.ws.jpa.fvt.inheritance.entities.singletable.xml;

import com.ibm.ws.jpa.fvt.inheritance.entities.ITreeLeaf3;

public class XMLSTIDTreeLeaf3Entity extends XMLSTIDTreeMSC implements ITreeLeaf3 {
    public XMLSTIDTreeLeaf3Entity() {
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
        return "XMLSTIDTreeLeaf3Entity [stringVal2=" + stringVal2 + ", getStringVal1()=" + getStringVal1()
               + ", getId()=" + getId() + ", getName()=" + getName() + "]";
    }
}
