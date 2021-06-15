/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.oneXone.entities;

/**
 * Interface implemented by all entities belonging to the inverse side of the One-to-One bidirectional relationship.
 *
 */
public interface IEntityBBi extends IEntityB {
    /*
     * Relationship Fields
     */

    /*
     * Reference to the IEntityA entity that owns the relationship with this entity.
     */
    public IEntityA getEntityAField();

    public void setEntityAField(IEntityA entity);
}
