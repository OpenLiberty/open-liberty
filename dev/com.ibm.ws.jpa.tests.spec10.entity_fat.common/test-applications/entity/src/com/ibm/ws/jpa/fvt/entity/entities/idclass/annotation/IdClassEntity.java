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

package com.ibm.ws.jpa.fvt.entity.entities.idclass.annotation;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

import com.ibm.ws.jpa.fvt.entity.entities.IIdClassEntity;
import com.ibm.ws.jpa.fvt.entity.support.PKey;

/**
 * Entity demonstrating the use of compound primary key functionality using an IdClass.
 *
 * @author Jody Grassel (jgrassel@us.ibm.com)
 *
 */
@Entity
@IdClass(PKey.class)
public class IdClassEntity implements IIdClassEntity {
    @Id
    private int id;
    @Id
    private String country;

    private int intVal;

    public IdClassEntity() {

    }

    public IdClassEntity(int id, String country) {
        this.id = id;
        this.country = country;
    }

    @Override
    public String getCountry() {
        return country;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setCountry(String country) {
        this.country = country;
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
    public String toString() {
        return "IdClassEntity [id=" + id + ", country=" + country + "]";
    }
}
