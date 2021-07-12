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

import java.util.ArrayList;
import java.util.Collection;

import com.ibm.ws.query.entities.interfaces.ICustomerBean;
import com.ibm.ws.query.entities.interfaces.IOrderBean;
import com.ibm.ws.query.entities.interfaces.IProduct;

public class CustomerBean implements ICustomerBean {
    private int id;
    private String name;
    private int rating;

    private Collection<OrderBean> orders = new ArrayList();

    private Collection<Product> supplies = new ArrayList();

    public CustomerBean() {
    }

    public CustomerBean(int id, String name, int rating) {
        this.id = id;
        this.name = name;
        this.rating = rating;
    }

    @Override
    public int getId() {
        System.err.println("Customer.getId called");
        return id;
    }

    @Override
    public void setId(int id) {
        System.err.println("Customer.setId called");
        this.id = id;
    }

    @Override
    public String getName() {
        System.err.println("Customer.getName called");
        return name;
    }

    @Override
    public void setName(String name) {
        System.err.println("Customer.setName called");
        this.name = name;
    }

    @Override
    public int getRating() {
        System.err.println("Customer.getRating called");
        return rating;
    }

    @Override
    public void setRating(int rating) {
        System.err.println("Customer.setRating called");
        this.rating = rating;
    }

    @Override
    public Collection<OrderBean> getOrders() {
        System.err.println("Customer.getOrders called");
        return orders;
    }

    @Override
    public void setOrders(Collection<? extends IOrderBean> orders) {
        System.err.println("Customer.setOrders called");
        this.orders = (Collection<OrderBean>) orders;
    }

    @Override
    public String toString() {
        int noOrders = 0;
        if (getOrders() != null)
            noOrders = getOrders().size();
        return "Customer:" + id + " name:" + name + " rating:" + rating + " orders:" + noOrders;
    }

    @Override
    public Collection<Product> getSupplies() {
        System.err.println("Customer.getSupplies called");
        return supplies;
    }

    @Override
    public void setSupplies(Collection<? extends IProduct> supplies) {
        System.err.println("Customer.setSupplies called");
        this.supplies = (Collection<Product>) supplies;
    }

}
