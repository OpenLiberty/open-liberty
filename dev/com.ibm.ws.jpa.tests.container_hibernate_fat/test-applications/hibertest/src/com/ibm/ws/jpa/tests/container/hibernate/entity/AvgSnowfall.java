/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.tests.container.hibernate.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
public class AvgSnowfall {
    @Id
    @NotNull
    @Size(min = 1, max = 50)
    private String city;

    @Max(500)
    @Min(0)
    private int amount;

    public AvgSnowfall() {
    }

    public AvgSnowfall(String city) {
        this.city = city;
    }

    public String getCity() {
        return city;
    }

    public int getAmount() {
        return amount;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
