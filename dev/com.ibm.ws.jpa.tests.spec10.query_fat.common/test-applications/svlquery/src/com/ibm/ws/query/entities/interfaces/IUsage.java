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

public interface IUsage {
    public int getId();

    public void setId(int id);

    public IPart getParent();

    public void setParent(IPartComposite parent);

    public int getQuantity();

    public void setQuantity(int quantity);

    public IPart getChild();

    public void setChild(IPart child);

    @Override
    public String toString();
}
