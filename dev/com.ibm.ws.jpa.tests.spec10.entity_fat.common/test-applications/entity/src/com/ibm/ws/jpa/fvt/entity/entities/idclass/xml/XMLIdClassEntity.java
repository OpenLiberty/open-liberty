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

package com.ibm.ws.jpa.fvt.entity.entities.idclass.xml;

import com.ibm.ws.jpa.fvt.entity.entities.IIdClassEntity;

/**
 * Entity demonstrating the use of compound primary key functionality using an IdClass.
 *
 * @author Jody Grassel (jgrassel@us.ibm.com)
 *
 */
public class XMLIdClassEntity implements IIdClassEntity {
    private int id;
    private String country;

    private int intVal;

    public XMLIdClassEntity() {

    }

    public XMLIdClassEntity(int id, String country) {
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
}
