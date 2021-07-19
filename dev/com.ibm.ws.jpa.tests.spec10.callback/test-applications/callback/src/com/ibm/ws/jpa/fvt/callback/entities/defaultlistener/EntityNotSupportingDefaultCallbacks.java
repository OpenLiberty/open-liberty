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

package com.ibm.ws.jpa.fvt.callback.entities.defaultlistener;

import javax.persistence.Entity;
import javax.persistence.ExcludeDefaultListeners;
import javax.persistence.Id;
import javax.persistence.Table;

import com.ibm.ws.jpa.fvt.callback.entities.ICallbackEntity;

@Entity
@Table(name = "CallbkEntNSptDefCbk")
@ExcludeDefaultListeners
public class EntityNotSupportingDefaultCallbacks implements ICallbackEntity {
    @Id
    private int id;

    private String name;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "EntityNotSupportingDefaultCallbacks [id=" + id + ", name=" + name + "]";
    }
}
