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

package com.ibm.ws.query.entities.interfaces;

import java.util.Collection;

public interface ICustomerBean {
    public int getId();

    public void setId(int id);

    public String getName();

    public void setName(String name);

    public int getRating();

    public void setRating(int rating);

    public Collection<? extends IOrderBean> getOrders();

    public void setOrders(Collection<? extends IOrderBean> orders);

    @Override
    public String toString();

    public Collection<? extends IProduct> getSupplies();

    public void setSupplies(Collection<? extends IProduct> supplies);

}
