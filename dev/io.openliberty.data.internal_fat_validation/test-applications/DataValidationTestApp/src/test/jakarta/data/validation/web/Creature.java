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
import java.time.OffsetDateTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Java class with bean validation annotations and no entity annotation.
 */
public class Creature {
    @Size(min = 1, max = 100)
    public String commonName;

    public long id;

    @NotNull
    @PastOrPresent
    public OffsetDateTime identifiedOn;

    @Max(90)
    @Min(-90)
    public BigDecimal latitude;

    @DecimalMax("180")
    @DecimalMin("-180")
    public BigDecimal longitude;

    // begin with capital, then any number of characters or space, then space, then begin with lower case, then any number of characters or space
    @Pattern(regexp = "[A-Z][a-zA-z\\s]*[\\s][a-z][a-zA-z\\s]*")
    public String scientificName;

    @Positive
    public float weight;

    public Creature() {
    }

    public Creature(long id, String commonName, String scientificName,
                    BigDecimal latitude, BigDecimal longitude, OffsetDateTime identifiedOn, float weight) {
        this.id = id;
        this.identifiedOn = identifiedOn;
        this.latitude = latitude;
        this.longitude = longitude;
        this.commonName = commonName;
        this.scientificName = scientificName;
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "Creature#" + id + " " + scientificName + " (" + commonName + ") @" + latitude + "," + longitude + " " + weight + "kg";
    }
}
