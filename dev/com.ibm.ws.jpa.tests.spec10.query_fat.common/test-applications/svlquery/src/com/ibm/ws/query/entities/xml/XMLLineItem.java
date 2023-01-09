/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

package com.ibm.ws.query.entities.xml;

import com.ibm.ws.query.entities.interfaces.ILineItem;
import com.ibm.ws.query.entities.interfaces.IOrderBean;
import com.ibm.ws.query.entities.interfaces.IProduct;

public class XMLLineItem implements ILineItem {
    private int lid;
    private XMLProduct product;
    private int quantity;
    private double cost;
    private XMLOrderBean order;

    public XMLLineItem() {
    }

    public XMLLineItem(int key, XMLProduct p, int quantity, double cost, XMLOrderBean o) {
        lid = key;
        if (p != null)
            product = p;
        this.quantity = quantity;
        this.cost = cost;
        order = o;
    }

    @Override
    public double getCost() {
        return cost;
    }

    @Override
    public void setCost(double cost) {
        this.cost = cost;
    }

    @Override
    public int getLid() {
        return lid;
    }

    @Override
    public void setLid(int lid) {
        this.lid = lid;
    }

    @Override
    public XMLProduct getProduct() {
        return product;
    }

    @Override
    public void setProduct(IProduct product) {
        this.product = (XMLProduct) product;
    }

    @Override
    public int getQuantity() {
        return quantity;
    }

    @Override
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public XMLOrderBean getOrder() {
        return order;
    }

    @Override
    public void setOrder(IOrderBean order) {
        this.order = (XMLOrderBean) order;
    }

}
