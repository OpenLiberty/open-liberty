/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.manyXone.entities;

import java.util.Collection;

/**
 * Interface implemented by all entities belonging to the inverse side of the bidirectional
 * Many-to-One relationship.
 *
 */
public interface IEntityBBi extends IEntityB {
    public Collection getEntityACollection();

    public boolean isMemberOfEntityAField(IEntityA iEntityA);

    public void setEntityACollectionField(Collection iEntityA);

    public void insertEntityAField(IEntityA iEntityA);

    public void removeEntityAField(IEntityA iEntityA);
}
