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

public interface IPartBase extends IPart {
    @Override
    public int getPartno();

    @Override
    public String getName();

    public double getCost();

    public void setCost(double cost);

    public double getMass();

    public void setMass(double mass);

    public Collection<? extends ISupplier> getSuppliers();

    public void setSuppliers(Collection<? extends ISupplier> suppliers);

    @Override
    public String toString();
}
