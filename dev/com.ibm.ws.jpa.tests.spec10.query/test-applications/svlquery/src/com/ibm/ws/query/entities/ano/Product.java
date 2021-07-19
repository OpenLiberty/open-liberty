/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.query.entities.ano;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.ibm.ws.query.entities.interfaces.ICustomerBean;
import com.ibm.ws.query.entities.interfaces.ILineItem;
import com.ibm.ws.query.entities.interfaces.IProduct;

@Entity
@Table(name = "JPAProductPartTab")
public class Product implements IProduct {
    @Id
    private int pid;
    @Column(length = 40)
    private String description;
    private int inventory;
    private int backorder;
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "product")
    private java.util.Collection<LineItem> lineitems;
    @ManyToOne(fetch = FetchType.EAGER)
    private CustomerBean supplier;

    public Product() {
    }

    public Product(int key, String desc, int inv) {
        pid = key;
        description = desc;
        inventory = inv;
        backorder = 0;
    }

    @Override
    public int getBackorder() {
        return backorder;
    }

    @Override
    public void setBackorder(int backorder) {
        this.backorder = backorder;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public int getInventory() {
        return inventory;
    }

    @Override
    public void setInventory(int inventory) {
        this.inventory = inventory;
    }

    @Override
    public java.util.Collection<LineItem> getLineitems() {
        return lineitems;
    }

    @Override
    public void setLineitems(java.util.Collection<? extends ILineItem> lineitems) {
        this.lineitems = (java.util.Collection<LineItem>) lineitems;
    }

    @Override
    public int getPid() {
        return pid;
    }

    @Override
    public void setPid(int pid) {
        this.pid = pid;
    }

    @Override
    public CustomerBean getSupplier() {
        return supplier;
    }

    @Override
    public void setSupplier(ICustomerBean supplier) {
        this.supplier = (CustomerBean) supplier;
    }

    @Override
    public String toString() {
        return "Product:" + pid + " description:" + description + " inventory:" + inventory + " backorder:" + backorder;
    }

}
