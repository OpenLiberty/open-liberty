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

package com.ibm.ws.query.entities.interfaces;

public interface IProduct {
    public int getBackorder();

    public void setBackorder(int backorder);

    public String getDescription();

    public void setDescription(String description);

    public int getInventory();

    public void setInventory(int inventory);

    public java.util.Collection<? extends ILineItem> getLineitems();

    public void setLineitems(java.util.Collection<? extends ILineItem> lineitems);

    public int getPid();

    public void setPid(int pid);

    public ICustomerBean getSupplier();

    public void setSupplier(ICustomerBean supplier);

    @Override
    public String toString();
}
