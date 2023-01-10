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

package com.ibm.ws.jpa.fvt.entity.entities.readonly.annotation;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import com.ibm.ws.jpa.fvt.entity.entities.IReadOnlyEntity;

@Entity
public class ReadOnlyEntity implements IReadOnlyEntity {
    @Id
    private int id;

    private int intVal;

    @Column(insertable = false)
    private int noInsertIntVal;

    @Column(updatable = false)
    private int noUpdatableIntVal;

    @Column(insertable = false, updatable = false)
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
        return "ReadOnlyEntity [id=" + id + ", intVal=" + intVal + ", noInsertIntVal=" + noInsertIntVal
               + ", noUpdatableIntVal=" + noUpdatableIntVal + ", readOnlyIntVal=" + readOnlyIntVal + "]";
    }

}
