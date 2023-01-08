/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.fvt.entity.entities;

import com.ibm.ws.jpa.fvt.entity.entities.embeddable.SimpleEmbeddableObject;

public interface IEmbeddedObjectEntity {
    public int getId();

    public void setId(int id);

    public int getLocalIntVal();

    public void setLocalIntVal(int localIntVal);

    public String getLocalStrVal();

    public void setLocalStrVal(String localStrVal);

    public SimpleEmbeddableObject getEmbeddedObj();

    public void setEmbeddedObj(SimpleEmbeddableObject embeddedObj);
}
