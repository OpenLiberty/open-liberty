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

package com.ibm.ws.jpa.fvt.inheritance.entities.concretetable.xml;

import com.ibm.ws.jpa.fvt.inheritance.entities.ITreeMSC;

public class XMLConcreteTreeMSC extends XMLConcreteTreeRootEntity implements ITreeMSC {
    public XMLConcreteTreeMSC() {
        super();
    }

    private String stringVal1;

    @Override
    public String getStringVal1() {
        return stringVal1;
    }

    @Override
    public void setStringVal1(String stringVal1) {
        this.stringVal1 = stringVal1;
    }

    @Override
    public String toString() {
        return "XMLConcreteTreeMSC [stringVal1=" + stringVal1 + ", getId()=" + getId() + ", getName()=" + getName()
               + "]";
    }
}
