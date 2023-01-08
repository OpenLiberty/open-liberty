/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities;

import java.util.ArrayList;
import java.util.Collection;

public class MFDRelationalEntA {
    private int id;
    private String name;

    private MFDRelationalEntityB oneXoneEntityB;

    private MFDRelationalEntityB manyXoneEntityB;

    private Collection oneXmanyEntityBCollection;

    private Collection manyXmanyEntityBCollection;

    public MFDRelationalEntA() {
        oneXmanyEntityBCollection = new ArrayList();
        manyXmanyEntityBCollection = new ArrayList();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Collection getManyXmanyEntityBCollection() {
        return manyXmanyEntityBCollection;
    }

    public void setManyXmanyEntityBCollection(Collection manyXmanyEntityBCollection) {
        this.manyXmanyEntityBCollection = manyXmanyEntityBCollection;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MFDRelationalEntityB getManyXoneEntityB() {
        return manyXoneEntityB;
    }

    public void setManyXOneEntityB(MFDRelationalEntityB manyXoneEntityB) {
        this.manyXoneEntityB = manyXoneEntityB;
    }

    public Collection getOneXmanyEntityBCollection() {
        return oneXmanyEntityBCollection;
    }

    public void setOneXmanyEntityBCollection(Collection oneXmanyEntityBCollection) {
        this.oneXmanyEntityBCollection = oneXmanyEntityBCollection;
    }

    public MFDRelationalEntityB getOneXoneEntityB() {
        return oneXoneEntityB;
    }

    public void setOneXoneEntityB(MFDRelationalEntityB oneXoneEntityB) {
        this.oneXoneEntityB = oneXoneEntityB;
    }

    @Override
    public String toString() {
        return "MFDRelationalEntA [id=" + id + ", name=" + name + ", oneXoneEntityB=" + oneXoneEntityB
               + ", manyXoneEntityB=" + manyXoneEntityB + ", oneXmanyEntityBCollection=" + oneXmanyEntityBCollection
               + ", manyXmanyEntityBCollection=" + manyXmanyEntityBCollection + "]";
    }

}
