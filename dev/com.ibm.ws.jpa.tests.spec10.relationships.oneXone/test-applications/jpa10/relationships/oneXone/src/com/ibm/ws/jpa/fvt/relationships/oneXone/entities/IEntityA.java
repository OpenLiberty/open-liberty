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
 * Interface implemented by all entities belonging to the owning side of the One-to-One relationship.
 *
 */
public interface IEntityA {
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
     * Field: B1
     *
     * One to one mapping to an UniEntityB-type entity. Overriding the name of the join column to "UNIENT_B1".
     *
     * OneToOne Config Cascade: default no Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: The foreign key column name is set to "UNIENT_B1".
     */
    public IEntityB getB1Field();

    public void setB1Field(IEntityB b1);

    /*
     * Field: B2
     *
     * One to one mapping to an UniEntityB-type entity. No override of the foreign key column name, which should default
     * to "B2_ID" (Name of the field of the referencing entity + " " + the name of the referenced primary key column).
     *
     * OneToOne Config Cascade: default no Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config (complete default, so no JoinColumn annotation) Name: The foreign key column name is set to
     * "B2_ID".
     */
    public IEntityB getB2Field();

    public void setB2Field(IEntityB b2);

    /*
     * Field: B4
     *
     * One to one mapping to an UniEntityB-type entity. Overriding the name of the join column to "UNIENT_B4". FetchType
     * has been set to LAZY.
     *
     * OneToOne Config Cascade: default no Fetch: LAZY Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: The foreign key column name is set to "UNIENT_B1".
     */
    public IEntityB getB4Field();

    public void setB4Field(IEntityB b4);

    /*
     * Field: B5CA
     *
     * One to one mapping to an UniEntityB-type entity. Overriding the name of the join column to "UNIENT_B5CA". This
     * relation field has the CascadeType of ALL.
     *
     * OneToOne Config Cascade: ALL Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: The foreign key column name is set to "UNIENT_B1".
     */
    public IEntityB getB5caField();

    public void setB5caField(IEntityB b5ca);

    /*
     * Field: B5CM
     *
     * One to one mapping to an UniEntityB-type entity. Overriding the name of the join column to "UNIENT_B5CM". This
     * relation field has the CascadeType of MERGE.
     *
     * OneToOne Config Cascade: MERGE Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: The foreign key column name is set to "UNIENT_B1".
     */
    public IEntityB getB5cmField();

    public void setB5cmField(IEntityB b5cm);

    /*
     * Field: B5CP
     *
     * One to one mapping to an UniEntityB-type entity. Overriding the name of the join column to "UNIENT_B5CP". This
     * relation field has the CascadeType of PERSIST.
     *
     * OneToOne Config Cascade: PERSIST Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: The foreign key column name is set to "UNIENT_B1".
     */
    public IEntityB getB5cpField();

    public void setB5cpField(IEntityB b5cp);

    /*
     * Field: B5RF
     *
     * One to one mapping to an UniEntityB-type entity. Overriding the name of the join column to "UNIENT_B5RF". This
     * relation field has the CascadeType of REFRESH.
     *
     * OneToOne Config Cascade: REFRESH Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: The foreign key column name is set to "UNIENT_B1".
     */
    public IEntityB getB5rfField();

    public void setB5rfField(IEntityB b5rf);

    /*
     * Field: B5RM
     *
     * One to one mapping to an UniEntityB-type entity. Overriding the name of the join column to "UNIENT_B5RM". This
     * relation field has the CascadeType of REMOVE.
     *
     * OneToOne Config Cascade: REMOVE Fetch: default eager Optional: default true (reference can be null).
     *
     * JoinColumn Config Name: The foreign key column name is set to "UNIENT_B1".
     */
    public IEntityB getB5rmField();

    public void setB5rmField(IEntityB b5rm);
}
