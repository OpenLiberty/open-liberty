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

public interface ICharityFund {
    public Double getCharityAmount();

    public void setCharityAmount(Double charityAmount);

    public String getCharityName();

    public void setCharityName(String charityName);

    @Override
    public String toString();

    public boolean equals(ICharityFund arg);
}
