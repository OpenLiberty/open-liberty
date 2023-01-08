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

import java.util.ArrayList;
import java.util.Collection;

import com.ibm.ws.query.entities.interfaces.ICustomerBean;
import com.ibm.ws.query.entities.interfaces.ILineItem;
import com.ibm.ws.query.entities.interfaces.IOrderBean;

public class XMLOrderBean implements IOrderBean {
    private int oid;
    private double amount;
    private boolean delivered;
    private XMLCustomerBean customer;
    private Collection<XMLLineItem> lineitems = new ArrayList();

    public XMLOrderBean() {
    }

    public XMLOrderBean(int oid, double amt, boolean delivered, XMLCustomerBean c) {
        this.oid = oid;
        amount = amt;
        this.delivered = delivered;
        customer = c;
        if (c != null)
            c.getOrders().add(this);
    }

    @Override
    public double getAmount() {
        return amount;
    }

    @Override
    public void setAmount(double amount) {
        this.amount = amount;
    }

    @Override
    public XMLCustomerBean getCustomer() {
        return customer;
    }

    @Override
    public void setCustomer(ICustomerBean customer) {
        this.customer = (XMLCustomerBean) customer;
    }

    @Override
    public boolean isDelivered() {
        return delivered;
    }

    @Override
    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    @Override
    public int getOid() {
        return oid;
    }

    @Override
    public void setOid(int oid) {
        this.oid = oid;
    }

    @Override
    public String toString() {
        return "Order:" + oid + " amount:" + amount + " delivered:" + delivered + " customer:" +
               (customer != null ? customer.getId() : -1);
    }

    @Override
    public Collection<XMLLineItem> getLineitems() {
        return lineitems;
    }

    @Override
    public void setLineitems(Collection<? extends ILineItem> lineitems) {
        this.lineitems = (Collection<XMLLineItem>) lineitems;
    }

}
