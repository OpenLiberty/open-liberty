/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.entity.entities.readonly.xml;

import com.ibm.ws.jpa.fvt.entity.entities.IReadOnlyEntity;

public class XMLReadOnlyEntity implements IReadOnlyEntity {
    private int id;

    private int intVal;

    private int noInsertIntVal;

    private int noUpdatableIntVal;

    private int readOnlyIntVal;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
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
    public int getNoInsertIntVal() {
        return noInsertIntVal;
    }

    @Override
    public void setNoInsertIntVal(int noInsertIntVal) {
        this.noInsertIntVal = noInsertIntVal;
    }

    @Override
    public int getNoUpdatableIntVal() {
        return noUpdatableIntVal;
    }

    @Override
    public void setNoUpdatableIntVal(int noUpdatableIntVal) {
        this.noUpdatableIntVal = noUpdatableIntVal;
    }

    @Override
    public int getReadOnlyIntVal() {
        return readOnlyIntVal;
    }

    @Override
    public void setReadOnlyIntVal(int readOnlyIntVal) {
        this.readOnlyIntVal = readOnlyIntVal;
    }

    @Override
    public String toString() {
        return "XMLReadOnlyEntity [id=" + id + ", intVal=" + intVal + ", noInsertIntVal=" + noInsertIntVal
               + ", noUpdatableIntVal=" + noUpdatableIntVal + ", readOnlyIntVal=" + readOnlyIntVal + "]";
    }

}
