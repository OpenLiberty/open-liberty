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

public interface IAddressBean {

    @Override
    public String toString();

    public IAddressPK getStreet();

    public void setStreet(IAddressPK street);

    public String getCity();

    public void setCity(String city);

    public String getState();

    public void setState(String state);

    public String getZip();

    public void setZip(String zip);
}
