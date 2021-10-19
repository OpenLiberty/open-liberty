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

package com.ibm.ws.query.entities.xml;

import com.ibm.ws.query.entities.interfaces.ILineItem;
import com.ibm.ws.query.entities.interfaces.IOrderBean;
import com.ibm.ws.query.entities.interfaces.IProduct;

public class LineItem implements ILineItem {
    private int lid;
    private Product product;
    private int quantity;
    private double cost;
    private OrderBean order;

    public LineItem() {
    }

    public LineItem(int key, Product p, int quantity, double cost, OrderBean o) {
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
    public Product getProduct() {
        return product;
    }

    @Override
    public void setProduct(IProduct product) {
        this.product = (Product) product;
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
    public OrderBean getOrder() {
        return order;
    }

    @Override
    public void setOrder(IOrderBean order) {
        this.order = (OrderBean) order;
    }

}
