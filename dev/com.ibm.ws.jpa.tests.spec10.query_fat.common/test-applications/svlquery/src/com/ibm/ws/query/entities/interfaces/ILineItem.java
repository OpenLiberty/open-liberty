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

public interface ILineItem {
    public double getCost();

    public void setCost(double cost);

    public int getLid();

    public void setLid(int lid);

    public IProduct getProduct();

    public void setProduct(IProduct product);

    public int getQuantity();

    public void setQuantity(int quantity);

    public IOrderBean getOrder();

    public void setOrder(IOrderBean order);

}
