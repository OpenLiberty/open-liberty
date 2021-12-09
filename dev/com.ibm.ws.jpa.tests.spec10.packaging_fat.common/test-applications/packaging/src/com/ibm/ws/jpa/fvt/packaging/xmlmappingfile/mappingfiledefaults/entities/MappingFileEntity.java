/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities;

public interface MappingFileEntity {
    public int getId();

    public void setId(int id);

    public String getName();

    public void setName(String name);

    public String getCity();

    public void setCity(String city);

    public String getState();

    public void setState(String state);

    public String getStreet();

    public void setStreet(String street);

    public String getZip();

    public void setZip(String zip);
}
