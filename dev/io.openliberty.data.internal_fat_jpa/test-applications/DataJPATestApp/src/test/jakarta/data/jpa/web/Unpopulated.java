/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package test.jakarta.data.jpa.web;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Entity without any values in the database.
 * This is used for a test that references the entity only as
 * the result of a single finder method. Do not add other usage.
 */
@Entity
public class Unpopulated {

    @Id
    @Column(nullable = false)
    private Integer id;

    @Column(nullable = false)
    private String something;

    public Integer getId() {
        return id;
    }

    public String getSomething() {
        return something;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String something) {
        this.something = something;
    }
}