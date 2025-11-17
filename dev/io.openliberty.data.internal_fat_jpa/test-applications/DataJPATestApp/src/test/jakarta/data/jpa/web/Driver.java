/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import java.time.LocalDate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

/**
 * Entity that has one-to-one mapping with the DriversLicense entity.
 */
@Entity
public class Driver {

    @Column
    public LocalDate birthday;

    @Column
    public String fullName;

    @Column
    public int heightInInches;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn
    public DriversLicense license;

    @Id
    public int ssn;

    @Column
    public int weightInPounds;

    public Driver() {
    }

    public Driver(String fullName, int ssn, LocalDate birthday, int height, int weight,
                  String licenseNum, String stateName, LocalDate issuedOn, LocalDate expiresOn) {
        this.fullName = fullName;
        this.ssn = ssn;
        this.birthday = birthday;
        this.heightInInches = height;
        this.weightInPounds = weight;
        this.license = new DriversLicense();
        license.licenseNum = licenseNum;
        license.stateName = stateName;
        license.issuedOn = issuedOn;
        license.expiresOn = expiresOn;
    }

    @Override
    public String toString() {
        return "Driver SSN#" + ssn + " " + fullName + " born " + birthday + " " +
               heightInInches + " inches " + weightInPounds + " lbs with license #" + (license == null ? null : license.licenseNum);
    }
}
