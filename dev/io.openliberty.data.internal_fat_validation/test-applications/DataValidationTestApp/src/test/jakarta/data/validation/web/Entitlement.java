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
package test.jakarta.data.validation.web;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Jakarta Persistence entity class with bean validation annotations.
 */
@Entity
public class Entitlement {
    public static enum Frequency {
        YEARLY, MONTHLY, ONE_TIME, AS_NEEDED
    };

    @NotNull
    public Frequency frequency;

    @Id
    public long id;

    @Digits(integer = 7, fraction = 2)
    @DecimalMin("0.00")
    public BigDecimal maxBenefit;

    @Digits(integer = 7, fraction = 2)
    @DecimalMin("0.00")
    public Float minBenefit;

    @Email
    @NotNull
    public String beneficiaryEmail;

    private int minAge;

    private Integer maxAge;

    @Pattern(regexp = "US-.*")
    @Size(min = 5, max = 50)
    public String type;

    public Entitlement() {
    }

    public Entitlement(long id, String type, String beneficiaryEmail, Frequency frequency,
                       int minAge, Integer maxAge, Float minBenefit, BigDecimal maxBenefit) {
        this.id = id;
        this.type = type;
        this.beneficiaryEmail = beneficiaryEmail;
        this.frequency = frequency;
        this.maxBenefit = maxBenefit;
        this.minBenefit = minBenefit;
        this.minAge = minAge;
        this.maxAge = maxAge;
    }

    @PositiveOrZero
    public Integer getMaxAge() {
        return maxAge;
    }

    @Min(0)
    public int getMinAge() {
        return minAge;
    }

    public void setMaxAge(Integer age) {
        maxAge = age;
    }

    public void setMinAge(int age) {
        minAge = age;
    }

    @Override
    public String toString() {
        return "Entitlement#" + id + ":" + type + " " + frequency + " for " + beneficiaryEmail +
               " from age " + minAge + "-" + maxAge +
               " min $" + minBenefit + " max $" + maxBenefit;
    }

}
