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

import javax.persistence.EntityManager;

public interface IPartComposite extends IPart {
    public IPartComposite addSubPart(EntityManager em, int quantity, IPart subpart);

    public double getAssemblyCost();

    public void setAssemblyCost(double assemblyCost);

    public double getMassIncrement();

    public void setMassIncrement(double massIncrement);

    @Override
    public String toString();

    public Collection<? extends IUsage> getPartsUsed();

    public void setPartsUsed(Collection<? extends IUsage> partsUsed);
}
