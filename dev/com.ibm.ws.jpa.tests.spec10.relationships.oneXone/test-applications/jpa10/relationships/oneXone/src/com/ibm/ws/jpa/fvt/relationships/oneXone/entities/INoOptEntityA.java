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
 * Interface implemented by all entities belonging to the owning side of the One-to-One relationship that are testing
 * non-optional relationship fields.
 *
 */
public interface INoOptEntityA {
    /*
     * Entity Primary Key
     */
    public int getId();

    public void setId(int id);

    /*
     * Simple Entity Persistent Data
     */
    public String getName();

    public void setName(String name);

    /*
     * Relationship Fields
     */

    /**
     * One to One Relationship, non optional.
     */
    public INoOptEntityB getBField();

    public void setBField(INoOptEntityB b);

}
