/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.wadl;

import java.util.ArrayList;
import java.util.List;

public class OrderInfoImpl implements OrderInfo {

    List<Order> list = new ArrayList<Order>();

    public OrderInfoImpl() {
        Order order = new Order();
        order.setOrderId(1);
        order.setItemName("Soap");
        order.setQuantity(120);
        order.setCustomerName("Sandeep");
        order.setShippingAddress("Gurgaon");
        list.add(0, order);
        order = new Order();
        order.setOrderId(2);
        order.setItemName("Shampoo");
        order.setQuantity(50);
        order.setCustomerName("Sandeep");
        order.setShippingAddress("Gurgaon");
        list.add(1, order);
    }

    @Override
    public Order getOrder(int orderId) {
        System.out.println("Inside the GetOrder...");
        if (list.get(0).getOrderId() == orderId) {
            return list.get(0);
        } else if (list.get(1).getOrderId() == orderId) {
            return list.get(1);
        } else {
            return null;
        }
    }

    @Override
    public OrderList getAllOrders() {
        OrderList details = new OrderList();
        for (Order order : list) {
            details.getOrder().add(order);
        }
        return details;
    }
}