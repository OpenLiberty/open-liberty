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

/**
 * Interface implemented by all entities belonging to the inverse side of the Many-to-One relationship.
 *
 */
public interface IEntityB {
    /*
     * Entity Primary Key
     *
     */
    public int getId();

    public void setId(int id);

    /*
     * Simple Entity Persistent Data
     *
     */
    public String getName();

    public void setName(String name);
}
