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

    /*
     * Field: noOptional
     *
     * Many to one mapping to an IEntityB-type entity. No override of the foreign key column name.
     *
     * ManyToOne Config Cascade: default no Fetch: default eager Optional: false (reference can not be null).
     *
     * JoinColumn Config (complete default, so no JoinColumn annotation) Name: Default column name.
     */
    public INoOptEntityB getNoOptionalField();

    public void setNoOptionalField(INoOptEntityB b2);
}
