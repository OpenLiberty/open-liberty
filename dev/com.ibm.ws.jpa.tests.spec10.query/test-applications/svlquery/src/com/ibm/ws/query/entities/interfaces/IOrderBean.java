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

import java.util.Collection;

public interface IOrderBean {
    public double getAmount();

    public void setAmount(double amount);

    public ICustomerBean getCustomer();

    public void setCustomer(ICustomerBean customer);

    public boolean isDelivered();

    public void setDelivered(boolean delivered);

    public int getOid();

    public void setOid(int oid);

    @Override
    public String toString();

    public Collection<? extends ILineItem> getLineitems();

    public void setLineitems(Collection<? extends ILineItem> lineitems);

}
