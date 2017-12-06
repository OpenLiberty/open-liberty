/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package jpa22bval.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.Email;
import javax.validation.constraints.Future;
import javax.validation.constraints.NotNull;

@Entity
public class BeanValEntity {
    @Id
    @GeneratedValue
    private long id;

    @Email
    @NotNull
    private String email;

    @Future
    private java.time.Instant futureInstant;

    /**
     * @return the futureInstant
     */
    public java.time.Instant getFutureInstant() {
        return futureInstant;
    }

    /**
     * @param futureInstant the futureInstant to set
     */
    public void setFutureInstant(java.time.Instant futureInstant) {
        this.futureInstant = futureInstant;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return this.email;
    }
}
