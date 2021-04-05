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

package com.ibm.ws.jpa.fvt.callback.entities.orderofinvocation;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.Table;

import com.ibm.ws.jpa.fvt.callback.AbstractCallbackListener;
import com.ibm.ws.jpa.fvt.callback.entities.ICallbackEntity;

@Entity
@Table(name = "OOIRootEntity")
@Inheritance(strategy = javax.persistence.InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "leafType")
public abstract class OrderOfInvocationRootEntity extends AbstractCallbackListener implements ICallbackEntity {
    @Id
    private int id;

    private String name;

    public OrderOfInvocationRootEntity() {
        super();
    }

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
        return "OOIRootPkgEntity [id=" + id + ", name=" + name + "]";
    }
}
