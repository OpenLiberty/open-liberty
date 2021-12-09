/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.manyXmany.entities;

import java.util.Collection;

public interface IEntityBBi extends IEntityB {
    public Collection getEntityACollection();

    public void setEntityACollectionField(Collection iEntityA);

    public void insertEntityAField(IEntityA iEntityA);

    public void removeEntityAField(IEntityA iEntityA);

    public boolean isMemberOfEntityAField(IEntityA iEntityA);
}
