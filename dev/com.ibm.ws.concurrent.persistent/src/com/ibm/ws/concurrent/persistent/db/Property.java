/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * JPA entity for a property entry in the persistent store.
 */
@Entity
@Trivial
public class Property {
    /**
     * Property name.
     */
    @Column(length = 254, nullable = false)
    @Id
    public String ID;

    /**
     * Property value.
     */
    @Column(length = 254, nullable = false)
    public String VAL;

    public Property() {}

    Property(String id, String value) {
        ID = id;
        VAL = value;
    }
}
