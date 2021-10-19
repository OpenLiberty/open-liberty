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

public interface ISupplier {
    public ISupplier addPart(IPartBase p);

    public String getName();

    public void setName(String name);

    public int getSid();

    public void setSid(int sid);

    public Collection<? extends IPartBase> getSupplies();

    public void setSupplies(Collection<? extends IPartBase> supplies);

    @Override
    public String toString();
}
